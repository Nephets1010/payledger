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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 内存版 {@link EntryStore}：按租户分区的线程安全实现。
 *
 * <p>保存分录的同时维护每个租户的序列号计数器（{@link #nextSequenceNo}），供调用方在构造不可变 {@link LedgerEntry} 前取号。
 *
 * <p>为避免内存无界增长，全量分录受 {@code capacity}（默认 {@value #DEFAULT_CAPACITY}）约束：超出后按插入顺序淘汰最旧的分录。
 */
public final class InMemoryEntryStore implements EntryStore {

    static final long DEFAULT_CAPACITY = 1_000_000L;

    private final long capacity;

    private final Object evictionLock = new Object();

    private final Map<String, List<LedgerEntry>> byTenant = new ConcurrentHashMap<>();

    private final Map<String, AtomicLong> seqByTenant = new ConcurrentHashMap<>();

    // 全局插入顺序（旧 -> 新），用于容量淘汰。同时充当淘汰相关操作的锁。
    private final LinkedHashMap<LedgerEntry, Boolean> order = new LinkedHashMap<>();

    public InMemoryEntryStore() {
        this(DEFAULT_CAPACITY);
    }

    public InMemoryEntryStore(long capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity 必须为正");
        }
        this.capacity = capacity;
    }

    @Override
    public void save(List<LedgerEntry> entries) {
        if (entries == null) {
            throw new IllegalArgumentException("entries 不能为 null");
        }
        synchronized (evictionLock) {
            for (LedgerEntry entry : entries) {
                if (order.putIfAbsent(entry, Boolean.TRUE) == null) {
                    // 仅首次出现的分录计入容量、序列号与按租户索引。
                    byTenant.computeIfAbsent(entry.getTenantId(), k -> new CopyOnWriteArrayList<>())
                            .add(entry);
                    AtomicLong seq =
                            seqByTenant.computeIfAbsent(entry.getTenantId(), k -> new AtomicLong());
                    if (entry.getSequenceNo() > seq.get()) {
                        seq.set(entry.getSequenceNo());
                    }
                }
            }
            evictIfNeeded();
        }
    }

    @Override
    public List<LedgerEntry> findByAccount(String tenantId, String accountId) {
        return findAll(tenantId).stream()
                .filter(e -> e.getAccountId().equals(accountId))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<LedgerEntry> findByTransaction(String tenantId, String transactionId) {
        return findAll(tenantId).stream()
                .filter(e -> e.getTransactionId().equals(transactionId))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<LedgerEntry> findAll(String tenantId) {
        List<LedgerEntry> list = byTenant.get(tenantId);
        return list == null ? List.of() : List.copyOf(list);
    }

    /** 该租户下一个序列号（自增）。 */
    public long nextSequenceNo(String tenantId) {
        return seqByTenant.computeIfAbsent(tenantId, k -> new AtomicLong()).incrementAndGet();
    }

    /** 容量超限时淘汰最旧（最先保存）的分录。 */
    private void evictIfNeeded() {
        Iterator<LedgerEntry> it = order.keySet().iterator();
        while (order.size() > capacity && it.hasNext()) {
            LedgerEntry eldest = it.next();
            it.remove();
            List<LedgerEntry> list = byTenant.get(eldest.getTenantId());
            if (list != null) {
                list.remove(eldest);
                if (list.isEmpty()) {
                    byTenant.remove(eldest.getTenantId(), list);
                }
            }
        }
    }
}
