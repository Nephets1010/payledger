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

/** 支付渠道回调解析出的结构化支付事件（业务单号、金额、渠道、发生时间）。 */
public final class ExternalPaymentEvent {

    private final String reference;

    private final Money amount;

    private final String channel;

    private final Instant occurredAt;

    public ExternalPaymentEvent(
            String reference, Money amount, String channel, Instant occurredAt) {
        this.reference = Objects.requireNonNull(reference, "reference 不能为 null");
        this.amount = Objects.requireNonNull(amount, "amount 不能为 null");
        this.channel = channel;
        this.occurredAt = occurredAt;
    }

    public String getReference() {
        return reference;
    }

    public Money getAmount() {
        return amount;
    }

    public String getChannel() {
        return channel;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ExternalPaymentEvent that)) {
            return false;
        }
        return reference.equals(that.reference)
                && amount.equals(that.amount)
                && Objects.equals(channel, that.channel)
                && Objects.equals(occurredAt, that.occurredAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reference, amount, channel, occurredAt);
    }
}
