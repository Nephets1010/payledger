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

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class PayledgerAutoConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(PayledgerAutoConfiguration.class));

    @Test
    void registersCoreBeans() {
        runner.run(
                context -> {
                    assertThat(context).hasSingleBean(InMemoryEntryStore.class);
                    assertThat(context).hasSingleBean(InMemoryBalanceStore.class);
                    assertThat(context).hasSingleBean(PostingService.class);
                    assertThat(context).hasSingleBean(Reconciler.class);
                    assertThat(context).hasSingleBean(SettlementEngine.class);
                    assertThat(context).hasSingleBean(WebhookVerifier.class);
                    assertThat(context).hasSingleBean(InMemoryIdempotencyStore.class);
                    assertThat(context).hasSingleBean(LlmClient.class);
                    assertThat(context.getBean(LlmClient.class))
                            .isInstanceOf(KeywordLedgerAssistant.class);
                    // 非 Web 上下文不注册控制器
                    assertThat(context).doesNotHaveBean(LedgerController.class);
                });
    }

    @Test
    void wechatChannelRegisteredWhenConfigured() {
        runner.withPropertyValues("payledger.webhook.channels.wechat=demo-secret")
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(PaymentChannel.class);
                            assertThat(context.getBean(PaymentChannel.class))
                                    .isInstanceOf(WechatPayChannel.class);
                        });
    }

    @Test
    void alipayChannelRegisteredWhenConfigured() {
        runner.withPropertyValues("payledger.webhook.channels.alipay=demo-secret")
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(PaymentChannel.class);
                            assertThat(context.getBean(PaymentChannel.class))
                                    .isInstanceOf(AlipayChannel.class);
                        });
    }

    @Test
    void noChannelWhenNotConfigured() {
        runner.run(context -> assertThat(context).doesNotHaveBean(PaymentChannel.class));
    }

    @Test
    void honorsUserProvidedPostingService() {
        runner.withUserConfiguration(CustomPostingConfig.class)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(PostingService.class);
                            assertThat(context.getBean(PostingService.class))
                                    .isSameAs(CustomPostingConfig.POSTING);
                        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomPostingConfig {

        static final PostingService POSTING =
                new PostingService(new InMemoryEntryStore(), new InMemoryBalanceStore());

        @Bean
        PostingService postingService() {
            return POSTING;
        }
    }
}
