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
import io.github.nephets.payledger.core.exception.EmptyTransactionException;
import io.github.nephets.payledger.core.exception.UnbalancedEntryException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostingServiceTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    private InMemoryEntryStore entryStore;

    private InMemoryBalanceStore balanceStore;

    private PostingService posting;

    @BeforeEach
    void setUp() {
        entryStore = new InMemoryEntryStore();
        balanceStore = new InMemoryBalanceStore();
        balanceStore.openAccount(new Account("acc_cash", "t1", AccountType.ASSET, "CNY"));
        balanceStore.openAccount(new Account("acc_rev", "t1", AccountType.INCOME, "CNY"));
        posting = new PostingService(entryStore, balanceStore);
        PayledgerTenant.set("t1");
    }

    @AfterEach
    void tearDown() {
        PayledgerTenant.clear();
    }

    private Transaction build(double cash, double rev) {
        Transaction tx = new Transaction("tx1", "t1", NOW, "收款");
        tx.addEntry(
                new LedgerEntry(
                        "e1",
                        "t1",
                        "acc_cash",
                        Direction.DEBIT,
                        Money.of(String.valueOf(cash), "CNY"),
                        "tx1",
                        "",
                        "现金",
                        1L,
                        NOW));
        tx.addEntry(
                new LedgerEntry(
                        "e2",
                        "t1",
                        "acc_rev",
                        Direction.CREDIT,
                        Money.of(String.valueOf(rev), "CNY"),
                        "tx1",
                        "",
                        "收入",
                        2L,
                        NOW));
        return tx;
    }

    @Test
    void postsAndUpdatesBalances() {
        Transaction tx = posting.post(build(100.00, 100.00));
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.POSTED);
        // 余额存储对所有账户统一按「借加贷减」记账：现金（资产）借 100 → +100，
        // 收入（贷）贷 100 → -100。这是简单内存实现的约定。
        assertThat(balanceStore.getBalance("t1", "acc_cash")).isEqualTo(Money.of("100.00", "CNY"));
        assertThat(balanceStore.getBalance("t1", "acc_rev")).isEqualTo(Money.of("-100.00", "CNY"));
        assertThat(entryStore.findByTransaction("t1", "tx1")).hasSize(2);
    }

    @Test
    void explicitTenantOverrides() {
        Transaction tx = posting.post(build(50.00, 50.00), "t1");
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.POSTED);
        assertThat(balanceStore.getBalance("t1", "acc_cash")).isEqualTo(Money.of("50.00", "CNY"));
    }

    @Test
    void unbalancedDoesNotPost() {
        Transaction tx = build(100.00, 90.00);
        assertThatThrownBy(() -> posting.post(tx)).isInstanceOf(UnbalancedEntryException.class);
        assertThat(entryStore.findAll("t1")).isEmpty();
        assertThat(balanceStore.getBalance("t1", "acc_cash")).isEqualTo(Money.zero("CNY"));
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void emptyTransactionDoesNotPost() {
        Transaction tx = new Transaction("txEmpty", "t1", NOW, "空");
        assertThatThrownBy(() -> posting.post(tx)).isInstanceOf(EmptyTransactionException.class);
    }

    @Test
    void accountNotFoundDoesNotPost() {
        Transaction tx = new Transaction("txNf", "t1", NOW, "m");
        tx.addEntry(
                new LedgerEntry(
                        "e1",
                        "t1",
                        "acc_missing",
                        Direction.DEBIT,
                        Money.of("10.00", "CNY"),
                        "txNf",
                        "",
                        "m",
                        1L,
                        NOW));
        tx.addEntry(
                new LedgerEntry(
                        "e2",
                        "t1",
                        "acc_rev",
                        Direction.CREDIT,
                        Money.of("10.00", "CNY"),
                        "txNf",
                        "",
                        "m",
                        2L,
                        NOW));
        assertThatThrownBy(() -> posting.post(tx)).isInstanceOf(AccountNotFoundException.class);
        assertThat(balanceStore.getBalance("t1", "acc_rev")).isEqualTo(Money.zero("CNY"));
    }

    @Test
    void frozenAccountRejected() {
        balanceStore.openAccount(
                new Account("acc_frozen", "t1", AccountType.ASSET, "CNY", AccountStatus.FROZEN));
        Transaction tx = new Transaction("txFz", "t1", NOW, "m");
        tx.addEntry(
                new LedgerEntry(
                        "e1",
                        "t1",
                        "acc_frozen",
                        Direction.DEBIT,
                        Money.of("10.00", "CNY"),
                        "txFz",
                        "",
                        "m",
                        1L,
                        NOW));
        tx.addEntry(
                new LedgerEntry(
                        "e2",
                        "t1",
                        "acc_rev",
                        Direction.CREDIT,
                        Money.of("10.00", "CNY"),
                        "txFz",
                        "",
                        "m",
                        2L,
                        NOW));
        assertThatThrownBy(() -> posting.post(tx)).isInstanceOf(AccountFrozenException.class);
    }

    @Test
    void currencyMismatchWithAccountRejected() {
        balanceStore.openAccount(new Account("acc_usd", "t1", AccountType.ASSET, "USD"));
        Transaction tx = new Transaction("txCur", "t1", NOW, "m");
        tx.addEntry(
                new LedgerEntry(
                        "e1",
                        "t1",
                        "acc_usd",
                        Direction.DEBIT,
                        Money.of("10.00", "USD"),
                        "txCur",
                        "",
                        "m",
                        1L,
                        NOW));
        tx.addEntry(
                new LedgerEntry(
                        "e2",
                        "t1",
                        "acc_rev",
                        Direction.CREDIT,
                        Money.of("10.00", "CNY"),
                        "txCur",
                        "",
                        "m",
                        2L,
                        NOW));
        assertThatThrownBy(() -> posting.post(tx)).isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void fixedTenantLeavesCurrentUntouched() {
        PayledgerTenant.set("other");
        try {
            PostingService fixed = new PostingService(entryStore, balanceStore, "t1");
            fixed.post(build(5.00, 5.00));
            assertThat(balanceStore.getBalance("t1", "acc_cash"))
                    .isEqualTo(Money.of("5.00", "CNY"));
        } finally {
            PayledgerTenant.clear();
        }
    }

    @Test
    void requireTenantThrowsWhenUnsetAndNoFixed() {
        PayledgerTenant.clear();
        PostingService noFixed = new PostingService(entryStore, balanceStore);
        Transaction tx = build(1.00, 1.00);
        assertThatThrownBy(() -> noFixed.post(tx)).isInstanceOf(IllegalStateException.class);
    }
}
