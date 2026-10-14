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
import java.util.Optional;

/**
 * 支付渠道 SPI：把原始回调报文解析成待验签的 {@link VerifyContext}，可选解析成 {@link ExternalPaymentEvent}。
 *
 * <p>这些实现是参考实现，只做 HMAC 验签，不联网；生产环境应按各渠道文档接入真实密钥与证书。
 */
public interface PaymentChannel {

    /** 渠道标识，如 {@code wechat}/{@code alipay}/{@code applepay}。 */
    String channelId();

    /** 从原始报文与请求头解析出验签上下文（签名与密钥已就位）。 */
    VerifyContext parse(String rawBody, Map<String, String> headers);

    /** 可选：把原始报文解析成结构化的支付事件；默认不解析。 */
    default Optional<ExternalPaymentEvent> parseEvent(String rawBody) {
        return Optional.empty();
    }
}
