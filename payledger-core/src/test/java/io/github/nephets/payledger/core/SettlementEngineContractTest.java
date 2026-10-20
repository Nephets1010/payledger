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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 为什么测这些：清结算把多笔对手方交易压成净额指令，方向相反自动对冲、无对手方跳过、来源归集，是降低实际转账笔数的关键，规则必须精确。 */
class SettlementEngineContractTest {

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
    void threePartyNet() {
        SettlementEngine engine = new SettlementEngine();
        // A→B 100, B→C 100, C→A 40 ⇒ A→B 100? 计算矩阵后净额：A→B 100, B→C 100, C→A 40，
        // 反向：B→A -100, C→B -100, A→C -40。只保留正向(signum>0)：A→B 100, B→C 100, C→A 40。
        List<SettlementInstruction> instructions =
                engine.net(
                        "t1",
                        List.of(
                                transfer("t1", "A", "B", "100.00"),
                                transfer("t2", "B", "C", "100.00"),
                                transfer("t3", "C", "A", "40.00")));
        assertThat(instructions).hasSize(3);
        assertThat(instructions)
                .anyMatch(
                        i ->
                                i.getFrom().equals("A")
                                        && i.getTo().equals("B")
                                        && i.getNetAmount().equals(Money.of("100.00", "CNY")));
        assertThat(instructions)
                .anyMatch(
                        i ->
                                i.getFrom().equals("B")
                                        && i.getTo().equals("C")
                                        && i.getNetAmount().equals(Money.of("100.00", "CNY")));
        assertThat(instructions)
                .anyMatch(
                        i ->
                                i.getFrom().equals("C")
                                        && i.getTo().equals("A")
                                        && i.getNetAmount().equals(Money.of("40.00", "CNY")));
    }

    @Test
    void chainsOfOffsetsCollapse() {
        SettlementEngine engine = new SettlementEngine();
        // A→B 100, B→A 100 完全对冲 ⇒ 无指令。
        List<SettlementInstruction> instructions =
                engine.net(
                        "t1",
                        List.of(
                                transfer("t1", "A", "B", "100.00"),
                                transfer("t2", "B", "A", "100.00")));
        assertThat(instructions).isEmpty();
    }

    @Test
    void transactionWithoutDebitSkipped() {
        SettlementEngine engine = new SettlementEngine();
        Transaction tx = transfer("t1", "A", "B", "100.00");
        // 把所有分录改成贷方 ⇒ debitTotal 为 null ⇒ 该交易被跳过。
        tx.getEntries().forEach(e -> {}); // 不改；这里直接构造一个无借方的交易。
        Transaction noDebit = new Transaction("t2", "t1", NOW, "no-debit");
        noDebit.addEntry(
                new LedgerEntry(
                        "x-c",
                        "t1",
                        "B",
                        Direction.CREDIT,
                        Money.of("100.00", "CNY"),
                        "t2",
                        "",
                        "m",
                        1L,
                        NOW));
        noDebit.setFrom("A");
        noDebit.setTo("B");
        assertThat(engine.net("t1", List.of(noDebit))).isEmpty();
    }

    @Test
    void aggregatesMultipleSourcesIntoOneInstruction() {
        SettlementEngine engine = new SettlementEngine();
        List<SettlementInstruction> instructions =
                engine.net(
                        "t1",
                        List.of(
                                transfer("t1", "A", "B", "30.00"),
                                transfer("t2", "A", "B", "70.00")));
        assertThat(instructions).hasSize(1);
        assertThat(instructions.get(0).getNetAmount()).isEqualTo(Money.of("100.00", "CNY"));
        assertThat(instructions.get(0).getSources()).containsExactlyInAnyOrder("t1", "t2");
    }

    @Test
    void nullTenantThrows() {
        assertThatThrownBy(() -> new SettlementEngine().net(null, List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void emptyAndNullInputProduceNoInstructions() {
        SettlementEngine engine = new SettlementEngine();
        assertThat(engine.net("t1", List.of())).isEmpty();
        assertThat(engine.net("t1", null)).isEmpty();
    }

    @Test
    void singleTransferYieldsOneInstruction() {
        SettlementEngine engine = new SettlementEngine();
        List<SettlementInstruction> instructions =
                engine.net("t1", List.of(transfer("t1", "A", "B", "55.00")));
        assertThat(instructions).hasSize(1);
        assertThat(instructions.get(0).getFrom()).isEqualTo("A");
        assertThat(instructions.get(0).getTo()).isEqualTo("B");
        assertThat(instructions.get(0).getNetAmount()).isEqualTo(Money.of("55.00", "CNY"));
    }
}
