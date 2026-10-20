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

/** 为什么测这些：校验器是过账前的「闸门」，借贷平衡、币种一致、非空分录三条规则的任何误判都会导致错账入账，必须逐条覆盖。 */
class TransactionValidatorContractTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    private Transaction balanced() {
        Transaction tx = new Transaction("tx", "t1", NOW, "m");
        tx.addEntry(
                new LedgerEntry(
                        "e1",
                        "t1",
                        "a",
                        Direction.DEBIT,
                        Money.of("10.00", "CNY"),
                        "tx",
                        "",
                        "m",
                        1L,
                        NOW));
        tx.addEntry(
                new LedgerEntry(
                        "e2",
                        "t1",
                        "b",
                        Direction.CREDIT,
                        Money.of("10.00", "CNY"),
                        "tx",
                        "",
                        "m",
                        2L,
                        NOW));
        return tx;
    }

    @Test
    void validatePassesForBalanced() {
        TransactionValidator.validate(balanced());
    }

    @Test
    void isBalancedTrueForValid() {
        assertThat(TransactionValidator.isBalanced(balanced())).isTrue();
    }

    @Test
    void isBalancedFalseForEmptyOrUnbalancedOrMixedCurrency() {
        Transaction empty = new Transaction("e", "t1", NOW, "m");
        assertThat(TransactionValidator.isBalanced(empty)).isFalse();

        Transaction unbalanced = new Transaction("u", "t1", NOW, "m");
        unbalanced.addEntry(
                new LedgerEntry(
                        "e1",
                        "t1",
                        "a",
                        Direction.DEBIT,
                        Money.of("10.00", "CNY"),
                        "u",
                        "",
                        "m",
                        1L,
                        NOW));
        unbalanced.addEntry(
                new LedgerEntry(
                        "e2",
                        "t1",
                        "b",
                        Direction.CREDIT,
                        Money.of("9.00", "CNY"),
                        "u",
                        "",
                        "m",
                        2L,
                        NOW));
        assertThat(TransactionValidator.isBalanced(unbalanced)).isFalse();

        Transaction mixed = new Transaction("m", "t1", NOW, "m");
        mixed.addEntry(
                new LedgerEntry(
                        "e1",
                        "t1",
                        "a",
                        Direction.DEBIT,
                        Money.of("10.00", "CNY"),
                        "m",
                        "",
                        "m",
                        1L,
                        NOW));
        mixed.addEntry(
                new LedgerEntry(
                        "e2",
                        "t1",
                        "b",
                        Direction.CREDIT,
                        Money.of("10.00", "USD"),
                        "m",
                        "",
                        "m",
                        2L,
                        NOW));
        assertThat(TransactionValidator.isBalanced(mixed)).isFalse();
    }

    @Test
    void validateThrowsEmpty() {
        Transaction empty = new Transaction("e", "t1", NOW, "m");
        assertThatThrownBy(() -> TransactionValidator.validate(empty))
                .isInstanceOf(EmptyTransactionException.class)
                .satisfies(
                        e ->
                                assertThat(((EmptyTransactionException) e).getTransactionId())
                                        .isEqualTo("e"));
    }

    @Test
    void validateThrowsUnbalancedWithTotals() {
        Transaction unbalanced = new Transaction("u", "t1", NOW, "m");
        unbalanced.addEntry(
                new LedgerEntry(
                        "e1",
                        "t1",
                        "a",
                        Direction.DEBIT,
                        Money.of("10.00", "CNY"),
                        "u",
                        "",
                        "m",
                        1L,
                        NOW));
        unbalanced.addEntry(
                new LedgerEntry(
                        "e2",
                        "t1",
                        "b",
                        Direction.CREDIT,
                        Money.of("9.00", "CNY"),
                        "u",
                        "",
                        "m",
                        2L,
                        NOW));
        assertThatThrownBy(() -> TransactionValidator.validate(unbalanced))
                .isInstanceOf(UnbalancedEntryException.class)
                .satisfies(
                        e -> {
                            UnbalancedEntryException ex = (UnbalancedEntryException) e;
                            assertThat(ex.getDebitTotal()).isEqualTo(Money.of("10.00", "CNY"));
                            assertThat(ex.getCreditTotal()).isEqualTo(Money.of("9.00", "CNY"));
                        });
    }

    @Test
    void validateThrowsCurrencyMismatch() {
        Transaction mixed = new Transaction("m", "t1", NOW, "m");
        mixed.addEntry(
                new LedgerEntry(
                        "e1",
                        "t1",
                        "a",
                        Direction.DEBIT,
                        Money.of("10.00", "CNY"),
                        "m",
                        "",
                        "m",
                        1L,
                        NOW));
        mixed.addEntry(
                new LedgerEntry(
                        "e2",
                        "t1",
                        "b",
                        Direction.CREDIT,
                        Money.of("10.00", "USD"),
                        "m",
                        "",
                        "m",
                        2L,
                        NOW));
        assertThatThrownBy(() -> TransactionValidator.validate(mixed))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void validateNullTransactionRejected() {
        assertThatThrownBy(() -> TransactionValidator.validate(null))
                .isInstanceOf(NullPointerException.class);
    }
}
