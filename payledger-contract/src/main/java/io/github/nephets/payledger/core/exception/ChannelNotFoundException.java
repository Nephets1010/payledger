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
package io.github.nephets.payledger.core.exception;

/** 未配置或未找到指定支付渠道时抛出。 */
public class ChannelNotFoundException extends RuntimeException {

    private final String channelId;

    public ChannelNotFoundException(String channelId) {
        super("支付渠道不存在: " + channelId);
        this.channelId = channelId;
    }

    public String getChannelId() {
        return channelId;
    }
}
