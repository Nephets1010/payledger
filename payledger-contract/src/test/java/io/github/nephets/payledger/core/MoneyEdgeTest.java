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

import io.github.nephets.payledger.core.exception.CurrencyMismatchException;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 为什么测这些：Money 是整条记账链路的基石，精度（固定 2 位四舍五入）、币种隔离、不可变语义与相等约定都必须稳定，否则上层余额/对账会算出错误的钱。 */
class MoneyEdgeTest {

    @Test
    void ofWithStringRoundsToTwoScale() {
        // 字符串构造也要走同样的四舍五入，避免 "100.005" 直接截断成 100.00。
        assertThat(Money.of("100.005", "CNY").getAmount()).isEqualByComparingTo("100.01");
        assertThat(Money.of("100.004", "CNY").getAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void addIsAssociativeAndImmutable() {
        Money a = Money.of("1.00", "CNY");
        Money b = Money.of("2.00", "CNY");
        Money c = Money.of("3.00", "CNY");
        // 加法结果本身是新的不可变实例，不改变参与方。
        assertThat(a.add(b).add(c)).isEqualTo(Money.of("6.00", "CNY"));
        assertThat(a.getAmount()).isEqualByComparingTo("1.00");
    }

    @Test
    void subtractRejectsDifferentCurrency() {
        assertThatThrownBy(() -> Money.of("10.00", "CNY").subtract(Money.of("5.00", "USD")))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void compareToRejectsDifferentCurrency() {
        assertThatThrownBy(() -> Money.of("10.00", "CNY").compareTo(Money.of("9.00", "USD")))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void negateZeroStaysZero() {
        assertThat(Money.zero("CNY").negate().isZero()).isTrue();
        assertThat(Money.zero("CNY").negate().getAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void equalsConsidersScaleEquivalent() {
        // 1.0 与 1.00 在业务上等价（compareTo==0 即相等），Money.equals 用 compareTo 而非精确值。
        assertThat(Money.of("10.00", "CNY").equals(Money.of("10.0", "CNY"))).isTrue();
        assertThat(Money.of("10.00", "CNY").equals(Money.of("10.000", "CNY"))).isTrue();
    }

    @Test
    void hashCodeStableForEqualInstances() {
        assertThat(Money.of("10.00", "CNY").hashCode())
                .isEqualTo(Money.of("10.00", "CNY").hashCode());
    }

    @Test
    void isZeroFalseForNonZero() {
        assertThat(Money.of("0.01", "CNY").isZero()).isFalse();
    }

    @Test
    void toStringFormatIsAmountSpaceCurrency() {
        assertThat(Money.of("12.34", "USD").toString()).isEqualTo("12.34 USD");
    }

    @Test
    void largeAmountKeepsScale() {
        // 大额不能溢出或丢失精度（BigDecimal 任意精度）。
        Money big = Money.of("999999999999.999", "CNY");
        assertThat(big.getAmount()).isEqualByComparingTo("1000000000000.00");
        assertThat(big.getCurrency()).isEqualTo("CNY");
    }

    @Test
    void ofNullAmountRejected() {
        assertThatThrownBy(() -> Money.of((BigDecimal) null, "CNY"))
                .isInstanceOf(NullPointerException.class);
    }
}
