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
 * 清结算指令：从 {@code from} 向 {@code to} 净支付 {@code netAmount}，来源是 {@code sources} 里的若干交易。
 *
 * <p>净额为零的对手方关系不会生成指令。
 */
public final class SettlementInstruction {

    private final String from;

    private final String to;

    private final Money netAmount;

    private final List<String> sources;

    public SettlementInstruction(String from, String to, Money netAmount, List<String> sources) {
        this.from = Objects.requireNonNull(from, "from 不能为 null");
        this.to = Objects.requireNonNull(to, "to 不能为 null");
        this.netAmount = Objects.requireNonNull(netAmount, "netAmount 不能为 null");
        this.sources = List.copyOf(sources == null ? List.of() : sources);
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public Money getNetAmount() {
        return netAmount;
    }

    public List<String> getSources() {
        return sources;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SettlementInstruction that)) {
            return false;
        }
        return from.equals(that.from)
                && to.equals(that.to)
                && netAmount.equals(that.netAmount)
                && sources.equals(that.sources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, netAmount, sources);
    }

    @Override
    public String toString() {
        return "SettlementInstruction{from='"
                + from
                + "', to='"
                + to
                + "', netAmount="
                + netAmount
                + ", sources="
                + sources
                + '}';
    }
}
