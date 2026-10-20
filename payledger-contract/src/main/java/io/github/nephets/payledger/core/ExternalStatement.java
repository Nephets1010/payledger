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

import java.time.Instant;
import java.util.Objects;

/**
 * 外部流水：支付渠道侧的一笔资金变动记录，用于对账。
 *
 * <p>{@code reference} 是与内部分录配对用的业务单号；{@code amount} 的正负由渠道约定（通常收款为正）。对账只比较绝对值。
 */
public final class ExternalStatement {

    private final String reference;

    private final Money amount;

    private final Instant occurredAt;

    private final String channel;

    public ExternalStatement(String reference, Money amount, Instant occurredAt, String channel) {
        this.reference = Objects.requireNonNull(reference, "reference 不能为 null");
        this.amount = Objects.requireNonNull(amount, "amount 不能为 null");
        this.occurredAt = occurredAt;
        this.channel = channel;
    }

    public String getReference() {
        return reference;
    }

    public Money getAmount() {
        return amount;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getChannel() {
        return channel;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ExternalStatement that)) {
            return false;
        }
        return reference.equals(that.reference)
                && amount.equals(that.amount)
                && Objects.equals(occurredAt, that.occurredAt)
                && Objects.equals(channel, that.channel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reference, amount, occurredAt, channel);
    }

    @Override
    public String toString() {
        return "ExternalStatement{reference='"
                + reference
                + "', amount="
                + amount
                + ", channel='"
                + channel
                + "'}";
    }
}
