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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostTransactionRequestTest {

    @Test
    void settersAndGettersRoundTrip() {
        PostTransactionRequest request = new PostTransactionRequest();
        request.setTransactionId("tx");
        request.setTenantId("t1");
        request.setMemo("memo");

        PostTransactionRequest.EntryLine line = new PostTransactionRequest.EntryLine();
        line.setAccountId("acc");
        line.setDirection("DEBIT");
        line.setCurrency("CNY");
        line.setAmount("100.00");
        line.setReference("ref");
        line.setMemo("lineMemo");

        List<PostTransactionRequest.EntryLine> lines = Arrays.asList(line);
        request.setEntries(lines);

        assertThat(request.getTransactionId()).isEqualTo("tx");
        assertThat(request.getTenantId()).isEqualTo("t1");
        assertThat(request.getMemo()).isEqualTo("memo");
        assertThat(request.getEntries()).hasSize(1);

        PostTransactionRequest.EntryLine got = request.getEntries().get(0);
        assertThat(got.getAccountId()).isEqualTo("acc");
        assertThat(got.getDirection()).isEqualTo("DEBIT");
        assertThat(got.getCurrency()).isEqualTo("CNY");
        assertThat(got.getAmount()).isEqualTo("100.00");
        assertThat(got.getReference()).isEqualTo("ref");
        assertThat(got.getMemo()).isEqualTo("lineMemo");
    }
}
