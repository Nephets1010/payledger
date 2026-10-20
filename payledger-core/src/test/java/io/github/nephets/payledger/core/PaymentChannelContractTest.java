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

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** 为什么测这些：三家渠道的参考实现都只做 HMAC 验签，差异仅在取签名的请求头与 channelId，必须验证「缺头时拿不到签名」与「渠道标识正确」这些契约。 */
class PaymentChannelContractTest {

    private final WebhookVerifier verifier = new WebhookVerifier();

    @Test
    void channelIdsAreDistinct() {
        assertThat(new WechatPayChannel("s").channelId()).isEqualTo("wechat");
        assertThat(new AlipayChannel("s").channelId()).isEqualTo("alipay");
        assertThat(new ApplePayChannel("s").channelId()).isEqualTo("applepay");
    }

    @Test
    void wechatWithoutHeaderHasNoSignature() {
        VerifyContext ctx = new WechatPayChannel("s").parse("body", Map.of());
        assertThat(ctx.getSignature()).isNull();
        // 没有签名，验签必然失败。
        assertThat(verifier.verify(ctx)).isFalse();
    }

    @Test
    void alipayWithoutHeaderHasNoSignature() {
        VerifyContext ctx = new AlipayChannel("s").parse("body", Map.of());
        assertThat(ctx.getSignature()).isNull();
        assertThat(verifier.verify(ctx)).isFalse();
    }

    @Test
    void applepayWithoutHeaderHasNoSignature() {
        VerifyContext ctx = new ApplePayChannel("s").parse("body", Map.of());
        assertThat(ctx.getSignature()).isNull();
        assertThat(verifier.verify(ctx)).isFalse();
    }

    @Test
    void allChannelsDefaultParseEventEmpty() {
        assertThat(new WechatPayChannel("s").parseEvent("x")).isEqualTo(Optional.empty());
        assertThat(new AlipayChannel("s").parseEvent("x")).isEqualTo(Optional.empty());
        assertThat(new ApplePayChannel("s").parseEvent("x")).isEqualTo(Optional.empty());
    }

    @Test
    void parsedContextCarriesChannelIdAndSecret() {
        VerifyContext ctx = new AlipayChannel("my-secret").parse("b", Map.of("sign", "v"));
        assertThat(ctx.getChannelId()).isEqualTo("alipay");
        assertThat(ctx.getSecret()).isEqualTo("my-secret");
    }
}
