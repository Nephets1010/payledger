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

class SettlementEngineTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    private Transaction transfer(String id, String from, String to, String amount) {
        Transaction tx = new Transaction(id, "t1", NOW, "transfer");
        tx.addEntry(
                new LedgerEntry(
                        id + "-d",
                        "t1",
                        from,
                        Direction.DEBIT,
                        Money.of(amount, "CNY"),
                        id,
                        "",
                        "m",
                        1L,
                        NOW));
        tx.addEntry(
                new LedgerEntry(
                        id + "-c",
                        "t1",
                        to,
                        Direction.CREDIT,
                        Money.of(amount, "CNY"),
                        id,
                        "",
                        "m",
                        2L,
                        NOW));
        tx.setFrom(from);
        tx.setTo(to);
        return tx;
    }

    @Test
    void netsOppositeTransfers() {
        SettlementEngine engine = new SettlementEngine();
        List<SettlementInstruction> instructions =
                engine.net(
                        "t1",
                        List.of(
                                transfer("t1", "A", "B", "100.00"),
                                transfer("t2", "B", "A", "60.00")));
        assertThat(instructions).hasSize(1);
        SettlementInstruction instr = instructions.get(0);
        assertThat(instr.getFrom()).isEqualTo("A");
        assertThat(instr.getTo()).isEqualTo("B");
        assertThat(instr.getNetAmount()).isEqualTo(Money.of("40.00", "CNY"));
        assertThat(instr.getSources()).containsExactly("t1", "t2");
    }

    @Test
    void fullyOffsetProducesNoInstruction() {
        SettlementEngine engine = new SettlementEngine();
        List<SettlementInstruction> instructions =
                engine.net(
                        "t1",
                        List.of(
                                transfer("t1", "A", "B", "100.00"),
                                transfer("t2", "B", "A", "100.00")));
        assertThat(instructions).isEmpty();
    }

    @Test
    void ignoresTransactionsWithoutCounterparty() {
        SettlementEngine engine = new SettlementEngine();
        Transaction tx = transfer("t1", "A", "B", "100.00");
        tx.setFrom(null);
        List<SettlementInstruction> instructions = engine.net("t1", List.of(tx));
        assertThat(instructions).isEmpty();
    }

    @Test
    void nullInputsTreatAsEmpty() {
        SettlementEngine engine = new SettlementEngine();
        assertThat(engine.net("t1", null)).isEmpty();
    }
}
