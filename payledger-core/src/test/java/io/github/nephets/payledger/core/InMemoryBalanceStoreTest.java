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

import io.github.nephets.payledger.core.exception.AccountNotFoundException;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryBalanceStoreTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    @Test
    void openAccountInitializesZero() {
        InMemoryBalanceStore store = new InMemoryBalanceStore();
        store.openAccount(new Account("a", "t1", AccountType.ASSET, "CNY"));
        assertThat(store.getBalance("t1", "a")).isEqualTo(Money.zero("CNY"));
    }

    @Test
    void openAccountIsIdempotent() {
        InMemoryBalanceStore store = new InMemoryBalanceStore();
        store.openAccount(new Account("a", "t1", AccountType.ASSET, "CNY"));
        store.applyEntry(debit("t1", "a", "10.00"));
        store.openAccount(new Account("a", "t1", AccountType.ASSET, "CNY"));
        assertThat(store.getBalance("t1", "a")).isEqualTo(Money.of("10.00", "CNY"));
    }

    @Test
    void applyEntryDebitAndCredit() {
        InMemoryBalanceStore store = new InMemoryBalanceStore();
        store.openAccount(new Account("a", "t1", AccountType.ASSET, "CNY"));
        store.applyEntry(debit("t1", "a", "30.00"));
        store.applyEntry(credit("t1", "a", "10.00"));
        assertThat(store.getBalance("t1", "a")).isEqualTo(Money.of("20.00", "CNY"));
    }

    @Test
    void getBalanceMissingThrows() {
        InMemoryBalanceStore store = new InMemoryBalanceStore();
        assertThatThrownBy(() -> store.getBalance("t1", "missing"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void applyEntryMissingThrows() {
        InMemoryBalanceStore store = new InMemoryBalanceStore();
        assertThatThrownBy(() -> store.applyEntry(debit("t1", "missing", "1.00")))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void findAccountReturnsEmptyWhenMissing() {
        InMemoryBalanceStore store = new InMemoryBalanceStore();
        assertThat(store.findAccount("t1", "x")).isEmpty();
    }

    @Test
    void findAccountReturnsAccount() {
        InMemoryBalanceStore store = new InMemoryBalanceStore();
        Account a = new Account("a", "t1", AccountType.ASSET, "CNY");
        store.openAccount(a);
        assertThat(store.findAccount("t1", "a")).contains(a);
    }

    @Test
    void nextSequenceNoIncrements() {
        InMemoryBalanceStore store = new InMemoryBalanceStore();
        assertThat(store.nextSequenceNo("t1")).isEqualTo(1);
        assertThat(store.nextSequenceNo("t1")).isEqualTo(2);
    }

    @Test
    void partitionedByTenant() {
        InMemoryBalanceStore store = new InMemoryBalanceStore();
        store.openAccount(new Account("a", "t1", AccountType.ASSET, "CNY"));
        store.openAccount(new Account("a", "t2", AccountType.ASSET, "CNY"));
        assertThat(store.findAccount("t1", "a")).isPresent();
        assertThat(store.findAccount("t2", "a")).isPresent();
    }

    private LedgerEntry debit(String tenant, String account, String amount) {
        return new LedgerEntry(
                "e",
                tenant,
                account,
                Direction.DEBIT,
                Money.of(amount, "CNY"),
                "tx",
                "",
                "m",
                1L,
                NOW);
    }

    private LedgerEntry credit(String tenant, String account, String amount) {
        return new LedgerEntry(
                "e",
                tenant,
                account,
                Direction.CREDIT,
                Money.of(amount, "CNY"),
                "tx",
                "",
                "m",
                1L,
                NOW);
    }
}
