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

/** 交易没有任何分录时抛出。 */
public class EmptyTransactionException extends RuntimeException {

    private final String transactionId;

    public EmptyTransactionException() {
        super("交易没有分录");
        this.transactionId = null;
    }

    public EmptyTransactionException(String transactionId) {
        super("交易没有分录: " + transactionId);
        this.transactionId = transactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
