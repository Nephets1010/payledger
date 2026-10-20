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

/** 为什么测这些：对账的配对规则（按单号+金额绝对值）、差异项、空集合边界，直接决定资金对账能否发现漏账与错账，必须精确。 */
class ReconcilerContractTest {

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
    void multipleMatchedPairs() {
        Reconciler r = new Reconciler();
        List<LedgerEntry> internal =
                List.of(entry("R1", "10.00"), entry("R2", "20.00"), entry("R3", "30.00"));
        List<ExternalStatement> external =
                List.of(
                        statement("R1", "10.00"),
                        statement("R2", "20.00"),
                        statement("R3", "30.00"));
        ReconciliationReport report = r.reconcile("t1", internal, external);
        assertThat(report.getMatched()).hasSize(3);
        assertThat(report.isClean()).isTrue();
    }

    @Test
    void mixedMatchedUnmatchedAndDiscrepancy() {
        Reconciler r = new Reconciler();
        List<LedgerEntry> internal =
                List.of(entry("R1", "10.00"), entry("R2", "20.00"), entry("R3", "30.00"));
        List<ExternalStatement> external =
                List.of(
                        statement("R1", "10.00"),
                        statement("R2", "99.00"),
                        statement("RX", "5.00"));
        ReconciliationReport report = r.reconcile("t1", internal, external);
        assertThat(report.getMatched()).hasSize(1); // R1 金额相等
        assertThat(report.getDiscrepancies()).hasSize(1); // R2 金额不符
        // R2（差异项，内部侧）与 R3（无对端）都进入未匹配内部；RX 与外部那条差错的 R2 都进入未匹配外部。
        assertThat(report.getUnmatchedInternal()).hasSize(2);
        assertThat(report.getUnmatchedExternal()).hasSize(2);
        assertThat(report.isClean()).isFalse();
    }

    @Test
    void discrepancyPicksFirstCandidateWhenNoMatch() {
        Reconciler r = new Reconciler();
        // 同一单号 R1 在外部出现两条，且都不与内部 100 匹配；按设计取第一条（90）作为差异记录。
        List<LedgerEntry> internal = List.of(entry("R1", "100.00"));
        List<ExternalStatement> external =
                List.of(statement("R1", "90.00"), statement("R1", "80.00"));
        ReconciliationReport report = r.reconcile("t1", internal, external);
        assertThat(report.getMatched()).isEmpty();
        assertThat(report.getDiscrepancies()).hasSize(1);
        assertThat(report.getDiscrepancies().get(0).getReference()).isEqualTo("R1");
        assertThat(report.getDiscrepancies().get(0).getExternalAmount())
                .isEqualTo(Money.of("90.00", "CNY"));
        assertThat(report.getUnmatchedInternal()).hasSize(1);
        assertThat(report.getUnmatchedExternal()).hasSize(2);
    }

    @Test
    void emptyInternalWithExternalOnly() {
        Reconciler r = new Reconciler();
        ReconciliationReport report =
                r.reconcile("t1", List.of(), List.of(statement("R1", "10.00")));
        assertThat(report.getMatched()).isEmpty();
        assertThat(report.getUnmatchedExternal()).hasSize(1);
        assertThat(report.isClean()).isFalse();
    }

    @Test
    void emptyExternalWithInternalOnly() {
        Reconciler r = new Reconciler();
        ReconciliationReport report = r.reconcile("t1", List.of(entry("R1", "10.00")), List.of());
        assertThat(report.getUnmatchedInternal()).hasSize(1);
        assertThat(report.isClean()).isFalse();
    }

    @Test
    void nullReferenceEntryNeverMatches() {
        Reconciler r = new Reconciler();
        LedgerEntry noRef =
                new LedgerEntry(
                        "e0",
                        "t1",
                        "acc",
                        Direction.DEBIT,
                        Money.of("10.00", "CNY"),
                        "tx1",
                        null,
                        "m",
                        1L,
                        NOW);
        ReconciliationReport report =
                r.reconcile("t1", List.of(noRef), List.of(statement("R1", "10.00")));
        // 内部无单号，外部有单号 → 互不配对。
        assertThat(report.getMatched()).isEmpty();
        assertThat(report.getUnmatchedInternal()).hasSize(1);
        assertThat(report.getUnmatchedExternal()).hasSize(1);
    }
}
