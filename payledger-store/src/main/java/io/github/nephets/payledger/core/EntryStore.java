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

/** 分录存储 SPI：负责持久化与按账户/交易/租户查询分录。 */
public interface EntryStore {

    /** 保存一批分录（按租户隔离）。 */
    void save(List<LedgerEntry> entries);

    /** 查询某账户下的全部分录。 */
    List<LedgerEntry> findByAccount(String tenantId, String accountId);

    /** 查询某笔交易下的全部分录。 */
    List<LedgerEntry> findByTransaction(String tenantId, String transactionId);

    /** 查询某租户下的全部分录。 */
    List<LedgerEntry> findAll(String tenantId);
}
