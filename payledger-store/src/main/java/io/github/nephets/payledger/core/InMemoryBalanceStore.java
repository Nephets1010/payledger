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

import io.github.nephets.payledger.core.exception.AccountNotFoundException;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 内存版 {@link BalanceStore}：以 {@code tenantId + accountId} 为键保存账户与其余额。
 *
 * <p>借贷按币种调整：借增、贷减。账户未开户时 {@link #getBalance} 与 {@link #applyEntry} 抛 {@link
 * AccountNotFoundException}。
 *
 * <p>为避免内存无界增长，账户数量受 {@code capacity}（默认 {@value #DEFAULT_CAPACITY}）约束：超出后按开户顺序淘汰最旧的账户。
 */
public final class InMemoryBalanceStore implements BalanceStore {

    static final long DEFAULT_CAPACITY = 1_000_000L;

    private final long capacity;

    private final Object evictionLock = new Object();

    private final Map<String, AccountBalance> store = new ConcurrentHashMap<>();

    // 全局开户顺序（旧 -> 新），用于容量淘汰。同时充当淘汰相关操作的锁。
    private final LinkedHashMap<String, Boolean> order = new LinkedHashMap<>();

    private final Map<String, AtomicLong> seqs = new ConcurrentHashMap<>();

    public InMemoryBalanceStore() {
        this(DEFAULT_CAPACITY);
    }

    public InMemoryBalanceStore(long capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity 必须为正");
        }
        this.capacity = capacity;
    }

    private static final class AccountBalance {

        final Account account;

        // 余额用 AtomicReference 保护：并发过账对同一账户的 read-modify-write 必须是原子的，
        // 否则会出现丢失更新（资损）。原实现用 volatile Money 做非原子 RMW，高并发下丢更新。
        final AtomicReference<Money> balance;

        AccountBalance(Account account, Money balance) {
            this.account = account;
            this.balance = new AtomicReference<>(balance);
        }
    }

    private static String key(String tenantId, String accountId) {
        return tenantId + "\u0000" + accountId;
    }

    @Override
    public void openAccount(Account account) {
        Objects.requireNonNull(account, "account 不能为 null");
        String k = key(account.getTenantId(), account.getAccountId());
        synchronized (evictionLock) {
            if (store.putIfAbsent(k, new AccountBalance(account, Money.zero(account.getCurrency())))
                    == null) {
                // 仅新开户的账户计入容量与淘汰顺序（重复开户保持幂等，不影响顺序）。
                order.put(k, Boolean.TRUE);
                evictIfNeeded();
            }
        }
    }

    @Override
    public Money getBalance(String tenantId, String accountId) {
        AccountBalance ab = store.get(key(tenantId, accountId));
        if (ab == null) {
            throw new AccountNotFoundException(tenantId, accountId);
        }
        return ab.balance.get();
    }

    @Override
    public void applyEntry(LedgerEntry entry) {
        AccountBalance ab = store.get(key(entry.getTenantId(), entry.getAccountId()));
        if (ab == null) {
            throw new AccountNotFoundException(entry.getTenantId(), entry.getAccountId());
        }
        // 原子地按分录方向调整余额：DEBIT 加、CREDIT 减。accumulateAndGet 保证同一账户的
        // 并发过账不会丢失更新。Money 是不可变值对象（add/subtract 返回新实例），CAS 安全。
        ab.balance.accumulateAndGet(
                entry.getAmount(),
                (cur, amt) ->
                        entry.getDirection() == Direction.DEBIT ? cur.add(amt) : cur.subtract(amt));
    }

    @Override
    public Optional<Account> findAccount(String tenantId, String accountId) {
        AccountBalance ab = store.get(key(tenantId, accountId));
        return ab == null ? Optional.empty() : Optional.of(ab.account);
    }

    /** 该租户下一个序列号（与 {@code InMemoryEntryStore} 同款的取号器）。 */
    public long nextSequenceNo(String tenantId) {
        return seqs.computeIfAbsent(tenantId, k -> new AtomicLong()).incrementAndGet();
    }

    /** 容量超限时淘汰最旧（最先开户）的账户。 */
    private void evictIfNeeded() {
        Iterator<String> it = order.keySet().iterator();
        while (order.size() > capacity && it.hasNext()) {
            String eldest = it.next();
            it.remove();
            store.remove(eldest);
        }
    }
}
