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

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PayledgerPropertiesTest {

    @Test
    void defaultsAreSensible() {
        PayledgerProperties properties = new PayledgerProperties();
        assertThat(properties.getDefaultCurrency()).isEqualTo("CNY");
        assertThat(properties.getTenantHeader()).isEqualTo("X-Tenant-Id");
        assertThat(properties.getWebhook().isEnabled()).isTrue();
        assertThat(properties.getWebhook().getChannels()).isEmpty();
    }

    @Test
    void settersUpdateValues() {
        PayledgerProperties properties = new PayledgerProperties();
        properties.setDefaultCurrency("USD");
        properties.setTenantHeader("X-Org-Id");
        assertThat(properties.getDefaultCurrency()).isEqualTo("USD");
        assertThat(properties.getTenantHeader()).isEqualTo("X-Org-Id");

        Map<String, String> channels = new LinkedHashMap<>();
        channels.put("wechat", "secret");
        properties.getWebhook().setEnabled(false);
        properties.getWebhook().setChannels(channels);
        assertThat(properties.getWebhook().isEnabled()).isFalse();
        assertThat(properties.getWebhook().getChannels()).containsEntry("wechat", "secret");
    }
}
