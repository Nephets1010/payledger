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

/**
 * 幂等去重 SPI：按租户记录已处理过的回调事件 id，避免重复入账。
 *
 * <p>{@link #consume} 是常用组合操作：首次出现则标记并返回 true，重复出现返回 false。
 */
public interface IdempotencyStore {

    /** 该事件是否已处理过。 */
    boolean seen(String tenantId, String eventId);

    /** 标记该事件为已处理。 */
    void mark(String tenantId, String eventId);

    /** 首次处理返回 true 并标记；重复返回 false。 */
    default boolean consume(String tenantId, String eventId) {
        if (seen(tenantId, eventId)) {
            return false;
        }
        mark(tenantId, eventId);
        return true;
    }
}
