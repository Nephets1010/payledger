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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 为什么测这些：内存存储必须有界，否则长期运行会 OOM。容量淘汰按开户顺序淘汰最旧账户，是「有界」的核心保证，必须验证被淘汰账户确实读不到了。 */
class InMemoryBalanceStoreEvictionContractTest {

    @Test
    void capacityOneKeepsOnlyLatest() {
        InMemoryBalanceStore store = new InMemoryBalanceStore(1);
        store.openAccount(new Account("a", "t1", AccountType.ASSET, "CNY"));
        store.openAccount(new Account("b", "t1", AccountType.ASSET, "CNY"));
        // 容量 1：开户 b 后 a 被淘汰。
        assertThatThrownBy(() -> store.getBalance("t1", "a"))
                .isInstanceOf(AccountNotFoundException.class);
        assertThat(store.findAccount("t1", "b")).isPresent();
    }

    @Test
    void capacityExceededEvictsOldest() {
        InMemoryBalanceStore store = new InMemoryBalanceStore(2);
        store.openAccount(new Account("a", "t1", AccountType.ASSET, "CNY"));
        store.openAccount(new Account("b", "t1", AccountType.ASSET, "CNY"));
        store.openAccount(new Account("c", "t1", AccountType.ASSET, "CNY"));
        // 容量 2：最旧账户 a 被淘汰，b、c 保留。
        assertThatThrownBy(() -> store.getBalance("t1", "a"))
                .isInstanceOf(AccountNotFoundException.class);
        assertThat(store.findAccount("t1", "b")).isPresent();
        assertThat(store.findAccount("t1", "c")).isPresent();
    }

    @Test
    void reopeningEvictedAccountRestoresIt() {
        InMemoryBalanceStore store = new InMemoryBalanceStore(1);
        store.openAccount(new Account("a", "t1", AccountType.ASSET, "CNY"));
        store.openAccount(new Account("b", "t1", AccountType.ASSET, "CNY"));
        // 重新开户被淘汰的 a，应再次可读且余额为零。
        store.openAccount(new Account("a", "t1", AccountType.ASSET, "CNY"));
        assertThat(store.getBalance("t1", "a")).isEqualTo(Money.zero("CNY"));
    }

    @Test
    void capacityMustBePositive() {
        assertThatThrownBy(() -> new InMemoryBalanceStore(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
