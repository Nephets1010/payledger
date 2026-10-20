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

/** 为什么测这些：账户是复式记账的科目实体，状态（冻结）直接决定是否允许过账，构造时的非空约束与相等约定必须可靠。 */
class AccountContractTest {

    @Test
    void defaultStatusIsOpen() {
        Account a = new Account("a", "t1", AccountType.ASSET, "CNY");
        assertThat(a.getStatus()).isEqualTo(AccountStatus.OPEN);
        assertThat(a.isFrozen()).isFalse();
    }

    @Test
    void frozenStatusReported() {
        Account a = new Account("a", "t1", AccountType.ASSET, "CNY", AccountStatus.FROZEN);
        assertThat(a.isFrozen()).isTrue();
    }

    @Test
    void accessorsExposeAllFields() {
        Account a = new Account("a", "t1", AccountType.INCOME, "USD", AccountStatus.OPEN);
        assertThat(a.getAccountId()).isEqualTo("a");
        assertThat(a.getTenantId()).isEqualTo("t1");
        assertThat(a.getType()).isEqualTo(AccountType.INCOME);
        assertThat(a.getCurrency()).isEqualTo("USD");
        assertThat(a.getStatus()).isEqualTo(AccountStatus.OPEN);
    }

    @Test
    void equalsAndHashCodeByAllFields() {
        Account a = new Account("a", "t1", AccountType.ASSET, "CNY");
        Account b = new Account("a", "t1", AccountType.ASSET, "CNY");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equalsDiffersByTenant() {
        Account a = new Account("a", "t1", AccountType.ASSET, "CNY");
        Account b = new Account("a", "t2", AccountType.ASSET, "CNY");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equalsDiffersByStatus() {
        Account a = new Account("a", "t1", AccountType.ASSET, "CNY", AccountStatus.OPEN);
        Account b = new Account("a", "t1", AccountType.ASSET, "CNY", AccountStatus.FROZEN);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void notEqualToDifferentType() {
        assertThat(new Account("a", "t1", AccountType.ASSET, "CNY")).isNotEqualTo("x");
    }

    @Test
    void nullGuardsOnConstructor() {
        assertThatThrownBy(() -> new Account(null, "t1", AccountType.ASSET, "CNY"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Account("a", null, AccountType.ASSET, "CNY"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Account("a", "t1", null, "CNY"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Account("a", "t1", AccountType.ASSET, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void toStringContainsIdentity() {
        assertThat(new Account("acc_1", "tenant_1", AccountType.ASSET, "CNY").toString())
                .contains("acc_1")
                .contains("tenant_1");
    }
}
