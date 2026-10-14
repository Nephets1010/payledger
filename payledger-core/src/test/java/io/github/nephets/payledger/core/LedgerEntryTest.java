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

class LedgerEntryTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    @Test
    void nineArgConstructorDefaultsReferenceToEmpty() {
        LedgerEntry e =
                new LedgerEntry(
                        "e1",
                        "t1",
                        "acc1",
                        Direction.DEBIT,
                        Money.of("10.00", "CNY"),
                        "tx1",
                        "memo",
                        1L,
                        NOW);
        assertThat(e.getReference()).isEmpty();
        assertThat(e.getMemo()).isEqualTo("memo");
        assertThat(e.getSequenceNo()).isEqualTo(1L);
    }

    @Test
    void tenArgConstructorKeepsReference() {
        LedgerEntry e =
                new LedgerEntry(
                        "e1",
                        "t1",
                        "acc1",
                        Direction.CREDIT,
                        Money.of("10.00", "CNY"),
                        "tx1",
                        "REF-1",
                        "memo",
                        2L,
                        NOW);
        assertThat(e.getReference()).isEqualTo("REF-1");
        assertThat(e.getDirection()).isEqualTo(Direction.CREDIT);
        assertThat(e.getCreatedAt()).isEqualTo(NOW);
        assertThat(e.getAmount()).isEqualTo(Money.of("10.00", "CNY"));
    }

    @Test
    void nullReferenceBecomesEmpty() {
        LedgerEntry e =
                new LedgerEntry(
                        "e1",
                        "t1",
                        "acc1",
                        Direction.DEBIT,
                        Money.of("10.00", "CNY"),
                        "tx1",
                        null,
                        "memo",
                        1L,
                        NOW);
        assertThat(e.getReference()).isEmpty();
    }

    @Test
    void nullRejected() {
        assertThatThrownBy(
                        () ->
                                new LedgerEntry(
                                        null,
                                        "t1",
                                        "acc1",
                                        Direction.DEBIT,
                                        Money.of("10.00", "CNY"),
                                        "tx1",
                                        "",
                                        "m",
                                        1L,
                                        NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void equalsHashCodeToString() {
        LedgerEntry a = sample("e1");
        LedgerEntry b = sample("e1");
        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(sample("e2"));
        assertThat(a).isNotEqualTo("x");
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains("e1");
    }

    private LedgerEntry sample(String id) {
        return new LedgerEntry(
                id,
                "t1",
                "acc1",
                Direction.DEBIT,
                Money.of("10.00", "CNY"),
                "tx1",
                "REF",
                "memo",
                1L,
                NOW);
    }
}
