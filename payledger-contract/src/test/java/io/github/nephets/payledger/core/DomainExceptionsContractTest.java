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

import io.github.nephets.payledger.core.exception.AccountFrozenException;
import io.github.nephets.payledger.core.exception.AccountNotFoundException;
import io.github.nephets.payledger.core.exception.ChannelNotFoundException;
import io.github.nephets.payledger.core.exception.CurrencyMismatchException;
import io.github.nephets.payledger.core.exception.EmptyTransactionException;
import io.github.nephets.payledger.core.exception.UnbalancedEntryException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 为什么测这些：这些领域异常是控制流的关键信号（过账拒绝、渠道缺失等），必须保证携带的字段与消息正确，便于上层精确处理与排障。 */
class DomainExceptionsContractTest {

    @Test
    void accountFrozenCarriesTenantAndAccount() {
        AccountFrozenException e = new AccountFrozenException("t1", "acc");
        assertThat(e.getTenantId()).isEqualTo("t1");
        assertThat(e.getAccountId()).isEqualTo("acc");
        assertThat(e).isInstanceOf(RuntimeException.class);
    }

    @Test
    void accountNotFoundCarriesTenantAndAccount() {
        AccountNotFoundException e = new AccountNotFoundException("t1", "acc");
        assertThat(e.getTenantId()).isEqualTo("t1");
        assertThat(e.getAccountId()).isEqualTo("acc");
    }

    @Test
    void channelNotFoundCarriesId() {
        ChannelNotFoundException e = new ChannelNotFoundException("wechat");
        assertThat(e.getChannelId()).isEqualTo("wechat");
    }

    @Test
    void currencyMismatchCarriesExpectedAndActual() {
        CurrencyMismatchException e = new CurrencyMismatchException("CNY", "USD");
        assertThat(e.getExpected()).isEqualTo("CNY");
        assertThat(e.getActual()).isEqualTo("USD");
    }

    @Test
    void emptyTransactionCarriesIdOrNull() {
        assertThat(new EmptyTransactionException("tx").getTransactionId()).isEqualTo("tx");
        assertThat(new EmptyTransactionException().getTransactionId()).isNull();
    }

    @Test
    void unbalancedCarriesTotals() {
        UnbalancedEntryException e =
                new UnbalancedEntryException(Money.of("10", "CNY"), Money.of("9", "CNY"));
        assertThat(e.getDebitTotal()).isEqualTo(Money.of("10", "CNY"));
        assertThat(e.getCreditTotal()).isEqualTo(Money.of("9", "CNY"));
    }
}
