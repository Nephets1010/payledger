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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReconcilerTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    private LedgerEntry entry(String ref, String amount) {
        return new LedgerEntry(
                "e1",
                "t1",
                "acc",
                Direction.DEBIT,
                Money.of(amount, "CNY"),
                "tx1",
                ref,
                "m",
                1L,
                NOW);
    }

    private ExternalStatement statement(String ref, String amount) {
        return new ExternalStatement(ref, Money.of(amount, "CNY"), NOW, "wechat");
    }

    @Test
    void matchesOnReferenceAndAmount() {
        Reconciler r = new Reconciler();
        ReconciliationReport report =
                r.reconcile(
                        "t1", List.of(entry("R1", "100.00")), List.of(statement("R1", "100.00")));
        assertThat(report.getMatched()).hasSize(1);
        assertThat(report.isClean()).isTrue();
        assertThat(report.getUnmatchedInternal()).isEmpty();
        assertThat(report.getUnmatchedExternal()).isEmpty();
        assertThat(report.getDiscrepancies()).isEmpty();
    }

    @Test
    void amountsComparedByAbsoluteValue() {
        Reconciler r = new Reconciler();
        ReconciliationReport report =
                r.reconcile(
                        "t1", List.of(entry("R1", "100.00")), List.of(statement("R1", "-100.00")));
        assertThat(report.getMatched()).hasSize(1);
    }

    @Test
    void internalWithoutExternalIsUnmatched() {
        Reconciler r = new Reconciler();
        ReconciliationReport report =
                r.reconcile(
                        "t1", List.of(entry("R9", "100.00")), List.of(statement("R1", "100.00")));
        assertThat(report.getUnmatchedInternal()).hasSize(1);
        assertThat(report.getUnmatchedExternal()).hasSize(1);
    }

    @Test
    void referenceExistsButAmountDiffersIsDiscrepancy() {
        Reconciler r = new Reconciler();
        ReconciliationReport report =
                r.reconcile(
                        "t1", List.of(entry("R1", "100.00")), List.of(statement("R1", "90.00")));
        assertThat(report.getDiscrepancies()).hasSize(1);
        assertThat(report.getDiscrepancies().get(0).getReference()).isEqualTo("R1");
        assertThat(report.getUnmatchedInternal()).hasSize(1);
        assertThat(report.getUnmatchedExternal()).hasSize(1);
    }

    @Test
    void nullInputsTreatAsEmpty() {
        Reconciler r = new Reconciler();
        ReconciliationReport report = r.reconcile("t1", null, null);
        assertThat(report.isClean()).isTrue();
    }
}
