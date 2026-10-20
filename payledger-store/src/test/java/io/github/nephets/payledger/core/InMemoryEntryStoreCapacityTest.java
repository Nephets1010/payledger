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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** 容量上限回归测试：累计保存超过容量后，最旧的分录被淘汰。 */
class InMemoryEntryStoreCapacityTest {

    private static final long CAPACITY = 100L;

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    private LedgerEntry e(String id, long seq) {
        return new LedgerEntry(
                id, "t1", "a", Direction.DEBIT, Money.of("1.00", "CNY"), "tx", "", "m", seq, NOW);
    }

    private boolean contains(List<LedgerEntry> entries, String entryId) {
        return entries.stream().anyMatch(x -> x.getEntryId().equals(entryId));
    }

    @Test
    void overflowEvictsOldest() {
        InMemoryEntryStore store = new InMemoryEntryStore(CAPACITY);
        for (int i = 0; i < 150; i++) {
            store.save(List.of(e("e" + i, i)));
        }
        // 容量约束：最多保留 CAPACITY 条。
        assertThat(store.findAll("t1")).hasSize((int) CAPACITY);
        List<LedgerEntry> byAccount = store.findByAccount("t1", "a");
        // 最旧的被淘汰。
        assertThat(contains(byAccount, "e0")).isFalse();
        assertThat(contains(byAccount, "e49")).isFalse();
        // 最新的保留。
        assertThat(contains(byAccount, "e149")).isTrue();
        assertThat(contains(byAccount, "e100")).isTrue();
    }

    @Test
    void defaultCapacityKeepsAllWithinBounds() {
        InMemoryEntryStore store = new InMemoryEntryStore();
        for (int i = 0; i < 10; i++) {
            store.save(List.of(e("e" + i, i)));
        }
        assertThat(
                        store.findAll("t1").stream()
                                .map(LedgerEntry::getEntryId)
                                .collect(Collectors.toSet()))
                .contains("e0", "e9");
    }
}
