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

class PaymentChannelTest {

    private final WebhookVerifier verifier = new WebhookVerifier();

    private VerifyContext signAndParse(PaymentChannel channel, String headerName, String body) {
        String secret = "demo-secret";
        String sig = verifier.sign(body, secret, SignatureAlgorithm.HMAC_SHA256);
        VerifyContext ctx = channel.parse(body, Map.of(headerName, sig));
        assertThat(ctx.getSecret()).isEqualTo(secret);
        assertThat(ctx.getAlgorithm()).isEqualTo(SignatureAlgorithm.HMAC_SHA256);
        return ctx;
    }

    @Test
    void wechatParsesSignatureHeaderAndVerifies() {
        WechatPayChannel channel = new WechatPayChannel("demo-secret");
        assertThat(channel.channelId()).isEqualTo("wechat");
        VerifyContext ctx = signAndParse(channel, "Wechatpay-Signature", "{\"id\":\"1\"}");
        assertThat(verifier.verify(ctx)).isTrue();
    }

    @Test
    void wechatWrongSignatureFails() {
        WechatPayChannel channel = new WechatPayChannel("demo-secret");
        VerifyContext ctx = channel.parse("body", Map.of("Wechatpay-Signature", "bad"));
        assertThat(verifier.verify(ctx)).isFalse();
    }

    @Test
    void alipayUsesAlipaySignatureHeader() {
        AlipayChannel channel = new AlipayChannel("demo-secret");
        assertThat(channel.channelId()).isEqualTo("alipay");
        VerifyContext ctx = signAndParse(channel, "Alipay-Signature", "notify");
        assertThat(verifier.verify(ctx)).isTrue();
    }

    @Test
    void alipayFallsBackToSignField() {
        AlipayChannel channel = new AlipayChannel("demo-secret");
        VerifyContext ctx = signAndParse(channel, "sign", "notify");
        assertThat(verifier.verify(ctx)).isTrue();
    }

    @Test
    void applePayUsesSignatureHeader() {
        ApplePayChannel channel = new ApplePayChannel("demo-secret");
        assertThat(channel.channelId()).isEqualTo("applepay");
        VerifyContext ctx = signAndParse(channel, "signature", "apple");
        assertThat(verifier.verify(ctx)).isTrue();
    }

    @Test
    void parseEventDefaultsToEmpty() {
        assertThat(new WechatPayChannel("s").parseEvent("x")).isEqualTo(Optional.empty());
    }
}
