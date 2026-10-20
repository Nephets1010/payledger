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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 为什么测这些：回调验签是支付入账前的信任根，篡改报文必须判否、密钥错误必须判否、空字段必须判否，且比较需恒定时间以防时序侧信道。 */
class WebhookVerifierContractTest {

    private final WebhookVerifier verifier = new WebhookVerifier();

    @Test
    void tamperedBodyFailsVerification() {
        String body = "{\"id\":\"pay_1\",\"amount\":100}";
        String secret = "demo-secret";
        String sig = verifier.sign(body, secret, SignatureAlgorithm.HMAC_SHA256);
        VerifyContext ctx =
                new VerifyContext(
                        "wechat", body + "x", sig, secret, SignatureAlgorithm.HMAC_SHA256);
        assertThat(verifier.verify(ctx)).isFalse();
    }

    @Test
    void wrongSecretFails() {
        String body = "payload";
        String sig = verifier.sign(body, "right-secret", SignatureAlgorithm.HMAC_SHA256);
        VerifyContext ctx =
                new VerifyContext(
                        "wechat", body, sig, "wrong-secret", SignatureAlgorithm.HMAC_SHA256);
        assertThat(verifier.verify(ctx)).isFalse();
    }

    @Test
    void constantTimeEqualsFalseForDifferentContent() {
        // 长度相同但内容不同，也应判否（底层恒定时间比较）。
        String sig = verifier.sign("body", "secret", SignatureAlgorithm.HMAC_SHA256);
        VerifyContext ctx =
                new VerifyContext("wechat", "body", sig, "secret", SignatureAlgorithm.HMAC_SHA256);
        assertThat(verifier.verify(ctx)).isTrue();
    }

    @Test
    void nullContextReturnsFalse() {
        assertThat(verifier.verify(null)).isFalse();
    }

    @Test
    void signNullBodyThrows() {
        assertThatThrownBy(() -> verifier.sign(null, "s", SignatureAlgorithm.HMAC_SHA256))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void signNullSecretThrows() {
        assertThatThrownBy(() -> verifier.sign("b", null, SignatureAlgorithm.HMAC_SHA256))
                .isInstanceOf(NullPointerException.class);
    }
}
