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

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 为什么测这些：分录存储是审计与对账的数据底座，按账户/交易/租户精确检索、同一条分录的幂等去重、取号器的隔离性，都是上层功能正确性的前提。 */
class InMemoryEntryStoreContractTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    private LedgerEntry entry(String id, String tenant, String account, String tx, String amount) {
        return new LedgerEntry(
                id,
                tenant,
                account,
                Direction.DEBIT,
                Money.of(amount, "CNY"),
                tx,
                "",
                "m",
                1L,
                NOW);
    }

    @Test
    void saveNullRejected() {
        InMemoryEntryStore store = new InMemoryEntryStore();
        assertThatThrownBy(() -> store.save(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findByAccountReturnsOnlyThatAccount() {
        InMemoryEntryStore store = new InMemoryEntryStore();
        store.save(
                List.of(
                        entry("e1", "t1", "a", "tx1", "10.00"),
                        entry("e2", "t1", "b", "tx1", "10.00")));
        assertThat(store.findByAccount("t1", "a")).hasSize(1);
        assertThat(store.findByAccount("t1", "b")).hasSize(1);
    }

    @Test
    void findByTransactionReturnsOnlyThatTransaction() {
        InMemoryEntryStore store = new InMemoryEntryStore();
        store.save(
                List.of(
                        entry("e1", "t1", "a", "tx1", "10.00"),
                        entry("e2", "t1", "a", "tx2", "20.00")));
        assertThat(store.findByTransaction("t1", "tx1")).hasSize(1);
        assertThat(store.findByTransaction("t1", "tx2")).hasSize(1);
    }

    @Test
    void findAllEmptyForUnknownTenant() {
        InMemoryEntryStore store = new InMemoryEntryStore();
        assertThat(store.findAll("unknown")).isEmpty();
    }

    @Test
    void saveIsIdempotentForSameEntry() {
        InMemoryEntryStore store = new InMemoryEntryStore();
        LedgerEntry e = entry("e1", "t1", "a", "tx1", "10.00");
        store.save(List.of(e));
        store.save(List.of(e));
        // 同一条分录（equals）重复保存不应产生重复记录。
        assertThat(store.findByAccount("t1", "a")).hasSize(1);
        assertThat(store.findAll("t1")).hasSize(1);
    }

    @Test
    void partitionedByTenant() {
        InMemoryEntryStore store = new InMemoryEntryStore();
        store.save(List.of(entry("e1", "t1", "a", "tx1", "10.00")));
        store.save(List.of(entry("e2", "t2", "a", "tx1", "10.00")));
        assertThat(store.findAll("t1")).hasSize(1);
        assertThat(store.findAll("t2")).hasSize(1);
    }

    @Test
    void nextSequenceNoIncrementsPerTenant() {
        InMemoryEntryStore store = new InMemoryEntryStore();
        assertThat(store.nextSequenceNo("t1")).isEqualTo(1);
        assertThat(store.nextSequenceNo("t1")).isEqualTo(2);
        // 不同租户各自独立计数。
        assertThat(store.nextSequenceNo("t2")).isEqualTo(1);
    }

    @Test
    void savePreservesAllEntries() {
        InMemoryEntryStore store = new InMemoryEntryStore();
        store.save(
                List.of(
                        entry("e1", "t1", "a", "tx1", "10.00"),
                        entry("e2", "t1", "b", "tx1", "10.00"),
                        entry("e3", "t1", "c", "tx1", "10.00")));
        assertThat(store.findAll("t1")).hasSize(3);
    }
}
