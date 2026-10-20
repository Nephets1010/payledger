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

import io.github.nephets.payledger.core.InMemoryBalanceStore;
import io.github.nephets.payledger.core.InMemoryEntryStore;
import io.github.nephets.payledger.core.PayledgerTenant;
import io.github.nephets.payledger.core.PostingService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.ServletException;

class LedgerControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PayledgerProperties properties = new PayledgerProperties();
        InMemoryEntryStore entryStore = new InMemoryEntryStore();
        InMemoryBalanceStore balanceStore = new InMemoryBalanceStore();
        PostingService postingService = new PostingService(entryStore, balanceStore);
        LedgerController controller =
                new LedgerController(postingService, balanceStore, properties);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    void tearDown() {
        PayledgerTenant.clear();
    }

    @Test
    void postsTransactionAndReturnsPosted() throws Exception {
        String body =
                "{\"transactionId\":\"tx1\",\"tenantId\":\"demo\",\"memo\":\"sale\","
                        + "\"entries\":["
                        + "{\"accountId\":\"acc_cash\",\"direction\":\"DEBIT\",\"amount\":\"100.00\","
                        + "\"currency\":\"CNY\",\"reference\":\"R1\",\"memo\":\"cash\"},"
                        + "{\"accountId\":\"acc_rev\",\"direction\":\"CREDIT\",\"amount\":\"100.00\","
                        + "\"currency\":\"CNY\",\"reference\":\"R1\",\"memo\":\"rev\"}"
                        + "]}";
        mockMvc.perform(
                        post("/payledger/transactions")
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("tx1"))
                .andExpect(jsonPath("$.status").value("POSTED"));
    }

    @Test
    void queriesBalanceAfterPost() throws Exception {
        String body =
                "{\"transactionId\":\"tx2\",\"tenantId\":\"demo\","
                        + "\"entries\":["
                        + "{\"accountId\":\"acc_cash\",\"direction\":\"DEBIT\",\"amount\":\"100.00\","
                        + "\"currency\":\"CNY\"},"
                        + "{\"accountId\":\"acc_rev\",\"direction\":\"CREDIT\",\"amount\":\"100.00\","
                        + "\"currency\":\"CNY\"}"
                        + "]}";
        mockMvc.perform(
                        post("/payledger/transactions")
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isOk());
        mockMvc.perform(get("/payledger/balances/acc_cash").header("X-Tenant-Id", "demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("acc_cash"))
                .andExpect(jsonPath("$.tenantId").value("demo"))
                .andExpect(jsonPath("$.currency").value("CNY"))
                .andExpect(jsonPath("$.amount").value("100.00"));
    }

    @Test
    void missingCurrencyFallsBackToDefault() throws Exception {
        String body =
                "{\"transactionId\":\"tx3\",\"tenantId\":\"demo\","
                        + "\"entries\":["
                        + "{\"accountId\":\"acc_x\",\"direction\":\"DEBIT\",\"amount\":\"50.00\"},"
                        + "{\"accountId\":\"acc_y\",\"direction\":\"CREDIT\",\"amount\":\"50.00\","
                        + "\"currency\":\"CNY\"}"
                        + "]}";
        mockMvc.perform(
                        post("/payledger/transactions")
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isOk());
        mockMvc.perform(get("/payledger/balances/acc_x").header("X-Tenant-Id", "demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("CNY"))
                .andExpect(jsonPath("$.amount").value("50.00"));
    }

    @Test
    void resolvesTenantFromHeader() throws Exception {
        String body =
                "{\"transactionId\":\"tx4\",\"entries\":["
                        + "{\"accountId\":\"acc_h\",\"direction\":\"DEBIT\",\"amount\":\"10.00\","
                        + "\"currency\":\"CNY\"},"
                        + "{\"accountId\":\"acc_hc\",\"direction\":\"CREDIT\",\"amount\":\"10.00\","
                        + "\"currency\":\"CNY\"}"
                        + "]}";
        mockMvc.perform(
                        post("/payledger/transactions")
                                .header("X-Tenant-Id", "headerTenant")
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("tx4"));
    }

    @Test
    void resolvesTenantFromThreadContext() throws Exception {
        PayledgerTenant.set("threadTenant");
        try {
            String body =
                    "{\"transactionId\":\"tx5\",\"entries\":["
                            + "{\"accountId\":\"acc_t\",\"direction\":\"DEBIT\",\"amount\":\"10.00\","
                            + "\"currency\":\"CNY\"},"
                            + "{\"accountId\":\"acc_tc\",\"direction\":\"CREDIT\",\"amount\":\"10.00\","
                            + "\"currency\":\"CNY\"}"
                            + "]}";
            mockMvc.perform(
                            post("/payledger/transactions")
                                    .contentType("application/json")
                                    .content(body))
                    .andExpect(status().isOk());
        } finally {
            PayledgerTenant.clear();
        }
    }

    @Test
    void unknownAccountReturns404() throws Exception {
        mockMvc.perform(get("/payledger/balances/nope").header("X-Tenant-Id", "demo"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void missingTenantPropagatesError() {
        String body =
                "{\"transactionId\":\"tx6\",\"entries\":["
                        + "{\"accountId\":\"acc_z\",\"direction\":\"DEBIT\",\"amount\":\"10.00\","
                        + "\"currency\":\"CNY\"},"
                        + "{\"accountId\":\"acc_zc\",\"direction\":\"CREDIT\",\"amount\":\"10.00\","
                        + "\"currency\":\"CNY\"}"
                        + "]}";
        assertThatThrownBy(
                        () ->
                                mockMvc.perform(
                                                post("/payledger/transactions")
                                                        .contentType("application/json")
                                                        .content(body))
                                        .andReturn())
                .isInstanceOf(ServletException.class);
    }

    @Test
    void emptyEntriesReturns400() throws Exception {
        String body = "{\"transactionId\":\"tx7\",\"tenantId\":\"demo\",\"entries\":[]}";
        mockMvc.perform(
                        post("/payledger/transactions")
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidDirectionReturns400() throws Exception {
        String body =
                "{\"transactionId\":\"tx8\",\"tenantId\":\"demo\",\"entries\":["
                        + "{\"accountId\":\"acc_cash\",\"direction\":\"SIDEWAYS\",\"amount\":\"10.00\"},"
                        + "{\"accountId\":\"acc_rev\",\"direction\":\"CREDIT\",\"amount\":\"10.00\"}"
                        + "]}";
        mockMvc.perform(
                        post("/payledger/transactions")
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidAmountReturns400() throws Exception {
        String body =
                "{\"transactionId\":\"tx9\",\"tenantId\":\"demo\",\"entries\":["
                        + "{\"accountId\":\"acc_cash\",\"direction\":\"DEBIT\",\"amount\":\"not-a-number\"},"
                        + "{\"accountId\":\"acc_rev\",\"direction\":\"CREDIT\",\"amount\":\"10.00\"}"
                        + "]}";
        mockMvc.perform(
                        post("/payledger/transactions")
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nullDirectionReturns400() throws Exception {
        String body =
                "{\"transactionId\":\"tx10\",\"tenantId\":\"demo\",\"entries\":["
                        + "{\"accountId\":\"acc_cash\",\"amount\":\"10.00\"},"
                        + "{\"accountId\":\"acc_rev\",\"direction\":\"CREDIT\",\"amount\":\"10.00\"}"
                        + "]}";
        mockMvc.perform(
                        post("/payledger/transactions")
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isBadRequest());
    }
}
