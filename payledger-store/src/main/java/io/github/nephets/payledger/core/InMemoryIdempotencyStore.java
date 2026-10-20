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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** 内存版 {@link IdempotencyStore}：按租户维护已处理的事件 id 集合。 */
public final class InMemoryIdempotencyStore implements IdempotencyStore {

    static final long DEFAULT_CAPACITY = 1_000_000L;

    private final long capacity;

    private final Object evictionLock = new Object();

    private final Map<String, Set<String>> byTenant = new ConcurrentHashMap<>();

    // 全局插入顺序（旧 -> 新），用于容量淘汰。同时充当淘汰相关操作的锁。
    private final LinkedHashMap<String, Boolean> order = new LinkedHashMap<>();

    public InMemoryIdempotencyStore() {
        this(DEFAULT_CAPACITY);
    }

    public InMemoryIdempotencyStore(long capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity 必须为正");
        }
        this.capacity = capacity;
    }

    @Override
    public boolean seen(String tenantId, String eventId) {
        Set<String> ids = byTenant.get(tenantId);
        return ids != null && ids.contains(eventId);
    }

    @Override
    public void mark(String tenantId, String eventId) {
        String k = key(tenantId, eventId);
        synchronized (evictionLock) {
            if (byTenant.computeIfAbsent(tenantId, t -> ConcurrentHashMap.newKeySet())
                    .add(eventId)) {
                // 仅首次出现的事件计入容量与淘汰顺序。
                order.put(k, Boolean.TRUE);
                evictIfNeeded();
            }
        }
    }

    /**
     * 覆盖默认实现，保证并发下「恰好一次」：默认实现的 {@code seen} 与 {@code mark} 非原子，并发时多个线程可能都看到未处理并都返回
     * true（重复入账）。这里在淘汰锁内完成「判断+标记」，仅真正首次写入的线程返回 true。
     */
    @Override
    public boolean consume(String tenantId, String eventId) {
        Objects.requireNonNull(tenantId, "tenantId 不能为 null");
        Objects.requireNonNull(eventId, "eventId 不能为 null");
        String k = key(tenantId, eventId);
        synchronized (evictionLock) {
            boolean added =
                    byTenant.computeIfAbsent(tenantId, t -> ConcurrentHashMap.newKeySet())
                            .add(eventId);
            if (added) {
                order.put(k, Boolean.TRUE);
                evictIfNeeded();
            }
            return added;
        }
    }

    /** 容量超限时淘汰最旧（最先标记）的事件。 */
    private void evictIfNeeded() {
        Iterator<String> it = order.keySet().iterator();
        while (order.size() > capacity && it.hasNext()) {
            String eldest = it.next();
            it.remove();
            String[] parts = split(eldest);
            Set<String> ids = byTenant.get(parts[0]);
            if (ids != null) {
                ids.remove(parts[1]);
                if (ids.isEmpty()) {
                    byTenant.remove(parts[0], ids);
                }
            }
        }
    }

    private static String key(String tenantId, String eventId) {
        return tenantId + "\u0000" + eventId;
    }

    private static String[] split(String k) {
        int idx = k.indexOf('\u0000');
        return new String[] {k.substring(0, idx), k.substring(idx + 1)};
    }
}
