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
package io.github.nephets.payledger.starter;

import io.github.nephets.payledger.core.Direction;
import io.github.nephets.payledger.core.Money;
import io.github.nephets.payledger.core.SuggestedEntry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeywordLedgerAssistantTest {

    private final KeywordLedgerAssistant assistant = new KeywordLedgerAssistant();

    @Test
    void revenueKeywordSuggestsCreditRevenue() {
        SuggestedEntry entry = assistant.suggest("销售一批货物 100").get(0);
        assertThat(entry.getAccountId()).isEqualTo("acc_revenue");
        assertThat(entry.getDirection()).isEqualTo(Direction.CREDIT);
        assertThat(entry.getAmount()).isEqualTo(Money.of("100.00", "CNY"));
    }

    @Test
    void expenseKeywordSuggestsDebitExpense() {
        SuggestedEntry entry = assistant.suggest("采购办公用品 200").get(0);
        assertThat(entry.getAccountId()).isEqualTo("acc_expense");
        assertThat(entry.getDirection()).isEqualTo(Direction.DEBIT);
    }

    @Test
    void cashKeywordSuggestsCash() {
        assertThat(
                        assistant.suggest("现金充值 50").stream()
                                .anyMatch(e -> e.getAccountId().equals("acc_cash")))
                .isTrue();
    }

    @Test
    void unknownDescriptionGivesPlaceholder() {
        assertThat(assistant.suggest("随便记一笔").get(0).getAccountId()).isEqualTo("acc_cash");
    }

    @Test
    void noNumberDefaultsToZero() {
        assertThat(assistant.suggest("销售收入").get(0).getAmount()).isEqualTo(Money.zero("CNY"));
    }

    @Test
    void nullDescriptionRejected() {
        assertThatThrownBy(() -> assistant.suggest(null)).isInstanceOf(NullPointerException.class);
    }
}
