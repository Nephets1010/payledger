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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    @Test
    void defaultStatusIsOpen() {
        Account a = new Account("acc1", "t1", AccountType.ASSET, "CNY");
        assertThat(a.getStatus()).isEqualTo(AccountStatus.OPEN);
        assertThat(a.isFrozen()).isFalse();
    }

    @Test
    void frozenFlag() {
        Account a = new Account("acc1", "t1", AccountType.ASSET, "CNY", AccountStatus.FROZEN);
        assertThat(a.isFrozen()).isTrue();
    }

    @Test
    void getters() {
        Account a = new Account("acc1", "t1", AccountType.INCOME, "CNY", AccountStatus.CLOSED);
        assertThat(a.getAccountId()).isEqualTo("acc1");
        assertThat(a.getTenantId()).isEqualTo("t1");
        assertThat(a.getType()).isEqualTo(AccountType.INCOME);
        assertThat(a.getCurrency()).isEqualTo("CNY");
        assertThat(a.getStatus()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    void nullRejected() {
        assertThatThrownBy(() -> new Account(null, "t1", AccountType.ASSET, "CNY"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Account("a", null, AccountType.ASSET, "CNY"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Account("a", "t", null, "CNY"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void equalsHashCodeToString() {
        Account a = new Account("acc1", "t1", AccountType.ASSET, "CNY");
        Account b = new Account("acc1", "t1", AccountType.ASSET, "CNY");
        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(new Account("acc2", "t1", AccountType.ASSET, "CNY"));
        assertThat(a).isNotEqualTo("x");
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains("acc1");
    }

    @Test
    void accountTypeLabels() {
        assertThat(AccountType.ASSET.getLabel()).isEqualTo("资产");
        assertThat(AccountType.LIABILITY.getLabel()).isEqualTo("负债");
        assertThat(AccountType.EQUITY.getLabel()).isEqualTo("权益");
        assertThat(AccountType.INCOME.getLabel()).isEqualTo("收入");
        assertThat(AccountType.EXPENSE.getLabel()).isEqualTo("费用");
    }

    @Test
    void directionLabels() {
        assertThat(Direction.DEBIT.getLabel()).isEqualTo("借");
        assertThat(Direction.CREDIT.getLabel()).isEqualTo("贷");
    }
}
