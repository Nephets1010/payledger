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
import io.github.nephets.payledger.core.exception.EmptyTransactionException;
import io.github.nephets.payledger.core.exception.UnbalancedEntryException;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionValidatorTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    @Test
    void balancedPasses() {
        Transaction tx = new Transaction("tx1", "t1", NOW, "m");
        tx.addEntry(e("e1", "a", Direction.DEBIT, "100.00"));
        tx.addEntry(e("e2", "b", Direction.CREDIT, "100.00"));
        TransactionValidator.validate(tx);
        assertThat(TransactionValidator.isBalanced(tx)).isTrue();
    }

    @Test
    void emptyThrows() {
        Transaction tx = new Transaction("tx1", "t1", NOW, "m");
        assertThatThrownBy(() -> TransactionValidator.validate(tx))
                .isInstanceOf(EmptyTransactionException.class);
        assertThat(TransactionValidator.isBalanced(tx)).isFalse();
    }

    @Test
    void unbalancedThrows() {
        Transaction tx = new Transaction("tx1", "t1", NOW, "m");
        tx.addEntry(e("e1", "a", Direction.DEBIT, "100.00"));
        tx.addEntry(e("e2", "b", Direction.CREDIT, "90.00"));
        assertThatThrownBy(() -> TransactionValidator.validate(tx))
                .isInstanceOf(UnbalancedEntryException.class);
        assertThat(TransactionValidator.isBalanced(tx)).isFalse();
    }

    @Test
    void mismatchedCurrencyThrows() {
        Transaction tx = new Transaction("tx1", "t1", NOW, "m");
        tx.addEntry(e("e1", "a", Direction.DEBIT, "100.00", "CNY"));
        tx.addEntry(e("e2", "b", Direction.CREDIT, "100.00", "USD"));
        assertThatThrownBy(() -> TransactionValidator.validate(tx))
                .isInstanceOf(CurrencyMismatchException.class);
        assertThat(TransactionValidator.isBalanced(tx)).isFalse();
    }

    @Test
    void nullTransactionRejected() {
        assertThatThrownBy(() -> TransactionValidator.validate(null))
                .isInstanceOf(NullPointerException.class);
    }

    private LedgerEntry e(String id, String account, Direction dir, String amount) {
        return new LedgerEntry(
                id, "t1", account, dir, Money.of(amount, "CNY"), "tx1", "", "m", 1L, NOW);
    }

    private LedgerEntry e(
            String id, String account, Direction dir, String amount, String currency) {
        return new LedgerEntry(
                id, "t1", account, dir, Money.of(amount, currency), "tx1", "", "m", 1L, NOW);
    }
}
