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

/** 为什么测这些：分录是不可变的最小记账单元，reference 缺省为空的约定、长/短构造器等价性、相等与序号语义，是被存储与对账反复依赖的契约。 */
class LedgerEntryContractTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    private LedgerEntry full(String ref) {
        return new LedgerEntry(
                "e1",
                "t1",
                "acc",
                Direction.DEBIT,
                Money.of("10.00", "CNY"),
                "tx1",
                ref,
                "memo",
                7L,
                NOW);
    }

    @Test
    void nullReferenceBecomesEmptyString() {
        LedgerEntry e =
                new LedgerEntry(
                        "e1",
                        "t1",
                        "acc",
                        Direction.DEBIT,
                        Money.of("10.00", "CNY"),
                        "tx1",
                        null,
                        "memo",
                        7L,
                        NOW);
        assertThat(e.getReference()).isEmpty();
    }

    @Test
    void shortConstructorEqualsFullConstructorWithEmptyReference() {
        LedgerEntry shortCtor =
                new LedgerEntry(
                        "e1",
                        "t1",
                        "acc",
                        Direction.DEBIT,
                        Money.of("10.00", "CNY"),
                        "tx1",
                        "memo",
                        7L,
                        NOW);
        LedgerEntry fullCtor = full("");
        assertThat(shortCtor).isEqualTo(fullCtor);
    }

    @Test
    void sequenceNoIsExposed() {
        assertThat(full("R1").getSequenceNo()).isEqualTo(7L);
    }

    @Test
    void accessorsExposeAllFields() {
        LedgerEntry e = full("R1");
        assertThat(e.getEntryId()).isEqualTo("e1");
        assertThat(e.getTenantId()).isEqualTo("t1");
        assertThat(e.getAccountId()).isEqualTo("acc");
        assertThat(e.getDirection()).isEqualTo(Direction.DEBIT);
        assertThat(e.getAmount()).isEqualTo(Money.of("10.00", "CNY"));
        assertThat(e.getTransactionId()).isEqualTo("tx1");
        assertThat(e.getReference()).isEqualTo("R1");
        assertThat(e.getMemo()).isEqualTo("memo");
        assertThat(e.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void equalsAndHashCodeByAllFields() {
        LedgerEntry a = full("R1");
        LedgerEntry b = full("R1");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equalsDiffersByReference() {
        assertThat(full("R1")).isNotEqualTo(full("R2"));
    }

    @Test
    void nullGuardsOnRequiredFields() {
        assertThatThrownBy(
                        () ->
                                new LedgerEntry(
                                        null,
                                        "t1",
                                        "acc",
                                        Direction.DEBIT,
                                        Money.of("1", "CNY"),
                                        "tx1",
                                        "",
                                        "m",
                                        1L,
                                        NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(
                        () ->
                                new LedgerEntry(
                                        "e",
                                        "t1",
                                        "acc",
                                        Direction.DEBIT,
                                        Money.of("1", "CNY"),
                                        "tx1",
                                        "",
                                        "m",
                                        1L,
                                        null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void notEqualToDifferentType() {
        assertThat(full("R1")).isNotEqualTo("x");
    }
}
