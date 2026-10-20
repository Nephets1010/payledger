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

class TransactionTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    private Transaction balanced() {
        Transaction tx = new Transaction("tx1", "t1", NOW, "收款");
        tx.addEntry(entry("e1", "acc_cash", Direction.DEBIT, "100.00"));
        tx.addEntry(entry("e2", "acc_rev", Direction.CREDIT, "100.00"));
        return tx;
    }

    private LedgerEntry entry(String id, String account, Direction dir, String amount) {
        return new LedgerEntry(
                id, "t1", account, dir, Money.of(amount, "CNY"), "tx1", "", "m", 1L, NOW);
    }

    @Test
    void addEntryAndReadBack() {
        Transaction tx = balanced();
        assertThat(tx.getEntries()).hasSize(2);
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(tx.getTransactionId()).isEqualTo("tx1");
        assertThat(tx.getTenantId()).isEqualTo("t1");
    }

    @Test
    void addEntryRejectsWrongTenant() {
        Transaction tx = new Transaction("tx1", "t1", NOW, "m");
        LedgerEntry wrong =
                new LedgerEntry(
                        "e1",
                        "t2",
                        "acc",
                        Direction.DEBIT,
                        Money.of("1.00", "CNY"),
                        "tx1",
                        "",
                        "m",
                        1L,
                        NOW);
        assertThatThrownBy(() -> tx.addEntry(wrong)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isBalancedReflectsContents() {
        assertThat(balanced().isBalanced()).isTrue();
        Transaction unbalanced = new Transaction("tx2", "t1", NOW, "m");
        unbalanced.addEntry(entry("e1", "a", Direction.DEBIT, "100.00"));
        assertThat(unbalanced.isBalanced()).isFalse();
    }

    @Test
    void fromToAndStatus() {
        Transaction tx = balanced();
        tx.setFrom("A");
        tx.setTo("B");
        tx.setStatus(TransactionStatus.POSTED);
        assertThat(tx.getFrom()).isEqualTo("A");
        assertThat(tx.getTo()).isEqualTo("B");
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.POSTED);
        assertThatThrownBy(() -> tx.setStatus(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullConstructorArgsRejected() {
        assertThatThrownBy(() -> new Transaction(null, "t1", NOW, "m"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Transaction("tx1", null, NOW, "m"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Transaction("tx1", "t1", null, "m"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void toStringContainsId() {
        assertThat(balanced().toString()).contains("tx1");
    }
}
