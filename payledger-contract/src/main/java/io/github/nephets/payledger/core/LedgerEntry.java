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
 * 分录：一笔交易里对单个账户的借/贷记录。
 *
 * <p>分录是不可变的。{@code reference} 用于和对账的外部流水按「单号 + 金额」配对，可留空。 {@code sequenceNo} 与 {@code entryId}
 * 在构造时给定（参考实现 {@code InMemoryEntryStore} 提供取号器）。
 */
public final class LedgerEntry {

    private final String entryId;

    private final String tenantId;

    private final String accountId;

    private final Direction direction;

    private final Money amount;

    private final String transactionId;

    private final String reference;

    private final String memo;

    private final long sequenceNo;

    private final Instant createdAt;

    public LedgerEntry(
            String entryId,
            String tenantId,
            String accountId,
            Direction direction,
            Money amount,
            String transactionId,
            String memo,
            long sequenceNo,
            Instant createdAt) {
        this(
                entryId,
                tenantId,
                accountId,
                direction,
                amount,
                transactionId,
                "",
                memo,
                sequenceNo,
                createdAt);
    }

    public LedgerEntry(
            String entryId,
            String tenantId,
            String accountId,
            Direction direction,
            Money amount,
            String transactionId,
            String reference,
            String memo,
            long sequenceNo,
            Instant createdAt) {
        this.entryId = Objects.requireNonNull(entryId, "entryId 不能为 null");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId 不能为 null");
        this.accountId = Objects.requireNonNull(accountId, "accountId 不能为 null");
        this.direction = Objects.requireNonNull(direction, "direction 不能为 null");
        this.amount = Objects.requireNonNull(amount, "amount 不能为 null");
        this.transactionId = Objects.requireNonNull(transactionId, "transactionId 不能为 null");
        this.reference = reference == null ? "" : reference;
        this.memo = memo;
        this.sequenceNo = sequenceNo;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt 不能为 null");
    }

    public String getEntryId() {
        return entryId;
    }

    public String getTenantId() {
        return tenantId;
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

    public String getTransactionId() {
        return transactionId;
    }

    public String getReference() {
        return reference;
    }

    public String getMemo() {
        return memo;
    }

    public long getSequenceNo() {
        return sequenceNo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LedgerEntry that)) {
            return false;
        }
        return sequenceNo == that.sequenceNo
                && entryId.equals(that.entryId)
                && tenantId.equals(that.tenantId)
                && accountId.equals(that.accountId)
                && direction == that.direction
                && amount.equals(that.amount)
                && transactionId.equals(that.transactionId)
                && reference.equals(that.reference)
                && Objects.equals(memo, that.memo)
                && createdAt.equals(that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                entryId,
                tenantId,
                accountId,
                direction,
                amount,
                transactionId,
                reference,
                memo,
                sequenceNo,
                createdAt);
    }

    @Override
    public String toString() {
        return "LedgerEntry{entryId='"
                + entryId
                + "', tenantId='"
                + tenantId
                + "', accountId='"
                + accountId
                + "', direction="
                + direction
                + ", amount="
                + amount
                + ", transactionId='"
                + transactionId
                + "', reference='"
                + reference
                + "', memo='"
                + memo
                + "', sequenceNo="
                + sequenceNo
                + ", createdAt="
                + createdAt
                + '}';
    }
}
