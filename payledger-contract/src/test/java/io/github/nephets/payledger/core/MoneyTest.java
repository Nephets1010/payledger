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

class MoneyTest {

    @Test
    void ofWithBigDecimalKeepsScaleTwo() {
        Money m = Money.of(new BigDecimal("100.005"), "CNY");
        assertThat(m.getAmount()).isEqualByComparingTo("100.01");
        assertThat(m.getCurrency()).isEqualTo("CNY");
    }

    @Test
    void ofWithStringWorks() {
        Money m = Money.of("50.00", "USD");
        assertThat(m.getAmount()).isEqualByComparingTo("50.00");
        assertThat(m.getCurrency()).isEqualTo("USD");
    }

    @Test
    void zeroReturnsZeroForCurrency() {
        Money z = Money.zero("CNY");
        assertThat(z.isZero()).isTrue();
        assertThat(z.getCurrency()).isEqualTo("CNY");
    }

    @Test
    void addSumsSameCurrency() {
        Money sum = Money.of("10.00", "CNY").add(Money.of("5.50", "CNY"));
        assertThat(sum.getAmount()).isEqualByComparingTo("15.50");
    }

    @Test
    void addRejectsDifferentCurrency() {
        assertThatThrownBy(() -> Money.of("10.00", "CNY").add(Money.of("5.00", "USD")))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void subtractWorks() {
        Money diff = Money.of("10.00", "CNY").subtract(Money.of("4.00", "CNY"));
        assertThat(diff.getAmount()).isEqualByComparingTo("6.00");
    }

    @Test
    void negateFlipsSign() {
        assertThat(Money.of("10.00", "CNY").negate().getAmount()).isEqualByComparingTo("-10.00");
    }

    @Test
    void isNegativeAndIsZero() {
        assertThat(Money.of("-1.00", "CNY").isNegative()).isTrue();
        assertThat(Money.of("1.00", "CNY").isNegative()).isFalse();
        assertThat(Money.zero("CNY").isZero()).isTrue();
        assertThat(Money.of("0.01", "CNY").isZero()).isFalse();
    }

    @Test
    void compareToSameCurrency() {
        assertThat(Money.of("10.00", "CNY").compareTo(Money.of("9.00", "CNY"))).isPositive();
        assertThat(Money.of("10.00", "CNY").compareTo(Money.of("10.00", "CNY"))).isZero();
    }

    @Test
    void equalsAndHashCode() {
        assertThat(Money.of("10.00", "CNY")).isEqualTo(Money.of("10.00", "CNY"));
        assertThat(Money.of("10.00", "CNY")).isNotEqualTo(Money.of("10.00", "USD"));
        assertThat(Money.of("10.00", "CNY")).isNotEqualTo(Money.of("9.00", "CNY"));
        assertThat(Money.of("10.00", "CNY")).isNotEqualTo("x");
        assertThat(Money.of("10.00", "CNY").hashCode())
                .isEqualTo(Money.of("10.00", "CNY").hashCode());
    }

    @Test
    void toStringContainsAmountAndCurrency() {
        assertThat(Money.of("10.00", "CNY").toString()).contains("10.00").contains("CNY");
    }

    @Test
    void nullInputsRejected() {
        assertThatThrownBy(() -> Money.of((BigDecimal) null, "CNY"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Money.of(BigDecimal.TEN, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Money.of(BigDecimal.TEN, "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
