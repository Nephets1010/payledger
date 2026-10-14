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

class InMemoryEntryStoreTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    private LedgerEntry e(String id, String tenant, String account, String tx, long seq) {
        return new LedgerEntry(
                id,
                tenant,
                account,
                Direction.DEBIT,
                Money.of("1.00", "CNY"),
                tx,
                "",
                "m",
                seq,
                NOW);
    }

    @Test
    void saveAndQueryByAccount() {
        InMemoryEntryStore store = new InMemoryEntryStore();
        store.save(List.of(e("e1", "t1", "a", "tx1", 1), e("e2", "t1", "b", "tx1", 2)));
        assertThat(store.findByAccount("t1", "a")).hasSize(1);
        assertThat(store.findByAccount("t1", "b")).hasSize(1);
        assertThat(store.findByAccount("t1", "missing")).isEmpty();
    }

    @Test
    void saveAndQueryByTransaction() {
        InMemoryEntryStore store = new InMemoryEntryStore();
        store.save(List.of(e("e1", "t1", "a", "tx1", 1), e("e2", "t1", "a", "tx2", 2)));
        assertThat(store.findByTransaction("t1", "tx1")).hasSize(1);
        assertThat(store.findByTransaction("t1", "tx2")).hasSize(1);
    }

    @Test
    void findAllPartitionedByTenant() {
        InMemoryEntryStore store = new InMemoryEntryStore();
        store.save(List.of(e("e1", "t1", "a", "tx1", 1)));
        store.save(List.of(e("e2", "t2", "a", "tx1", 1)));
        assertThat(store.findAll("t1")).hasSize(1);
        assertThat(store.findAll("t2")).hasSize(1);
        assertThat(store.findAll("t3")).isEmpty();
    }

    @Test
    void nextSequenceNoIncrements() {
        InMemoryEntryStore store = new InMemoryEntryStore();
        assertThat(store.nextSequenceNo("t1")).isEqualTo(1);
        assertThat(store.nextSequenceNo("t1")).isEqualTo(2);
        assertThat(store.nextSequenceNo("t2")).isEqualTo(1);
    }

    @Test
    void savingLargerSequenceBumpsCounter() {
        InMemoryEntryStore store = new InMemoryEntryStore();
        store.save(List.of(e("e1", "t1", "a", "tx1", 10)));
        assertThat(store.nextSequenceNo("t1")).isEqualTo(11);
    }

    @Test
    void nullEntriesRejected() {
        assertThatThrownBy(() -> new InMemoryEntryStore().save(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
