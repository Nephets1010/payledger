/*
 * Copyright 2026 The payledger Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.nephets.payledger.starter;

import io.github.nephets.payledger.core.Account;
import io.github.nephets.payledger.core.AccountType;
import io.github.nephets.payledger.core.BalanceStore;
import io.github.nephets.payledger.core.Direction;
import io.github.nephets.payledger.core.LedgerEntry;
import io.github.nephets.payledger.core.Money;
import io.github.nephets.payledger.core.PayledgerTenant;
import io.github.nephets.payledger.core.PostingService;
import io.github.nephets.payledger.core.Transaction;
import io.github.nephets.payledger.core.exception.AccountNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 最小 REST 控制器：过账与余额查询。
 *
 * <p>租户解析顺序：请求体里的 {@code tenantId} → 配置的头（默认 {@code X-Tenant-Id}）→
 * 当前线程上下文；都没有则报错。每次处理都在对应的租户上下文里执行，避免线程池串租户。
 */
@RestController
public class LedgerController {

    private static final Logger log = LoggerFactory.getLogger(LedgerController.class);

    private final PostingService postingService;

    private final BalanceStore balanceStore;

    private final PayledgerProperties properties;

    public LedgerController(
            PostingService postingService,
            BalanceStore balanceStore,
            PayledgerProperties properties) {
        this.postingService = Objects.requireNonNull(postingService);
        this.balanceStore = Objects.requireNonNull(balanceStore);
        this.properties = Objects.requireNonNull(properties);
    }

    @PostMapping("/payledger/transactions")
    public Map<String, Object> post(
            @RequestBody PostTransactionRequest request, HttpServletRequest http) {
        String tenant = resolveTenant(request, http);
        // 先做全量参数校验再进任何副作用：过去畸形输入（空 entries / 非法 direction /
        // 非法 amount）会以 NPE/IllegalArgumentException 冒到 500，且可能已开了一半账户。
        // 合法请求的路径与结果不变。
        validateRequest(request);
        log.info(
                "post transaction request tenantId={} transactionId={} entries={}",
                tenant,
                request.getTransactionId(),
                request.getEntries().size());
        PayledgerTenant.runWith(
                tenant,
                () -> {
                    Transaction tx =
                            new Transaction(
                                    request.getTransactionId(),
                                    tenant,
                                    Instant.now(),
                                    request.getMemo());
                    int seq = 0;
                    for (PostTransactionRequest.EntryLine line : request.getEntries()) {
                        seq++;
                        String currency =
                                line.getCurrency() != null
                                        ? line.getCurrency()
                                        : properties.getDefaultCurrency();
                        Direction direction =
                                Direction.valueOf(line.getDirection().toUpperCase(Locale.ROOT));
                        Money amount = Money.of(line.getAmount(), currency);
                        balanceStore.openAccount(
                                new Account(
                                        line.getAccountId(), tenant, AccountType.ASSET, currency));
                        LedgerEntry entry =
                                new LedgerEntry(
                                        request.getTransactionId() + "-" + seq,
                                        tenant,
                                        line.getAccountId(),
                                        direction,
                                        amount,
                                        request.getTransactionId(),
                                        line.getReference(),
                                        line.getMemo(),
                                        seq,
                                        Instant.now());
                        tx.addEntry(entry);
                    }
                    postingService.post(tx, tenant);
                });
        log.debug("post transaction done transactionId={}", request.getTransactionId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("transactionId", request.getTransactionId());
        result.put("status", "POSTED");
        return result;
    }

    @GetMapping("/payledger/balances/{accountId}")
    public Map<String, Object> balance(@PathVariable String accountId, HttpServletRequest http) {
        String tenant = resolveTenant(null, http);
        log.info("balance query tenantId={} accountId={}", tenant, accountId);
        PayledgerTenant.set(tenant);
        try {
            Money money = balanceStore.getBalance(tenant, accountId);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("accountId", accountId);
            result.put("tenantId", tenant);
            result.put("currency", money.getCurrency());
            result.put("amount", money.getAmount().toPlainString());
            return result;
        } finally {
            PayledgerTenant.clear();
        }
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAccountNotFound(AccountNotFoundException ex) {
        log.error("account not found: {}", ex.getMessage(), ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.status(404).body(body);
    }

    /**
     * 过账请求的前置校验：把客户端错误收敛成 400（带明确原因），而不是任其变成 500。
     *
     * <p>刻意放在开账户等副作用之前：历史上非法分录会在中途 500，留下「账开了一半」的中间态。
     */
    private void validateRequest(PostTransactionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求体不能为空");
        }
        if (request.getTransactionId() == null || request.getTransactionId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transactionId 不能为空");
        }
        List<PostTransactionRequest.EntryLine> entries = request.getEntries();
        if (entries == null || entries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "entries 不能为空");
        }
        for (PostTransactionRequest.EntryLine line : entries) {
            if (line == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分录不能为 null");
            }
            if (line.getAccountId() == null || line.getAccountId().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "accountId 不能为空");
            }
            if (line.getDirection() == null || line.getDirection().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "direction 不能为空");
            }
            try {
                Direction.valueOf(line.getDirection().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "direction 非法（应为 DEBIT/CREDIT）：" + line.getDirection());
            }
            if (line.getAmount() == null || line.getAmount().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount 不能为空");
            }
            try {
                Money.of(
                        line.getAmount(),
                        line.getCurrency() != null
                                ? line.getCurrency()
                                : properties.getDefaultCurrency());
            } catch (RuntimeException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "amount 非法：" + line.getAmount());
            }
        }
    }

    private String resolveTenant(PostTransactionRequest request, HttpServletRequest http) {
        if (request != null && request.getTenantId() != null && !request.getTenantId().isBlank()) {
            return request.getTenantId();
        }
        String header = http.getHeader(properties.getTenantHeader());
        if (header != null && !header.isBlank()) {
            return header;
        }
        if (PayledgerTenant.isSet()) {
            return PayledgerTenant.require();
        }
        throw new IllegalStateException("无法确定租户：请通过请求头或请求体提供");
    }
}
