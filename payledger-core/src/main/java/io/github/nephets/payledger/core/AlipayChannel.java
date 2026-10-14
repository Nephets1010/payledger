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
 * 支付宝渠道（参考实现）：从 {@code Alipay-Signature} 头（或表单 {@code sign} 字段）取签名。
 *
 * <p>支付宝生产环境使用 SHA256withRSA（RSA2）配合平台公钥验签。这里为了不引入加密依赖、可独立运行，先用 HMAC-SHA256
 * 简化版：密钥由商户在配置里提供。接入生产时应替换为 RSA2 验签。
 */
public final class AlipayChannel implements PaymentChannel {

    private final String secret;

    public AlipayChannel(String secret) {
        this.secret = Objects.requireNonNull(secret, "secret 不能为 null");
    }

    @Override
    public String channelId() {
        return "alipay";
    }

    @Override
    public VerifyContext parse(String rawBody, Map<String, String> headers) {
        String signature = null;
        if (headers != null) {
            signature = headers.get("Alipay-Signature");
            if (signature == null) {
                signature = headers.get("sign");
            }
        }
        return new VerifyContext(
                "alipay", rawBody, signature, secret, SignatureAlgorithm.HMAC_SHA256);
    }
}
