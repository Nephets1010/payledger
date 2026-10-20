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

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 为什么测这些：交易是聚合根，addEntry 的租户一致性约束、不可变分录列表、isBalanced 委托、状态流转与 from/to 对手方字段，都是过账与清结算的前提。 */
class TransactionContractTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    private Transaction newTx() {
        return new Transaction("tx1", "t1", NOW, "memo");
    }

    private LedgerEntry entry(String id, Direction d, String amount) {
        return new LedgerEntry(
                id, "t1", "acc", d, Money.of(amount, "CNY"), "tx1", "", "m", 1L, NOW);
    }

    @Test
    void addEntryEnforcesTenantConsistency() {
        Transaction tx = newTx();
        assertThatThrownBy(
                        () ->
                                tx.addEntry(
                                        new LedgerEntry(
                                                "e",
                                                "OTHER_TENANT",
                                                "acc",
                                                Direction.DEBIT,
                                                Money.of("1.00", "CNY"),
                                                "tx1",
                                                "",
                                                "m",
                                                1L,
                                                NOW)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addEntryNullRejected() {
        assertThatThrownBy(() -> newTx().addEntry(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void isBalancedTrueForBalancedTx() {
        Transaction tx = newTx();
        tx.addEntry(entry("e1", Direction.DEBIT, "10.00"));
        tx.addEntry(entry("e2", Direction.CREDIT, "10.00"));
        assertThat(tx.isBalanced()).isTrue();
    }

    @Test
    void isBalancedFalseForUnbalancedTx() {
        Transaction tx = newTx();
        tx.addEntry(entry("e1", Direction.DEBIT, "10.00"));
        tx.addEntry(entry("e2", Direction.CREDIT, "9.00"));
        assertThat(tx.isBalanced()).isFalse();
    }

    @Test
    void getEntriesReturnsImmutableCopy() {
        Transaction tx = newTx();
        tx.addEntry(entry("e1", Direction.DEBIT, "10.00"));
        assertThat(tx.getEntries()).hasSize(1);
        // 返回的是防御性拷贝，外部无法篡改内部分录。
        assertThatThrownBy(() -> tx.getEntries().add(entry("e2", Direction.CREDIT, "10.00")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void setStatusNullRejected() {
        assertThatThrownBy(() -> newTx().setStatus(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void defaultStatusIsPending() {
        assertThat(newTx().getStatus()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void fromAndToAccessors() {
        Transaction tx = newTx();
        tx.setFrom("A");
        tx.setTo("B");
        assertThat(tx.getFrom()).isEqualTo("A");
        assertThat(tx.getTo()).isEqualTo("B");
    }

    @Test
    void constructorNullGuards() {
        assertThatThrownBy(() -> new Transaction(null, "t1", NOW, "m"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Transaction("tx", null, NOW, "m"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Transaction("tx", "t1", null, "m"))
                .isInstanceOf(NullPointerException.class);
    }
}
