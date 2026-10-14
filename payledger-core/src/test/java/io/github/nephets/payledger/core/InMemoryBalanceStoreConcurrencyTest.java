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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 并发过账回归测试：账户余额用 AtomicReference 保护后，同一账户的高并发借贷不应丢失任何更新。 修复前（volatile Money 做非原子
 * read-modify-write）此测试会断言失败。
 */
class InMemoryBalanceStoreConcurrencyTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");
    private static final int THREADS = 16;
    private static final int PER_THREAD = 1000;

    @Test
    void concurrentPostingsDoNotLoseUpdates() throws Exception {
        InMemoryBalanceStore store = new InMemoryBalanceStore();
        store.openAccount(new Account("a", "t1", AccountType.ASSET, "CNY"));

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            tasks.add(
                    () -> {
                        try {
                            start.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        for (int j = 0; j < PER_THREAD; j++) {
                            store.applyEntry(debit("t1", "a", "1.00"));
                        }
                    });
        }
        for (Runnable t : tasks) {
            pool.submit(t);
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        long expected = (long) THREADS * PER_THREAD;
        assertThat(store.getBalance("t1", "a"))
                .isEqualTo(Money.of(String.valueOf(expected) + ".00", "CNY"));
    }

    private LedgerEntry debit(String tenant, String account, String amount) {
        return new LedgerEntry(
                "e",
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
}
