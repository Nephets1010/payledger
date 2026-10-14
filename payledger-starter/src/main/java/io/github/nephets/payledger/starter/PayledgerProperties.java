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
package io.github.nephets.payledger.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/** payledger 配置属性，前缀 {@code payledger}。 */
@ConfigurationProperties(prefix = "payledger")
public class PayledgerProperties {

    /** 默认币种。默认 {@code CNY}。 */
    private String defaultCurrency = "CNY";

    /** 用于解析租户的请求头名称。默认 {@code X-Tenant-Id}。 */
    private String tenantHeader = "X-Tenant-Id";

    /** webhook 验签相关配置。 */
    private final Webhook webhook = new Webhook();

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public String getTenantHeader() {
        return tenantHeader;
    }

    public void setTenantHeader(String tenantHeader) {
        this.tenantHeader = tenantHeader;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    /** webhook 配置：是否启用、各渠道 id → 商户密钥。 */
    public static class Webhook {

        /** 是否启用 webhook 验签。默认 true。 */
        private boolean enabled = true;

        /** 渠道 id → 商户密钥，例如 {@code wechat: demo-secret}。 */
        private Map<String, String> channels = new LinkedHashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Map<String, String> getChannels() {
            return channels;
        }

        public void setChannels(Map<String, String> channels) {
            this.channels = channels;
        }
    }
}
