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

import java.util.Objects;

/**
 * 验签上下文：一次支付回调需要的所有信息。由各 {@link PaymentChannel#parse} 从原始报文与请求头解析得到。
 *
 * <p>{@code signature} 来自渠道头或报文体，{@code secret} 是商户密钥（由 starter 按渠道注入）。
 */
public final class VerifyContext {

    private final String channelId;

    private final String rawBody;

    private final String signature;

    private final String secret;

    private final SignatureAlgorithm algorithm;

    public VerifyContext(
            String channelId,
            String rawBody,
            String signature,
            String secret,
            SignatureAlgorithm algorithm) {
        this.channelId = Objects.requireNonNull(channelId, "channelId 不能为 null");
        this.rawBody = rawBody;
        this.signature = signature;
        this.secret = secret;
        this.algorithm = algorithm == null ? SignatureAlgorithm.HMAC_SHA256 : algorithm;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getRawBody() {
        return rawBody;
    }

    public String getSignature() {
        return signature;
    }

    public String getSecret() {
        return secret;
    }

    public SignatureAlgorithm getAlgorithm() {
        return algorithm;
    }
}
