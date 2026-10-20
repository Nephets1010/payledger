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
package io.github.nephets.payledger.core.exception;

/** 向冻结账户过账时抛出。 */
public class AccountFrozenException extends RuntimeException {

    private final String tenantId;

    private final String accountId;

    public AccountFrozenException(String tenantId, String accountId) {
        super("账户已冻结，禁止过账: tenant=" + tenantId + ", account=" + accountId);
        this.tenantId = tenantId;
        this.accountId = accountId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getAccountId() {
        return accountId;
    }
}
