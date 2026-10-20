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

import io.github.nephets.payledger.core.exception.CurrencyMismatchException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 为什么测这些：过账服务是「钱从哪来到哪去」的唯一入口，原子性（预演不过则一点不写）、租户解析优先级、状态流转、对账依据的 reference 落库，都是资金安全的核心。 */
class PostingServiceContractTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    private final InMemoryEntryStore entryStore = new InMemoryEntryStore();
    private final InMemoryBalanceStore balanceStore = new InMemoryBalanceStore();

    @BeforeEach
    void setUp() {
        // 过账默认从当前线程租户上下文解析租户，必须先设置，否则 require() 抛 IllegalStateException。
        PayledgerTenant.set("t1");
    }

    @AfterEach
    void tearDown() {
        PayledgerTenant.clear();
    }

    private PostingService posting() {
        balanceStore.openAccount(new Account("cash", "t1", AccountType.ASSET, "CNY"));
        balanceStore.openAccount(new Account("rev", "t1", AccountType.INCOME, "CNY"));
        balanceStore.openAccount(new Account("fee", "t1", AccountType.EXPENSE, "CNY"));
        return new PostingService(entryStore, balanceStore);
    }

    private Transaction threeWay(String cash, String rev, String fee) {
        Transaction tx = new Transaction("tx1", "t1", NOW, "收款分账");
        tx.addEntry(
                new LedgerEntry(
                        "e1",
                        "t1",
                        "cash",
                        Direction.DEBIT,
                        Money.of(cash, "CNY"),
                        "tx1",
                        "R1",
                        "现金",
                        1L,
                        NOW));
        tx.addEntry(
                new LedgerEntry(
                        "e2",
                        "t1",
                        "rev",
                        Direction.CREDIT,
                        Money.of(rev, "CNY"),
                        "tx1",
                        "R1",
                        "收入",
                        2L,
                        NOW));
        tx.addEntry(
                new LedgerEntry(
                        "e3",
                        "t1",
                        "fee",
                        Direction.CREDIT,
                        Money.of(fee, "CNY"),
                        "tx1",
                        "R1",
                        "费用",
                        3L,
                        NOW));
        return tx;
    }

    @Test
    void postsThreeWayBalancedTransaction() {
        PostingService posting = posting();
        Transaction tx = posting.post(threeWay("110.00", "100.00", "10.00"));
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.POSTED);
        assertThat(balanceStore.getBalance("t1", "cash")).isEqualTo(Money.of("110.00", "CNY"));
        assertThat(balanceStore.getBalance("t1", "rev")).isEqualTo(Money.of("-100.00", "CNY"));
        assertThat(balanceStore.getBalance("t1", "fee")).isEqualTo(Money.of("-10.00", "CNY"));
    }

    @Test
    void postedEntryCarriesReference() {
        PostingService posting = posting();
        posting.post(threeWay("110.00", "100.00", "10.00"));
        assertThat(entryStore.findByTransaction("t1", "tx1"))
                .allMatch(e -> "R1".equals(e.getReference()));
    }

    @Test
    void resolvesTenantFromThreadContext() {
        PayledgerTenant.set("t1");
        PostingService posting = posting();
        Transaction tx = posting.post(threeWay("10.00", "10.00", "0.00"));
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.POSTED);
    }

    @Test
    void explicitTenantWinsOverContext() {
        PayledgerTenant.set("other");
        try {
            PostingService posting = posting();
            // 显式传 t1，即使线程上下文是 other 也按 t1 过账。
            Transaction tx = posting.post(threeWay("10.00", "10.00", "0.00"), "t1");
            assertThat(balanceStore.getBalance("t1", "cash")).isEqualTo(Money.of("10.00", "CNY"));
        } finally {
            PayledgerTenant.clear();
        }
    }

    @Test
    void postNullTransactionThrows() {
        assertThatThrownBy(() -> posting().post(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void postNullTenantThrows() {
        assertThatThrownBy(() -> posting().post(threeWay("10.00", "10.00", "0.00"), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void mixedCurrencyEntriesRejectedBeforeWrite() {
        PostingService posting = posting();
        balanceStore.openAccount(new Account("usd", "t1", AccountType.ASSET, "USD"));
        Transaction tx = new Transaction("txM", "t1", NOW, "m");
        tx.addEntry(
                new LedgerEntry(
                        "e1",
                        "t1",
                        "cash",
                        Direction.DEBIT,
                        Money.of("10.00", "CNY"),
                        "txM",
                        "",
                        "m",
                        1L,
                        NOW));
        tx.addEntry(
                new LedgerEntry(
                        "e2",
                        "t1",
                        "usd",
                        Direction.CREDIT,
                        Money.of("10.00", "USD"),
                        "txM",
                        "",
                        "m",
                        2L,
                        NOW));
        assertThatThrownBy(() -> posting.post(tx)).isInstanceOf(CurrencyMismatchException.class);
        // 校验失败不能落账：cash 仍为零。
        assertThat(balanceStore.getBalance("t1", "cash")).isEqualTo(Money.zero("CNY"));
    }

    @Test
    void failsafeNoLostWriteOnValidationError() {
        PostingService posting = posting();
        Transaction tx = threeWay("100.00", "90.00", "5.00"); // 借贷不平衡：100 vs 95
        assertThatThrownBy(() -> posting.post(tx)).isInstanceOf(RuntimeException.class);
        // 校验失败必须一点不写：现金账户仍为零。
        assertThat(balanceStore.getBalance("t1", "cash")).isEqualTo(Money.zero("CNY"));
    }
}
