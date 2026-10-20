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

/**
 * 为什么测这些：SuggestedEntry/SettlementInstruction/ExternalStatement/ExternalPaymentEvent/VerifyContext
 * 这类值对象在记账建议、清结算、对账、验签之间传递，null 约束、不可变拷贝与相等约定必须稳定。
 */
class ValueObjectEdgeTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    @Test
    void suggestedEntryNullGuards() {
        Money amt = Money.of("1.00", "CNY");
        assertThatThrownBy(() -> new SuggestedEntry(null, Direction.DEBIT, amt, "m"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SuggestedEntry("acc", null, amt, "m"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SuggestedEntry("acc", Direction.DEBIT, null, "m"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void suggestedEntryMemoMayBeNull() {
        SuggestedEntry e =
                new SuggestedEntry("acc", Direction.DEBIT, Money.of("1.00", "CNY"), null);
        assertThat(e.getMemo()).isNull();
    }

    @Test
    void settlementInstructionCopiesSources() {
        SettlementInstruction a =
                new SettlementInstruction("A", "B", Money.of("10.00", "CNY"), List.of("t1", "t2"));
        // 源列表是不可变拷贝，外部修改不影响内部。
        List<String> sources = a.getSources();
        assertThat(sources).containsExactly("t1", "t2");
        assertThatThrownBy(() -> sources.add("t3"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void settlementInstructionNullSourcesTreatedAsEmpty() {
        SettlementInstruction a =
                new SettlementInstruction("A", "B", Money.of("10.00", "CNY"), null);
        assertThat(a.getSources()).isEmpty();
    }

    @Test
    void settlementInstructionNotEqualToDifferentNet() {
        SettlementInstruction a =
                new SettlementInstruction("A", "B", Money.of("10.00", "CNY"), List.of("t1"));
        SettlementInstruction b =
                new SettlementInstruction("A", "B", Money.of("11.00", "CNY"), List.of("t1"));
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void externalStatementNullGuards() {
        assertThatThrownBy(() -> new ExternalStatement(null, Money.of("1", "CNY"), NOW, "w"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ExternalStatement("R1", null, NOW, "w"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void externalStatementChannelMayBeNull() {
        ExternalStatement s = new ExternalStatement("R1", Money.of("1", "CNY"), NOW, null);
        assertThat(s.getChannel()).isNull();
        assertThat(s.getOccurredAt()).isEqualTo(NOW);
    }

    @Test
    void externalPaymentEventNullGuards() {
        assertThatThrownBy(() -> new ExternalPaymentEvent(null, Money.of("1", "CNY"), "w", NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ExternalPaymentEvent("R1", null, "w", NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void verifyContextNullGuards() {
        assertThatThrownBy(
                        () ->
                                new VerifyContext(
                                        null, "b", "s", "sec", SignatureAlgorithm.HMAC_SHA256))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void verifyContextDefaultsAlgorithmWhenNull() {
        VerifyContext ctx = new VerifyContext("w", "body", "sig", "secret", null);
        assertThat(ctx.getAlgorithm()).isEqualTo(SignatureAlgorithm.HMAC_SHA256);
    }
}
