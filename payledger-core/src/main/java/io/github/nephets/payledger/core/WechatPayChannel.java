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
 * 微信支付渠道（参考实现）：从 {@code Wechatpay-Signature} 头取签名，用商户 APIv3 密钥做 HMAC-SHA256 验签。
 *
 * <p>这是可独立运行的参考实现，不联网；生产环境应使用平台证书做非对称验签，并把商户密钥放到配置中心。
 */
public final class WechatPayChannel implements PaymentChannel {

    private final String secret;

    public WechatPayChannel(String secret) {
        this.secret = Objects.requireNonNull(secret, "secret 不能为 null");
    }

    @Override
    public String channelId() {
        return "wechat";
    }

    @Override
    public VerifyContext parse(String rawBody, Map<String, String> headers) {
        String signature = headers == null ? null : headers.get("Wechatpay-Signature");
        return new VerifyContext(
                "wechat", rawBody, signature, secret, SignatureAlgorithm.HMAC_SHA256);
    }
}
