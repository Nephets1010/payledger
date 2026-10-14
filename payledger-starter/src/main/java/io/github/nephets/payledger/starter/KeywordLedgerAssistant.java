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

import io.github.nephets.payledger.core.Direction;
import io.github.nephets.payledger.core.LlmClient;
import io.github.nephets.payledger.core.Money;
import io.github.nephets.payledger.core.SuggestedEntry;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 本地记账建议：不联网，按关键词给出建议分录。
 *
 * <p>规则很简单：描述里出现收入类词（销售/收款/回款等）建议贷收入账户，出现支出类词（采购/工资/费用等）建议借费用账户，出现现金/银行类词建议借现金账户。金额从描述里取第一个数字，取不到则为
 * 0。生产可替换为接入大模型的服务，只需实现 {@link LlmClient}。
 */
public class KeywordLedgerAssistant implements LlmClient {

    private final String defaultCurrency;

    private static final Pattern NUMBER = Pattern.compile("(\\d+(?:\\.\\d+)?)");

    public KeywordLedgerAssistant() {
        this("CNY");
    }

    public KeywordLedgerAssistant(String defaultCurrency) {
        this.defaultCurrency = Objects.requireNonNull(defaultCurrency, "defaultCurrency 不能为 null");
    }

    @Override
    public List<SuggestedEntry> suggest(String description) {
        Objects.requireNonNull(description, "description 不能为 null");
        String text = description.toLowerCase(Locale.ROOT);
        Money amount = parseAmount(description);

        List<SuggestedEntry> suggestions = new ArrayList<>();
        if (containsAny(text, "销售", "收款", "收入", "卖出", "回款", "到账")) {
            suggestions.add(new SuggestedEntry("acc_revenue", Direction.CREDIT, amount, "收入"));
        }
        if (containsAny(text, "采购", "付款", "支出", "买入", "工资", "费用", "报销", "花")) {
            suggestions.add(new SuggestedEntry("acc_expense", Direction.DEBIT, amount, "费用"));
        }
        if (containsAny(text, "现金", "充值", "提现", "银行", "存款")) {
            suggestions.add(new SuggestedEntry("acc_cash", Direction.DEBIT, amount, "现金"));
        }
        if (suggestions.isEmpty()) {
            suggestions.add(new SuggestedEntry("acc_cash", Direction.DEBIT, amount, "待归类"));
        }
        return suggestions;
    }

    private Money parseAmount(String description) {
        Matcher matcher = NUMBER.matcher(description);
        if (matcher.find()) {
            return Money.of(new BigDecimal(matcher.group(1)), defaultCurrency);
        }
        return Money.zero(defaultCurrency);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
