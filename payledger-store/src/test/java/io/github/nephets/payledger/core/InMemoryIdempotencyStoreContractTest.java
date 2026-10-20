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

/** 为什么测这些：幂等去重是防止支付回调重复入账（资损）的最后一道闸，consume 的「首次 true / 重复 false」语义、租户隔离与标记幂等必须精确。 */
class InMemoryIdempotencyStoreContractTest {

    @Test
    void consumeReturnsTrueOnlyFirstTime() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
        assertThat(store.consume("t1", "evt1")).isTrue();
        assertThat(store.consume("t1", "evt1")).isFalse();
        assertThat(store.seen("t1", "evt1")).isTrue();
    }

    @Test
    void unseenEventNotSeen() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
        assertThat(store.seen("t1", "nope")).isFalse();
    }

    @Test
    void markAndSeen() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
        store.mark("t1", "evt");
        assertThat(store.seen("t1", "evt")).isTrue();
        // 重复 mark 仍然是 seen。
        store.mark("t1", "evt");
        assertThat(store.seen("t1", "evt")).isTrue();
    }

    @Test
    void isolatedByTenant() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
        store.mark("t1", "evt");
        assertThat(store.seen("t2", "evt")).isFalse();
        assertThat(store.consume("t2", "evt")).isTrue();
    }

    @Test
    void consumeDistinctEventsAllTrue() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
        for (int i = 0; i < 50; i++) {
            assertThat(store.consume("t1", "evt-" + i)).isTrue();
        }
        assertThat(store.seen("t1", "evt-49")).isTrue();
    }

    @Test
    void evictionRemovesOldestEvent() {
        // 容量设为 10，标记 10 个后再标记第 11 个会把最早的淘汰。
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore(10);
        for (int i = 0; i < 10; i++) {
            store.mark("t1", "e" + i);
        }
        assertThat(store.seen("t1", "e0")).isTrue();
        store.mark("t1", "e10");
        // 超出容量后最旧事件被淘汰。
        assertThat(store.seen("t1", "e0")).isFalse();
        assertThat(store.seen("t1", "e10")).isTrue();
    }
}
