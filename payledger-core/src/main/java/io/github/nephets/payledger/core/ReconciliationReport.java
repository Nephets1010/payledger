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

import java.util.List;
import java.util.Objects;

/**
 * 对账结果：内部账与外部流水配对后的四部分。
 *
 * <ul>
 *   <li>{@code matched}：单号相同且金额（绝对值）相等的配对；
 *   <li>{@code unmatchedInternal}：内部有、外部找不到对应单号的分录；
 *   <li>{@code unmatchedExternal}：外部有、内部未配对上的流水；
 *   <li>{@code discrepancies}：单号相同但金额对不上的差异项。
 * </ul>
 */
public final class ReconciliationReport {

    private final List<MatchedPair> matched;

    private final List<LedgerEntry> unmatchedInternal;

    private final List<ExternalStatement> unmatchedExternal;

    private final List<Discrepancy> discrepancies;

    public ReconciliationReport(
            List<MatchedPair> matched,
            List<LedgerEntry> unmatchedInternal,
            List<ExternalStatement> unmatchedExternal,
            List<Discrepancy> discrepancies) {
        this.matched = List.copyOf(matched);
        this.unmatchedInternal = List.copyOf(unmatchedInternal);
        this.unmatchedExternal = List.copyOf(unmatchedExternal);
        this.discrepancies = List.copyOf(discrepancies);
    }

    public List<MatchedPair> getMatched() {
        return matched;
    }

    public List<LedgerEntry> getUnmatchedInternal() {
        return unmatchedInternal;
    }

    public List<ExternalStatement> getUnmatchedExternal() {
        return unmatchedExternal;
    }

    public List<Discrepancy> getDiscrepancies() {
        return discrepancies;
    }

    /** 是否完全对平：无未匹配项、无差异项（有正常配对不算脏）。 */
    public boolean isClean() {
        return unmatchedInternal.isEmpty()
                && unmatchedExternal.isEmpty()
                && discrepancies.isEmpty();
    }

    /** 一笔内部分录与一条外部流水的配对。 */
    public static final class MatchedPair {

        private final LedgerEntry entry;

        private final ExternalStatement statement;

        public MatchedPair(LedgerEntry entry, ExternalStatement statement) {
            this.entry = Objects.requireNonNull(entry);
            this.statement = Objects.requireNonNull(statement);
        }

        public LedgerEntry getEntry() {
            return entry;
        }

        public ExternalStatement getStatement() {
            return statement;
        }
    }

    /** 单号相同但金额对不上的差异项。 */
    public static final class Discrepancy {

        private final String reference;

        private final Money internalAmount;

        private final Money externalAmount;

        public Discrepancy(String reference, Money internalAmount, Money externalAmount) {
            this.reference = reference;
            this.internalAmount = internalAmount;
            this.externalAmount = externalAmount;
        }

        public String getReference() {
            return reference;
        }

        public Money getInternalAmount() {
            return internalAmount;
        }

        public Money getExternalAmount() {
            return externalAmount;
        }
    }
}
