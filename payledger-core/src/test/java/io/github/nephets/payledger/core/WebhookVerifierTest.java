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

import static org.assertj.core.api.Assertions.assertThat;

class WebhookVerifierTest {

    private final WebhookVerifier verifier = new WebhookVerifier();

    @Test
    void validSignaturePasses() {
        String body = "{\"id\":\"pay_1\"}";
        String secret = "demo-secret";
        String sig = verifier.sign(body, secret, SignatureAlgorithm.HMAC_SHA256);
        VerifyContext ctx =
                new VerifyContext("wechat", body, sig, secret, SignatureAlgorithm.HMAC_SHA256);
        assertThat(verifier.verify(ctx)).isTrue();
    }

    @Test
    void caseInsensitiveAndTrimmed() {
        String body = "payload";
        String secret = "k";
        String sig = verifier.sign(body, secret, SignatureAlgorithm.HMAC_SHA256).toUpperCase();
        // 报文原样参与签名；只有签名本身做去空白、大小写不敏感处理。
        VerifyContext ctx =
                new VerifyContext(
                        "wechat", body, "  " + sig + "  ", secret, SignatureAlgorithm.HMAC_SHA256);
        assertThat(verifier.verify(ctx)).isTrue();
    }

    @Test
    void wrongSignatureFails() {
        VerifyContext ctx =
                new VerifyContext(
                        "wechat", "body", "deadbeef", "secret", SignatureAlgorithm.HMAC_SHA256);
        assertThat(verifier.verify(ctx)).isFalse();
    }

    @Test
    void differentLengthSignatureFails() {
        String sig = verifier.sign("body", "secret", SignatureAlgorithm.HMAC_SHA256);
        VerifyContext ctx =
                new VerifyContext(
                        "wechat", "body", sig + "ff", "secret", SignatureAlgorithm.HMAC_SHA256);
        assertThat(verifier.verify(ctx)).isFalse();
    }

    @Test
    void missingFieldsReturnFalse() {
        assertThat(verifier.verify(null)).isFalse();
        assertThat(
                        verifier.verify(
                                new VerifyContext(
                                        "w",
                                        null,
                                        "sig",
                                        "secret",
                                        SignatureAlgorithm.HMAC_SHA256)))
                .isFalse();
        assertThat(
                        verifier.verify(
                                new VerifyContext(
                                        "w",
                                        "body",
                                        null,
                                        "secret",
                                        SignatureAlgorithm.HMAC_SHA256)))
                .isFalse();
        assertThat(
                        verifier.verify(
                                new VerifyContext(
                                        "w", "body", "sig", null, SignatureAlgorithm.HMAC_SHA256)))
                .isFalse();
    }

    @Test
    void signProducesHexAndIsDeterministic() {
        String a = verifier.sign("x", "s", SignatureAlgorithm.HMAC_SHA256);
        String b = verifier.sign("x", "s", SignatureAlgorithm.HMAC_SHA256);
        assertThat(a).isEqualTo(b);
        assertThat(a).matches("[0-9a-f]+");
    }
}
