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

import java.util.List;

/**
 * 记账建议接口：根据一段业务描述，给出建议的分录草稿。
 *
 * <p>这是零依赖的纯接口，core 不绑定任何大模型 SDK。starter 提供基于关键词的本地实现 {@code KeywordLedgerAssistant}。
 */
public interface LlmClient {

    /** 根据描述返回一组建议分录。 */
    List<SuggestedEntry> suggest(String description);
}
