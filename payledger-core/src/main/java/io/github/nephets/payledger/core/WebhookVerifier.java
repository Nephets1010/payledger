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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * 支付回调验签：用商户密钥对原始报文做 HMAC-SHA256，与渠道传来的签名做大小写不敏感、去空白的比较。
 *
 * <p>任何参数为空都直接判为不通过；比较采用定长字节的恒定时间算法，降低时序侧信道风险。
 */
public final class WebhookVerifier {

    /** 验证一次回调；参数缺失或签名不符返回 false。 */
    public boolean verify(VerifyContext ctx) {
        if (ctx == null
                || ctx.getSignature() == null
                || ctx.getRawBody() == null
                || ctx.getSecret() == null) {
            return false;
        }
        String expected = sign(ctx.getRawBody(), ctx.getSecret(), ctx.getAlgorithm());
        String actual = ctx.getSignature().trim().toLowerCase(Locale.ROOT);
        return constantTimeEquals(expected, actual);
    }

    /** 计算报文的 HMAC-SHA256 十六进制签名。 */
    public String sign(String body, String secret, SignatureAlgorithm algorithm) {
        Objects.requireNonNull(body, "body 不能为 null");
        Objects.requireNonNull(secret, "secret 不能为 null");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key =
                    new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            byte[] raw = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 验签失败", e);
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        byte[] x = expected.getBytes(StandardCharsets.UTF_8);
        byte[] y = actual.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < x.length; i++) {
            result |= x[i] ^ y[i];
        }
        return result == 0;
    }
}
