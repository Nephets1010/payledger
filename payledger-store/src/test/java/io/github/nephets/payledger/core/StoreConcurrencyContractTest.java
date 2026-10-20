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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** 为什么测这些：支付系统是并发重灾区，余额更新丢更新 = 资损，幂等去重并发失效 = 重复入账。这里用真实多线程验证「无丢失更新」与「恰好一次」。 */
class StoreConcurrencyContractTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    private LedgerEntry debit(String id, String tenant, String account, String amount) {
        return new LedgerEntry(
                id,
                tenant,
                account,
                Direction.DEBIT,
                Money.of(amount, "CNY"),
                "tx",
                "",
                "m",
                1L,
                NOW);
    }

    @Test
    void concurrentApplyEntryHasNoLostUpdates() throws InterruptedException {
        InMemoryBalanceStore store = new InMemoryBalanceStore();
        store.openAccount(new Account("acc", "t1", AccountType.ASSET, "CNY"));
        int threads = 64;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool.submit(
                    () -> {
                        try {
                            start.await();
                            // 每个线程对同一账户借记 1.00；AtomicReference 的 CAS 必须保证最终为 64.00。
                            store.applyEntry(debit("e" + idx, "t1", "acc", "1.00"));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            done.countDown();
                        }
                    });
        }
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();
        assertThat(store.getBalance("t1", "acc")).isEqualTo(Money.of("64.00", "CNY"));
    }

    @Test
    void concurrentIdempotencyConsumeExactlyOnce() throws InterruptedException {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
        int threads = 64;
        AtomicInteger trueCount = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(
                    () -> {
                        try {
                            start.await();
                            if (store.consume("t1", "same-event")) {
                                trueCount.incrementAndGet();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            done.countDown();
                        }
                    });
        }
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();
        // 并发下必须恰好一次「首次处理」。
        assertThat(trueCount.get()).isEqualTo(1);
    }

    @Test
    void concurrentOpenDistinctAccountsAllVisible() throws InterruptedException {
        InMemoryBalanceStore store = new InMemoryBalanceStore();
        int threads = 128;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool.submit(
                    () -> {
                        try {
                            start.await();
                            store.openAccount(
                                    new Account("a" + idx, "t1", AccountType.ASSET, "CNY"));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            done.countDown();
                        }
                    });
        }
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();
        for (int i = 0; i < threads; i++) {
            assertThat(store.findAccount("t1", "a" + i)).isPresent();
        }
    }

    @Test
    void concurrentSaveDistinctEntriesAllPersisted() throws InterruptedException {
        InMemoryEntryStore store = new InMemoryEntryStore();
        int threads = 128;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool.submit(
                    () -> {
                        try {
                            start.await();
                            store.save(List.of(debit("e" + idx, "t1", "acc", "1.00")));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            done.countDown();
                        }
                    });
        }
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();
        assertThat(store.findAll("t1")).hasSize(threads);
    }
}
