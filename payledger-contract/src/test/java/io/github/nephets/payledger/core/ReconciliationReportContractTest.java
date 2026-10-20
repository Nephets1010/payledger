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

/**
 * 为什么测这些：对账结果是「是否对平」的唯一判定依据，isClean 的语义（有正常配对不算脏、有未匹配/差异即脏）、Matcher/Discrepancy
 * 的内部结构，是下游告警与人工复核的契约。
 */
class ReconciliationReportContractTest {

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
    void cleanWhenOnlyMatched() {
        ReconciliationReport report =
                new ReconciliationReport(
                        List.of(
                                new ReconciliationReport.MatchedPair(
                                        entry("R1", "1"), statement("R1", "1"))),
                        List.of(),
                        List.of(),
                        List.of());
        assertThat(report.isClean()).isTrue();
        assertThat(report.getMatched()).hasSize(1);
    }

    @Test
    void dirtyWhenUnmatchedInternal() {
        ReconciliationReport report =
                new ReconciliationReport(
                        List.of(), List.of(entry("R9", "1")), List.of(), List.of());
        assertThat(report.isClean()).isFalse();
    }

    @Test
    void dirtyWhenUnmatchedExternal() {
        ReconciliationReport report =
                new ReconciliationReport(
                        List.of(), List.of(), List.of(statement("R9", "1")), List.of());
        assertThat(report.isClean()).isFalse();
    }

    @Test
    void dirtyWhenDiscrepancy() {
        ReconciliationReport report =
                new ReconciliationReport(
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(
                                new ReconciliationReport.Discrepancy(
                                        "R1", Money.of("10", "CNY"), Money.of("9", "CNY"))));
        assertThat(report.isClean()).isFalse();
    }

    @Test
    void matchedPairExposesBothSides() {
        LedgerEntry e = entry("R1", "1");
        ExternalStatement s = statement("R1", "1");
        ReconciliationReport.MatchedPair pair = new ReconciliationReport.MatchedPair(e, s);
        assertThat(pair.getEntry()).isEqualTo(e);
        assertThat(pair.getStatement()).isEqualTo(s);
    }

    @Test
    void discrepancyExposesAmounts() {
        ReconciliationReport.Discrepancy d =
                new ReconciliationReport.Discrepancy(
                        "R1", Money.of("10", "CNY"), Money.of("9", "CNY"));
        assertThat(d.getReference()).isEqualTo("R1");
        assertThat(d.getInternalAmount()).isEqualTo(Money.of("10", "CNY"));
        assertThat(d.getExternalAmount()).isEqualTo(Money.of("9", "CNY"));
    }

    @Test
    void enumsResolveByName() {
        assertThat(AccountStatus.valueOf("FROZEN")).isEqualTo(AccountStatus.FROZEN);
        assertThat(AccountStatus.values())
                .containsExactly(AccountStatus.OPEN, AccountStatus.CLOSED, AccountStatus.FROZEN);
        assertThat(TransactionStatus.valueOf("POSTED")).isEqualTo(TransactionStatus.POSTED);
        assertThat(TransactionStatus.values())
                .containsExactly(
                        TransactionStatus.PENDING,
                        TransactionStatus.POSTED,
                        TransactionStatus.REVERSED);
    }

    @Test
    void accountTypeLabels() {
        assertThat(AccountType.ASSET.getLabel()).isEqualTo("资产");
        assertThat(AccountType.values()).hasSize(5);
    }

    @Test
    void signatureAlgorithmIsHmacSha256() {
        assertThat(SignatureAlgorithm.HMAC_SHA256)
                .isEqualTo(SignatureAlgorithm.valueOf("HMAC_SHA256"));
    }
}
