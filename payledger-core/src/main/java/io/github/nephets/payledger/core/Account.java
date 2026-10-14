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

/**
 * 账户：复式记账里的科目实体。
 *
 * <p>账户绑定租户、类型、币种与状态。状态为 {@link AccountStatus#FROZEN} 时过账会被拒绝。
 */
public final class Account {

    private final String accountId;

    private final String tenantId;

    private final AccountType type;

    private final String currency;

    private final AccountStatus status;

    public Account(String accountId, String tenantId, AccountType type, String currency) {
        this(accountId, tenantId, type, currency, AccountStatus.OPEN);
    }

    public Account(
            String accountId,
            String tenantId,
            AccountType type,
            String currency,
            AccountStatus status) {
        this.accountId = Objects.requireNonNull(accountId, "accountId 不能为 null");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId 不能为 null");
        this.type = Objects.requireNonNull(type, "type 不能为 null");
        this.currency = Objects.requireNonNull(currency, "currency 不能为 null");
        this.status = Objects.requireNonNull(status, "status 不能为 null");
    }

    public String getAccountId() {
        return accountId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public AccountType getType() {
        return type;
    }

    public String getCurrency() {
        return currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    /** 账户是否处于冻结状态。 */
    public boolean isFrozen() {
        return status == AccountStatus.FROZEN;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Account that)) {
            return false;
        }
        return accountId.equals(that.accountId)
                && tenantId.equals(that.tenantId)
                && type == that.type
                && currency.equals(that.currency)
                && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, tenantId, type, currency, status);
    }

    @Override
    public String toString() {
        return "Account{accountId='"
                + accountId
                + "', tenantId='"
                + tenantId
                + "', type="
                + type
                + ", currency='"
                + currency
                + "', status="
                + status
                + '}';
    }
}
