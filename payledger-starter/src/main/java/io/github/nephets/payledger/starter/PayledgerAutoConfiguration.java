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

import io.github.nephets.payledger.core.AlipayChannel;
import io.github.nephets.payledger.core.ApplePayChannel;
import io.github.nephets.payledger.core.InMemoryBalanceStore;
import io.github.nephets.payledger.core.InMemoryEntryStore;
import io.github.nephets.payledger.core.InMemoryIdempotencyStore;
import io.github.nephets.payledger.core.LlmClient;
import io.github.nephets.payledger.core.PaymentChannel;
import io.github.nephets.payledger.core.PostingService;
import io.github.nephets.payledger.core.Reconciler;
import io.github.nephets.payledger.core.SettlementEngine;
import io.github.nephets.payledger.core.WebhookVerifier;
import io.github.nephets.payledger.core.WechatPayChannel;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * payledger 自动装配：注册过账、对账、清结算、验签、幂等等 Bean，并按 {@code payledger.webhook.channels} 里配了密钥的渠道注册对应的 {@link
 * PaymentChannel}。还提供 {@link KeywordLedgerAssistant}（本地记账建议）。仅当是 Servlet Web 应用时才注册 {@link
 * LedgerController}。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PayledgerProperties.class)
public class PayledgerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public InMemoryEntryStore entryStore() {
        return new InMemoryEntryStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public InMemoryBalanceStore balanceStore() {
        return new InMemoryBalanceStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public PostingService postingService(
            InMemoryEntryStore entryStore, InMemoryBalanceStore balanceStore) {
        return new PostingService(entryStore, balanceStore);
    }

    @Bean
    @ConditionalOnMissingBean
    public Reconciler reconciler() {
        return new Reconciler();
    }

    @Bean
    @ConditionalOnMissingBean
    public SettlementEngine settlementEngine() {
        return new SettlementEngine();
    }

    @Bean
    @ConditionalOnMissingBean
    public WebhookVerifier webhookVerifier() {
        return new WebhookVerifier();
    }

    @Bean
    @ConditionalOnMissingBean
    public InMemoryIdempotencyStore idempotencyStore() {
        return new InMemoryIdempotencyStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public LlmClient llmClient() {
        return new KeywordLedgerAssistant();
    }

    @Bean
    @ConditionalOnProperty(prefix = "payledger.webhook.channels", name = "wechat")
    @ConditionalOnMissingBean
    public PaymentChannel wechatPayChannel(PayledgerProperties properties) {
        return new WechatPayChannel(properties.getWebhook().getChannels().get("wechat"));
    }

    @Bean
    @ConditionalOnProperty(prefix = "payledger.webhook.channels", name = "alipay")
    @ConditionalOnMissingBean
    public PaymentChannel alipayChannel(PayledgerProperties properties) {
        return new AlipayChannel(properties.getWebhook().getChannels().get("alipay"));
    }

    @Bean
    @ConditionalOnProperty(prefix = "payledger.webhook.channels", name = "applepay")
    @ConditionalOnMissingBean
    public PaymentChannel applePayChannel(PayledgerProperties properties) {
        return new ApplePayChannel(properties.getWebhook().getChannels().get("applepay"));
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public LedgerController ledgerController(
            PostingService postingService,
            InMemoryBalanceStore balanceStore,
            PayledgerProperties properties) {
        return new LedgerController(postingService, balanceStore, properties);
    }
}
