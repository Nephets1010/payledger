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
package io.github.nephets.payledger.core.exception;

import io.github.nephets.payledger.core.Money;

/** 交易借贷不平衡时抛出：借方合计与贷方合计不一致。 */
public class UnbalancedEntryException extends RuntimeException {

    private final Money debitTotal;

    private final Money creditTotal;

    public UnbalancedEntryException(Money debitTotal, Money creditTotal) {
        super("借贷不平衡：借方 " + debitTotal + "，贷方 " + creditTotal);
        this.debitTotal = debitTotal;
        this.creditTotal = creditTotal;
    }

    public Money getDebitTotal() {
        return debitTotal;
    }

    public Money getCreditTotal() {
        return creditTotal;
    }
}
