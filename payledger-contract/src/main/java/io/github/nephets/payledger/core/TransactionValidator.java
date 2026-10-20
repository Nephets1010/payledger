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
import io.github.nephets.payledger.core.exception.EmptyTransactionException;
import io.github.nephets.payledger.core.exception.UnbalancedEntryException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 交易校验：检查借贷平衡、币种一致、非空分录。
 *
 * <p>{@link #validate} 在校验失败时抛出对应异常；{@link #isBalanced} 仅返回布尔结果，供 {@link Transaction#isBalanced()}
 * 调用。
 */
public final class TransactionValidator {

    private TransactionValidator() {}

    /**
     * 校验交易；不通过时抛出 {@link EmptyTransactionException}/{@link CurrencyMismatchException}/{@link
     * UnbalancedEntryException}。
     */
    public static void validate(Transaction tx) {
        Objects.requireNonNull(tx, "transaction 不能为 null");
        List<LedgerEntry> entries = tx.getEntries();
        if (entries.isEmpty()) {
            throw new EmptyTransactionException(tx.getTransactionId());
        }
        String currency = null;
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        for (LedgerEntry entry : entries) {
            Money amount = entry.getAmount();
            if (currency == null) {
                currency = amount.getCurrency();
            } else if (!currency.equals(amount.getCurrency())) {
                throw new CurrencyMismatchException(currency, amount.getCurrency());
            }
            if (entry.getDirection() == Direction.DEBIT) {
                debit = debit.add(amount.getAmount());
            } else {
                credit = credit.add(amount.getAmount());
            }
        }
        if (debit.compareTo(credit) != 0) {
            throw new UnbalancedEntryException(
                    Money.of(debit, currency), Money.of(credit, currency));
        }
    }

    /** 交易是否借贷平衡且币种一致（不抛异常）。 */
    public static boolean isBalanced(Transaction tx) {
        try {
            validate(tx);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
