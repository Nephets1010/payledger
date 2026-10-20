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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 为什么测这些：多租户上下文基于 InheritableThreadLocal，是余额/分录隔离的根基。任何「设置后未恢复」的泄漏都会让线程池串租户（资损级 bug），所以恢复语义必须严格。
 */
class PayledgerTenantContractTest {

    @AfterEach
    void tearDown() {
        PayledgerTenant.clear();
    }

    @Test
    void setCurrentClearRoundTrip() {
        assertThat(PayledgerTenant.isSet()).isFalse();
        PayledgerTenant.set("t1");
        assertThat(PayledgerTenant.current()).isEqualTo("t1");
        assertThat(PayledgerTenant.isSet()).isTrue();
        PayledgerTenant.clear();
        assertThat(PayledgerTenant.current()).isNull();
        assertThat(PayledgerTenant.isSet()).isFalse();
    }

    @Test
    void runWithRestoresPreviousContext() {
        PayledgerTenant.set("outer");
        try {
            PayledgerTenant.runWith(
                    "inner", () -> assertThat(PayledgerTenant.current()).isEqualTo("inner"));
            // 即使内层改了，外层原有上下文必须复原。
            assertThat(PayledgerTenant.current()).isEqualTo("outer");
        } finally {
            PayledgerTenant.clear();
        }
    }

    @Test
    void nestedRunWithRestoresEachLevel() {
        PayledgerTenant.set("a");
        try {
            PayledgerTenant.runWith(
                    "b",
                    () -> {
                        assertThat(PayledgerTenant.current()).isEqualTo("b");
                        PayledgerTenant.runWith(
                                "c", () -> assertThat(PayledgerTenant.current()).isEqualTo("c"));
                        assertThat(PayledgerTenant.current()).isEqualTo("b");
                    });
            assertThat(PayledgerTenant.current()).isEqualTo("a");
        } finally {
            PayledgerTenant.clear();
        }
    }

    @Test
    void withReturnsValueAndRestores() throws Exception {
        PayledgerTenant.set("outer");
        try {
            String result =
                    PayledgerTenant.with("inner", () -> "computed-" + PayledgerTenant.current());
            assertThat(result).isEqualTo("computed-inner");
            assertThat(PayledgerTenant.current()).isEqualTo("outer");
        } finally {
            PayledgerTenant.clear();
        }
    }

    @Test
    void withRestoresEvenWhenTaskThrows() {
        PayledgerTenant.set("outer");
        try {
            assertThatThrownBy(
                            () ->
                                    PayledgerTenant.with(
                                            "inner",
                                            () -> {
                                                throw new IllegalStateException("boom");
                                            }))
                    .isInstanceOf(IllegalStateException.class);
            // 异常路径也必须恢复上下文，否则线程被污染。
            assertThat(PayledgerTenant.current()).isEqualTo("outer");
        } finally {
            PayledgerTenant.clear();
        }
    }

    @Test
    void requireThrowsWhenUnset() {
        PayledgerTenant.clear();
        assertThatThrownBy(PayledgerTenant::require).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void requireReturnsCurrentWhenSet() {
        PayledgerTenant.set("t9");
        try {
            assertThat(PayledgerTenant.require()).isEqualTo("t9");
        } finally {
            PayledgerTenant.clear();
        }
    }

    @Test
    void setOverridesExistingValue() {
        PayledgerTenant.set("first");
        PayledgerTenant.set("second");
        assertThat(PayledgerTenant.current()).isEqualTo("second");
    }
}
