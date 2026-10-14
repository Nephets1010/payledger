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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 对账：把内部账分录与外部渠道流水按「单号相同 且 金额（绝对值）相等」配对。
 *
 * <p>匹配规则：
 *
 * <ul>
 *   <li>单号相同且金额绝对值相等 → 进 {@code matched}；
 *   <li>单号在外部存在、但金额对不上 → 进 {@code discrepancies}（同时该外部流水仍算未匹配）；
 *   <li>内部有单号但外部完全没有 → 进 {@code unmatchedInternal}；
 *   <li>外部剩余未配对的 → 进 {@code unmatchedExternal}。
 * </ul>
 */
public final class Reconciler {

    public ReconciliationReport reconcile(
            String tenantId, List<LedgerEntry> internal, List<ExternalStatement> external) {
        Objects.requireNonNull(tenantId, "tenantId 不能为 null");
        List<LedgerEntry> ints = internal == null ? List.of() : internal;
        List<ExternalStatement> exts = external == null ? List.of() : external;

        Map<String, List<ExternalStatement>> extByRef = new LinkedHashMap<>();
        for (ExternalStatement statement : exts) {
            extByRef.computeIfAbsent(statement.getReference(), k -> new ArrayList<>())
                    .add(statement);
        }

        List<ReconciliationReport.MatchedPair> matched = new ArrayList<>();
        List<LedgerEntry> unmatchedInternal = new ArrayList<>();
        List<ExternalStatement> unmatchedExternal = new ArrayList<>(exts);
        List<ReconciliationReport.Discrepancy> discrepancies = new ArrayList<>();

        for (LedgerEntry entry : ints) {
            String ref = entry.getReference();
            List<ExternalStatement> candidates = extByRef.getOrDefault(ref, List.of());
            ExternalStatement found = null;
            for (ExternalStatement statement : candidates) {
                BigDecimal internalAbs = entry.getAmount().getAmount().abs();
                BigDecimal externalAbs = statement.getAmount().getAmount().abs();
                if (internalAbs.compareTo(externalAbs) == 0) {
                    found = statement;
                    break;
                }
            }
            if (found != null) {
                matched.add(new ReconciliationReport.MatchedPair(entry, found));
                unmatchedExternal.remove(found);
            } else if (extByRef.containsKey(ref)) {
                ExternalStatement any = extByRef.get(ref).get(0);
                discrepancies.add(
                        new ReconciliationReport.Discrepancy(
                                ref, entry.getAmount(), any.getAmount()));
                unmatchedInternal.add(entry);
            } else {
                unmatchedInternal.add(entry);
            }
        }

        return new ReconciliationReport(
                matched, unmatchedInternal, unmatchedExternal, discrepancies);
    }
}
