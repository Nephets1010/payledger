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

import java.util.Optional;

/** 余额存储 SPI：维护账户余额，借加贷减。 */
public interface BalanceStore {

    /** 查询账户余额；账户未开户时抛 {@code AccountNotFoundException}。 */
    Money getBalance(String tenantId, String accountId);

    /** 按分录方向调整账户余额（借加、贷减）。 */
    void applyEntry(LedgerEntry entry);

    /** 开户：初始化账户余额为零（重复开户幂等）。 */
    void openAccount(Account account);

    /** 查询账户；不存在时返回 {@link Optional#empty()}。 */
    Optional<Account> findAccount(String tenantId, String accountId);
}
