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
package io.github.nephets.payledger.sample;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PayledgerSampleApplicationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void contextLoads() {}

    @Test
    void postTransactionThenQueryBalance() throws Exception {
        String body =
                "{"
                        + "\"transactionId\":\"tx1\","
                        + "\"tenantId\":\"t1\","
                        + "\"memo\":\"收款\","
                        + "\"entries\":["
                        + "{\"accountId\":\"cash\",\"direction\":\"DEBIT\",\"amount\":\"100.00\",\"currency\":\"CNY\"},"
                        + "{\"accountId\":\"revenue\",\"direction\":\"CREDIT\",\"amount\":\"100.00\",\"currency\":\"CNY\"}"
                        + "]}";

        mockMvc.perform(
                        post("/payledger/transactions")
                                .header("X-Tenant-Id", "t1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("tx1"))
                .andExpect(jsonPath("$.status").value("POSTED"));

        mockMvc.perform(get("/payledger/balances/cash").header("X-Tenant-Id", "t1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("cash"))
                .andExpect(jsonPath("$.amount").value("100.00"));
    }

    @Test
    void balanceOfUnknownAccountReturns404() throws Exception {
        mockMvc.perform(get("/payledger/balances/absent").header("X-Tenant-Id", "t1"))
                .andExpect(status().isNotFound());
    }
}
