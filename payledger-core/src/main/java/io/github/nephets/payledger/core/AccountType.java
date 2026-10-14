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

/** 账户类型：资产、负债、权益、收入、费用。 */
public enum AccountType {
    ASSET("资产"),
    LIABILITY("负债"),
    EQUITY("权益"),
    INCOME("收入"),
    EXPENSE("费用");

    private final String label;

    AccountType(String label) {
        this.label = label;
    }

    /** 中文名称。 */
    public String getLabel() {
        return label;
    }
}
