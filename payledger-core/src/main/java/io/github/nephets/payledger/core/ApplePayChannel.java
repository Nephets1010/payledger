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

import java.util.Map;
import java.util.Objects;

/**
 * Apple Pay 渠道（参考实现）：从 {@code signature} 头取签名，用商户密钥做 HMAC-SHA256 验签。
 *
 * <p>这是可独立运行的参考实现，不联网；生产环境应按 Apple Pay 文档接入真实密钥与证书。
 */
public final class ApplePayChannel implements PaymentChannel {

    private final String secret;

    public ApplePayChannel(String secret) {
        this.secret = Objects.requireNonNull(secret, "secret 不能为 null");
    }

    @Override
    public String channelId() {
        return "applepay";
    }

    @Override
    public VerifyContext parse(String rawBody, Map<String, String> headers) {
        String signature = headers == null ? null : headers.get("signature");
        return new VerifyContext(
                "applepay", rawBody, signature, secret, SignatureAlgorithm.HMAC_SHA256);
    }
}
