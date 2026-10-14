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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 金额：不可变值对象。构造时金额固定到 2 位小数（四舍五入），且只有币种相同的两个金额才能做加减。
 *
 * <p>不同币种相加/减会抛 {@link CurrencyMismatchException}，避免把美元和人民币混在一起算。
 */
public final class Money {

    /** 金额固定精度：2 位小数。 */
    public static final int SCALE = 2;

    private final BigDecimal amount;

    private final String currency;

    private Money(BigDecimal amount, String currency) {
        this.amount = amount.setScale(SCALE, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    /** 以 BigDecimal 金额与币种构造金额；金额为 null 或币种为空均拒绝。 */
    public static Money of(BigDecimal amount, String currency) {
        Objects.requireNonNull(amount, "amount 不能为 null");
        Objects.requireNonNull(currency, "currency 不能为 null");
        if (currency.isBlank()) {
            throw new IllegalArgumentException("currency 不能为空");
        }
        return new Money(amount, currency);
    }

    /** 以字符串金额与币种构造金额。 */
    public static Money of(String amount, String currency) {
        return of(new BigDecimal(amount), currency);
    }

    /** 指定币种的零金额。 */
    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    /** 两个金额相加，要求币种一致。 */
    public Money add(Money other) {
        checkSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    /** 两个金额相减，要求币种一致。 */
    public Money subtract(Money other) {
        checkSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    /** 取相反数（正负翻转，币种不变）。 */
    public Money negate() {
        return new Money(amount.negate(), currency);
    }

    /** 金额是否为负。 */
    public boolean isNegative() {
        return amount.signum() < 0;
    }

    /** 金额是否为零。 */
    public boolean isZero() {
        return amount.signum() == 0;
    }

    /** 与另一个金额比较大小，要求币种一致。 */
    public int compareTo(Money other) {
        checkSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    private void checkSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new CurrencyMismatchException(currency, other.currency);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Money that)) {
            return false;
        }
        return currency.equals(that.currency) && amount.compareTo(that.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(currency, amount);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}
