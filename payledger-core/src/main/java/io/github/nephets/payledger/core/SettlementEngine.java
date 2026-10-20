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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 清结算：把多笔带对手方的交易做多边净额，得到每个 (from, to) 的净应收/应付。
 *
 * <p>对每笔交易，取借方合计作为转移金额：在矩阵里给 {@code (from, to)} 加、给 {@code (to, from)}
 * 减。最后只保留净额非零的指令；方向相反的两笔会自动对冲（例如 A→B 100、B→A 60 合并为 A→B 40）。
 */
public final class SettlementEngine {

    private static final Logger log = LoggerFactory.getLogger(SettlementEngine.class);

    public List<SettlementInstruction> net(String tenantId, List<Transaction> txs) {
        Objects.requireNonNull(tenantId, "tenantId 不能为 null");
        List<Transaction> list = txs == null ? List.of() : txs;
        log.info("net settlement start tenantId={} txCount={}", tenantId, list.size());

        Map<String, Map<String, NetAgg>> matrix = new LinkedHashMap<>();
        for (Transaction tx : list) {
            String from = tx.getFrom();
            String to = tx.getTo();
            if (from == null || to == null) {
                continue;
            }
            Money amount = debitTotal(tx);
            if (amount == null) {
                continue;
            }
            add(matrix, from, to, amount, tx.getTransactionId());
            add(matrix, to, from, amount.negate(), tx.getTransactionId());
        }

        List<SettlementInstruction> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, NetAgg>> fromEntry : matrix.entrySet()) {
            String from = fromEntry.getKey();
            for (Map.Entry<String, NetAgg> toEntry : fromEntry.getValue().entrySet()) {
                String to = toEntry.getKey();
                NetAgg agg = toEntry.getValue();
                // 只保留正向净额（from 应收 to）；反向的相反数已在同一矩阵里，避免重复指令。
                if (agg.net.getAmount().signum() > 0) {
                    result.add(
                            new SettlementInstruction(from, to, agg.net, List.copyOf(agg.sources)));
                }
            }
        }
        log.debug("net settlement done tenantId={} instructions={}", tenantId, result.size());
        return result;
    }

    private void add(
            Map<String, Map<String, NetAgg>> matrix,
            String from,
            String to,
            Money amount,
            String txId) {
        NetAgg agg =
                matrix.computeIfAbsent(from, k -> new LinkedHashMap<>())
                        .computeIfAbsent(to, k -> new NetAgg());
        agg.net = agg.net == null ? amount : agg.net.add(amount);
        agg.sources.add(txId);
    }

    private Money debitTotal(Transaction tx) {
        BigDecimal sum = BigDecimal.ZERO;
        String currency = null;
        for (LedgerEntry entry : tx.getEntries()) {
            if (entry.getDirection() == Direction.DEBIT) {
                if (currency == null) {
                    currency = entry.getAmount().getCurrency();
                }
                sum = sum.add(entry.getAmount().getAmount());
            }
        }
        return currency == null ? null : Money.of(sum, currency);
    }

    private static final class NetAgg {

        Money net;

        final List<String> sources = new ArrayList<>();
    }
}
