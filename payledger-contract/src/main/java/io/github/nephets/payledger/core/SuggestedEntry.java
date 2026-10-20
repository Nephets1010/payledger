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

import java.util.Objects;

/** 一条记账建议：建议记到哪个账户、借还是贷、金额多少、附言是什么。 */
public final class SuggestedEntry {

    private final String accountId;

    private final Direction direction;

    private final Money amount;

    private final String memo;

    public SuggestedEntry(String accountId, Direction direction, Money amount, String memo) {
        this.accountId = Objects.requireNonNull(accountId, "accountId 不能为 null");
        this.direction = Objects.requireNonNull(direction, "direction 不能为 null");
        this.amount = Objects.requireNonNull(amount, "amount 不能为 null");
        this.memo = memo;
    }

    public String getAccountId() {
        return accountId;
    }

    public Direction getDirection() {
        return direction;
    }

    public Money getAmount() {
        return amount;
    }

    public String getMemo() {
        return memo;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SuggestedEntry that)) {
            return false;
        }
        return accountId.equals(that.accountId)
                && direction == that.direction
                && amount.equals(that.amount)
                && Objects.equals(memo, that.memo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, direction, amount, memo);
    }
}
