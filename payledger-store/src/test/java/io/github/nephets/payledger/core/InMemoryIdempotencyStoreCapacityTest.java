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

/** 容量上限回归测试：累计标记超过容量后，最旧的事件被淘汰。 */
class InMemoryIdempotencyStoreCapacityTest {

    private static final long CAPACITY = 100L;

    @Test
    void overflowEvictsOldest() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore(CAPACITY);
        for (int i = 0; i < 150; i++) {
            store.mark("t1", "e" + i);
        }
        // 超出容量后，最旧的一批事件被淘汰。
        assertThat(store.seen("t1", "e0")).isFalse();
        assertThat(store.seen("t1", "e49")).isFalse();
        // 最新的一批仍在。
        assertThat(store.seen("t1", "e100")).isTrue();
        assertThat(store.seen("t1", "e149")).isTrue();
    }

    @Test
    void defaultCapacityKeepsAllWithinBounds() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
        for (int i = 0; i < 10; i++) {
            store.mark("t1", "e" + i);
        }
        assertThat(store.seen("t1", "e0")).isTrue();
        assertThat(store.seen("t1", "e9")).isTrue();
    }
}
