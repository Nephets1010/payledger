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

class PayledgerTenantTest {

    @AfterEach
    void tearDown() {
        PayledgerTenant.clear();
    }

    @Test
    void setCurrentClear() {
        assertThat(PayledgerTenant.isSet()).isFalse();
        PayledgerTenant.set("acme");
        assertThat(PayledgerTenant.current()).isEqualTo("acme");
        assertThat(PayledgerTenant.isSet()).isTrue();
        PayledgerTenant.clear();
        assertThat(PayledgerTenant.isSet()).isFalse();
    }

    @Test
    void requireReturnsWhenSet() {
        PayledgerTenant.set("acme");
        assertThat(PayledgerTenant.require()).isEqualTo("acme");
    }

    @Test
    void requireThrowsWhenUnset() {
        PayledgerTenant.clear();
        assertThatThrownBy(PayledgerTenant::require).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void runWithRestoresPrevious() {
        PayledgerTenant.set("outer");
        PayledgerTenant.runWith(
                "inner", () -> assertThat(PayledgerTenant.current()).isEqualTo("inner"));
        assertThat(PayledgerTenant.current()).isEqualTo("outer");
    }

    @Test
    void runWithRestoresToNullWhenNoneBefore() {
        PayledgerTenant.clear();
        PayledgerTenant.runWith(
                "inner", () -> assertThat(PayledgerTenant.current()).isEqualTo("inner"));
        assertThat(PayledgerTenant.isSet()).isFalse();
    }

    @Test
    void runWithRestoresEvenOnException() {
        PayledgerTenant.set("keep");
        assertThatThrownBy(
                        () ->
                                PayledgerTenant.runWith(
                                        "temp",
                                        () -> {
                                            throw new IllegalStateException("boom");
                                        }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(PayledgerTenant.current()).isEqualTo("keep");
    }

    @Test
    void withReturnsValueAndRestores() throws Exception {
        String result = PayledgerTenant.with("acme", () -> "v-" + PayledgerTenant.current());
        assertThat(result).isEqualTo("v-acme");
        assertThat(PayledgerTenant.isSet()).isFalse();
    }

    @Test
    void childThreadInherits() throws Exception {
        PayledgerTenant.set("parent");
        String[] seen = new String[1];
        Thread child = new Thread(() -> seen[0] = PayledgerTenant.current());
        child.start();
        child.join(2000);
        assertThat(seen[0]).isEqualTo("parent");
    }
}
