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

class InMemoryIdempotencyStoreTest {

    @Test
    void seenStartsFalse() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
        assertThat(store.seen("t1", "e1")).isFalse();
    }

    @Test
    void markThenSeenTrue() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
        store.mark("t1", "e1");
        assertThat(store.seen("t1", "e1")).isTrue();
    }

    @Test
    void consumeReturnsTrueFirstTimeFalseAfter() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
        assertThat(store.consume("t1", "e1")).isTrue();
        assertThat(store.consume("t1", "e1")).isFalse();
        assertThat(store.seen("t1", "e1")).isTrue();
    }

    @Test
    void partitionedByTenant() {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
        store.mark("t1", "e1");
        assertThat(store.seen("t2", "e1")).isFalse();
    }
}
