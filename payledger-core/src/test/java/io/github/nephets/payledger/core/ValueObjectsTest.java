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

class ValueObjectsTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    @Test
    void suggestedEntryAccessorsAndEquality() {
        SuggestedEntry a =
                new SuggestedEntry("acc", Direction.DEBIT, Money.of("10.00", "CNY"), "memo");
        SuggestedEntry b =
                new SuggestedEntry("acc", Direction.DEBIT, Money.of("10.00", "CNY"), "memo");
        assertThat(a.getAccountId()).isEqualTo("acc");
        assertThat(a.getDirection()).isEqualTo(Direction.DEBIT);
        assertThat(a.getAmount()).isEqualTo(Money.of("10.00", "CNY"));
        assertThat(a.getMemo()).isEqualTo("memo");
        assertThat(a).isEqualTo(b);
        assertThat(a)
                .isNotEqualTo(
                        new SuggestedEntry(
                                "acc", Direction.CREDIT, Money.of("10.00", "CNY"), "memo"));
        assertThat(a).isNotEqualTo("x");
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void suggestedEntryNullRejected() {
        assertThatThrownBy(() -> new SuggestedEntry(null, Direction.DEBIT, Money.zero("CNY"), "m"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void externalStatementAccessorsAndEquality() {
        ExternalStatement a = new ExternalStatement("R1", Money.of("10.00", "CNY"), NOW, "wechat");
        ExternalStatement b = new ExternalStatement("R1", Money.of("10.00", "CNY"), NOW, "wechat");
        assertThat(a.getReference()).isEqualTo("R1");
        assertThat(a.getChannel()).isEqualTo("wechat");
        assertThat(a.getOccurredAt()).isEqualTo(NOW);
        assertThat(a).isEqualTo(b);
        assertThat(a)
                .isNotEqualTo(new ExternalStatement("R2", Money.of("10.00", "CNY"), NOW, "wechat"));
        assertThat(a).isNotEqualTo("x");
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains("R1");
    }

    @Test
    void externalPaymentEventAccessorsAndEquality() {
        ExternalPaymentEvent a =
                new ExternalPaymentEvent("R1", Money.of("10.00", "CNY"), "wechat", NOW);
        ExternalPaymentEvent b =
                new ExternalPaymentEvent("R1", Money.of("10.00", "CNY"), "wechat", NOW);
        assertThat(a.getReference()).isEqualTo("R1");
        assertThat(a.getAmount()).isEqualTo(Money.of("10.00", "CNY"));
        assertThat(a.getChannel()).isEqualTo("wechat");
        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo("x");
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void settlementInstructionAccessorsAndEquality() {
        SettlementInstruction a =
                new SettlementInstruction(
                        "A", "B", Money.of("10.00", "CNY"), java.util.List.of("t1"));
        SettlementInstruction b =
                new SettlementInstruction(
                        "A", "B", Money.of("10.00", "CNY"), java.util.List.of("t1"));
        assertThat(a.getFrom()).isEqualTo("A");
        assertThat(a.getTo()).isEqualTo("B");
        assertThat(a.getNetAmount()).isEqualTo(Money.of("10.00", "CNY"));
        assertThat(a.getSources()).containsExactly("t1");
        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo("x");
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains("A");
    }

    @Test
    void verifyContextDefaultsAlgorithm() {
        VerifyContext ctx = new VerifyContext("w", "body", "sig", "secret", null);
        assertThat(ctx.getChannelId()).isEqualTo("w");
        assertThat(ctx.getAlgorithm()).isEqualTo(SignatureAlgorithm.HMAC_SHA256);
        assertThat(ctx.getRawBody()).isEqualTo("body");
        assertThat(ctx.getSignature()).isEqualTo("sig");
    }
}
