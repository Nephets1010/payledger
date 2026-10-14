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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 交易：复式记账的聚合根。一笔交易包含至少一条借/贷分录，且借贷金额必须相等、币种一致。
 *
 * <p>{@code from}/{@code to} 是清结算用的对手方账户标识；{@code status} 由 {@link PostingService} 在过账成功后置为 {@link
 * TransactionStatus#POSTED}。
 */
public final class Transaction {

    private final String transactionId;

    private final String tenantId;

    private final Instant occurredAt;

    private final String memo;

    private final List<LedgerEntry> entries = new ArrayList<>();

    private TransactionStatus status = TransactionStatus.PENDING;

    private String from;

    private String to;

    public Transaction(String transactionId, String tenantId, Instant occurredAt, String memo) {
        this.transactionId = Objects.requireNonNull(transactionId, "transactionId 不能为 null");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId 不能为 null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt 不能为 null");
        this.memo = memo;
    }

    /** 追加一条分录；分录的租户必须与交易一致。 */
    public void addEntry(LedgerEntry entry) {
        Objects.requireNonNull(entry, "entry 不能为 null");
        if (!tenantId.equals(entry.getTenantId())) {
            throw new IllegalArgumentException(
                    "分录租户与交易租户不一致: " + entry.getTenantId() + " vs " + tenantId);
        }
        entries.add(entry);
    }

    /** 借贷是否平衡（金额相等且币种一致）。校验失败时返回 false。 */
    public boolean isBalanced() {
        return TransactionValidator.isBalanced(this);
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getMemo() {
        return memo;
    }

    /** 返回不可变的内部分录列表。 */
    public List<LedgerEntry> getEntries() {
        return List.copyOf(entries);
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = Objects.requireNonNull(status, "status 不能为 null");
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    @Override
    public String toString() {
        return "Transaction{transactionId='"
                + transactionId
                + "', tenantId='"
                + tenantId
                + "', status="
                + status
                + ", from='"
                + from
                + "', to='"
                + to
                + "', entries="
                + entries.size()
                + '}';
    }
}
