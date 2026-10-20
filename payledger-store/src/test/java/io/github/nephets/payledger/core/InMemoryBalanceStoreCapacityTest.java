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

import static org.assertj.core.api.Assertions.assertThat;

/** 容量上限回归测试：累计开户超过容量后，最旧的账户被淘汰。 */
class InMemoryBalanceStoreCapacityTest {

    private static final long CAPACITY = 100L;

    @Test
    void overflowEvictsOldest() {
        InMemoryBalanceStore store = new InMemoryBalanceStore(CAPACITY);
        for (int i = 0; i < 150; i++) {
            store.openAccount(new Account("acc" + i, "t1", AccountType.ASSET, "CNY"));
        }
        // 超出容量后，最旧的一批账户被淘汰。
        assertThat(store.findAccount("t1", "acc0")).isEmpty();
        assertThat(store.findAccount("t1", "acc49")).isEmpty();
        // 最新的一批仍在。
        assertThat(store.findAccount("t1", "acc100")).isPresent();
        assertThat(store.findAccount("t1", "acc149")).isPresent();
    }

    @Test
    void defaultCapacityKeepsAllWithinBounds() {
        InMemoryBalanceStore store = new InMemoryBalanceStore();
        for (int i = 0; i < 10; i++) {
            store.openAccount(new Account("acc" + i, "t1", AccountType.ASSET, "CNY"));
        }
        assertThat(store.findAccount("t1", "acc0")).isPresent();
        assertThat(store.findAccount("t1", "acc9")).isPresent();
    }
}
