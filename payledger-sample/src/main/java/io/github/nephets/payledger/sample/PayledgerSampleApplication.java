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
package io.github.nephets.payledger.sample;

import io.github.nephets.payledger.core.Account;
import io.github.nephets.payledger.core.AccountType;
import io.github.nephets.payledger.core.BalanceStore;
import io.github.nephets.payledger.core.Direction;
import io.github.nephets.payledger.core.ExternalStatement;
import io.github.nephets.payledger.core.LedgerEntry;
import io.github.nephets.payledger.core.Money;
import io.github.nephets.payledger.core.PayledgerTenant;
import io.github.nephets.payledger.core.PostingService;
import io.github.nephets.payledger.core.Reconciler;
import io.github.nephets.payledger.core.ReconciliationReport;
import io.github.nephets.payledger.core.SettlementEngine;
import io.github.nephets.payledger.core.SettlementInstruction;
import io.github.nephets.payledger.core.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Instant;
import java.util.List;

/**
 * 示例应用：开两个账户、过一笔交易、查余额，再跑一次对账与一次清结算，把结果打到日志。
 *
 * <p>全程在 {@code demo} 租户上下文里执行；多租户的余额与分录按租户隔离。
 */
@SpringBootApplication
public class PayledgerSampleApplication {

    private static final Logger log = LoggerFactory.getLogger(PayledgerSampleApplication.class);

    private final PostingService postingService;

    private final BalanceStore balanceStore;

    private final Reconciler reconciler;

    private final SettlementEngine settlementEngine;

    public PayledgerSampleApplication(
            PostingService postingService,
            BalanceStore balanceStore,
            Reconciler reconciler,
            SettlementEngine settlementEngine) {
        this.postingService = postingService;
        this.balanceStore = balanceStore;
        this.reconciler = reconciler;
        this.settlementEngine = settlementEngine;
    }

    public static void main(String[] args) {
        SpringApplication.run(PayledgerSampleApplication.class, args);
    }

    @Bean
    public CommandLineRunner demo() {
        return args ->
                PayledgerTenant.runWith(
                        "demo",
                        () -> {
                            balanceStore.openAccount(
                                    new Account("cash", "demo", AccountType.ASSET, "CNY"));
                            balanceStore.openAccount(
                                    new Account("revenue", "demo", AccountType.INCOME, "CNY"));

                            Transaction sale =
                                    new Transaction("tx_demo", "demo", Instant.now(), "收款");
                            sale.addEntry(
                                    new LedgerEntry(
                                            "e1",
                                            "demo",
                                            "cash",
                                            Direction.DEBIT,
                                            Money.of("100.00", "CNY"),
                                            "tx_demo",
                                            "REF1",
                                            "现金",
                                            1L,
                                            Instant.now()));
                            sale.addEntry(
                                    new LedgerEntry(
                                            "e2",
                                            "demo",
                                            "revenue",
                                            Direction.CREDIT,
                                            Money.of("100.00", "CNY"),
                                            "tx_demo",
                                            "REF1",
                                            "收入",
                                            2L,
                                            Instant.now()));
                            postingService.post(sale);

                            log.info("过账后 cash 余额 = {}", balanceStore.getBalance("demo", "cash"));
                            log.info(
                                    "过账后 revenue 余额 = {}",
                                    balanceStore.getBalance("demo", "revenue"));

                            // 对账：构造一条与 REF1 金额相同的外部流水，应当配对成功。
                            ExternalStatement ext =
                                    new ExternalStatement(
                                            "REF1",
                                            Money.of("100.00", "CNY"),
                                            Instant.now(),
                                            "wechat");
                            ReconciliationReport report =
                                    reconciler.reconcile("demo", sale.getEntries(), List.of(ext));
                            log.info(
                                    "对账结果：匹配 {} 条，未匹配内部 {} 条，未匹配外部 {} 条，差异 {} 条",
                                    report.getMatched().size(),
                                    report.getUnmatchedInternal().size(),
                                    report.getUnmatchedExternal().size(),
                                    report.getDiscrepancies().size());

                            // 清结算：A→B 100、B→A 60，净额应为 A→B 40。
                            Transaction t1 = transfer("st1", "A", "B", "100.00");
                            Transaction t2 = transfer("st2", "B", "A", "60.00");
                            List<SettlementInstruction> instructions =
                                    settlementEngine.net("demo", List.of(t1, t2));
                            log.info("清结算指令数 = {}", instructions.size());
                            instructions.forEach(
                                    i ->
                                            log.info(
                                                    "  {} -> {} : {}（来源 {}）",
                                                    i.getFrom(),
                                                    i.getTo(),
                                                    i.getNetAmount(),
                                                    i.getSources()));
                        });
    }

    private Transaction transfer(String id, String from, String to, String amount) {
        balanceStore.openAccount(new Account(from, "demo", AccountType.ASSET, "CNY"));
        balanceStore.openAccount(new Account(to, "demo", AccountType.ASSET, "CNY"));
        Transaction tx = new Transaction(id, "demo", Instant.now(), "transfer");
        tx.addEntry(
                new LedgerEntry(
                        id + "-d",
                        "demo",
                        from,
                        Direction.DEBIT,
                        Money.of(amount, "CNY"),
                        id,
                        "",
                        "m",
                        1L,
                        Instant.now()));
        tx.addEntry(
                new LedgerEntry(
                        id + "-c",
                        "demo",
                        to,
                        Direction.CREDIT,
                        Money.of(amount, "CNY"),
                        id,
                        "",
                        "m",
                        2L,
                        Instant.now()));
        tx.setFrom(from);
        tx.setTo(to);
        return tx;
    }
}
