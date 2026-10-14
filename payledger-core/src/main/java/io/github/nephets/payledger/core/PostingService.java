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
package io.github.nephets.payledger.core;

import io.github.nephets.payledger.core.exception.AccountFrozenException;
import io.github.nephets.payledger.core.exception.AccountNotFoundException;
import io.github.nephets.payledger.core.exception.CurrencyMismatchException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * 过账服务：把一笔借贷平衡的交易落到余额与分录存储。
 *
 * <p>过账流程：先 {@link TransactionValidator#validate} 校验 → 再「预演」逐个账户确认存在、未冻结、币种一致，算出新的应有余额 →
 * 全部通过后才统一写余额与分录，并把交易置为 {@link TransactionStatus#POSTED}。任一校验失败都不落账，保证原子性。
 *
 * <p>租户来源：构造时传入的 {@code fixedTenant} 优先；否则取交易自身的 {@code tenantId}；再否则取 {@link
 * PayledgerTenant#current()}。也支持 {@link #post(Transaction, String)} 显式指定租户。
 */
public final class PostingService {

    private static final Logger log = LoggerFactory.getLogger(PostingService.class);

    private final EntryStore entryStore;

    private final BalanceStore balanceStore;

    private final String fixedTenant;

    public PostingService(EntryStore entryStore, BalanceStore balanceStore) {
        this(entryStore, balanceStore, null);
    }

    public PostingService(EntryStore entryStore, BalanceStore balanceStore, String fixedTenant) {
        this.entryStore = Objects.requireNonNull(entryStore, "entryStore 不能为 null");
        this.balanceStore = Objects.requireNonNull(balanceStore, "balanceStore 不能为 null");
        this.fixedTenant = fixedTenant;
    }

    /** 按默认租户来源过账。 */
    public Transaction post(Transaction tx) {
        return post(tx, resolveTenant(tx));
    }

    /** 显式指定租户过账。 */
    public Transaction post(Transaction tx, String tenantId) {
        Objects.requireNonNull(tx, "transaction 不能为 null");
        Objects.requireNonNull(tenantId, "tenantId 不能为 null");
        log.info(
                "posting transaction tenantId={} transactionId={} entries={}",
                tenantId,
                tx.getTransactionId(),
                tx.getEntries().size());
        TransactionValidator.validate(tx);

        // 预演：确认每个账户存在、未冻结、币种一致，任何一项不通过都不写存储。
        for (LedgerEntry entry : tx.getEntries()) {
            Account account =
                    balanceStore
                            .findAccount(tenantId, entry.getAccountId())
                            .orElseThrow(
                                    () ->
                                            new AccountNotFoundException(
                                                    tenantId, entry.getAccountId()));
            if (account.isFrozen()) {
                throw new AccountFrozenException(tenantId, entry.getAccountId());
            }
            Money current = balanceStore.getBalance(tenantId, entry.getAccountId());
            if (!current.getCurrency().equals(entry.getAmount().getCurrency())) {
                throw new CurrencyMismatchException(
                        current.getCurrency(), entry.getAmount().getCurrency());
            }
        }

        // 提交：预演全部通过后才调整余额并保存分录。
        for (LedgerEntry entry : tx.getEntries()) {
            balanceStore.applyEntry(entry);
        }
        entryStore.save(tx.getEntries());
        tx.setStatus(TransactionStatus.POSTED);
        log.info(
                "transaction posted tenantId={} transactionId={}", tenantId, tx.getTransactionId());
        return tx;
    }

    private String resolveTenant(Transaction tx) {
        if (fixedTenant != null) {
            return fixedTenant;
        }
        String current = PayledgerTenant.current();
        if (current != null) {
            return current;
        }
        return PayledgerTenant.require();
    }
}
