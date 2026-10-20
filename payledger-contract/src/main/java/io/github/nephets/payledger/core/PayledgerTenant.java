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

import java.util.concurrent.Callable;

/**
 * 多租户上下文：基于 {@link InheritableThreadLocal} 的当前租户持有器。
 *
 * <p>提供 {@link #set}/{@link #current}/{@link #clear}/{@link #require} 等基础操作，以及 {@link
 * #runWith}/{@link #with} 的 try-with-resources 风格包装，确保子任务执行后恢复原上下文。所有账户的余额与分录都按当前租户隔离。
 */
public final class PayledgerTenant {

    private static final ThreadLocal<String> CURRENT = new InheritableThreadLocal<>();

    private PayledgerTenant() {}

    /** 设置当前线程的租户 id（覆盖已有值）。 */
    public static void set(String tenantId) {
        CURRENT.set(tenantId);
    }

    /** 返回当前线程的租户 id；未设置时为 {@code null}。 */
    public static String current() {
        return CURRENT.get();
    }

    /** 当前线程是否已设置租户。 */
    public static boolean isSet() {
        return CURRENT.get() != null;
    }

    /** 清除当前线程的租户（必须配对调用，避免线程池污染）。 */
    public static void clear() {
        CURRENT.remove();
    }

    /** 返回当前租户 id，未设置时抛 {@link IllegalStateException}。 */
    public static String require() {
        String tenantId = CURRENT.get();
        if (tenantId == null) {
            throw new IllegalStateException("当前线程未设置 payledger 租户");
        }
        return tenantId;
    }

    /** 在指定租户上下文中执行 {@link Runnable}，结束（含异常）后恢复原上下文。 */
    public static void runWith(String tenantId, Runnable task) {
        String previous = CURRENT.get();
        CURRENT.set(tenantId);
        try {
            task.run();
        } finally {
            restore(previous);
        }
    }

    /** 在指定租户上下文中执行 {@link Callable} 并返回其结果，结束（含异常）后恢复原上下文。 */
    public static <T> T with(String tenantId, Callable<T> task) throws Exception {
        String previous = CURRENT.get();
        CURRENT.set(tenantId);
        try {
            return task.call();
        } finally {
            restore(previous);
        }
    }

    private static void restore(String previous) {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }
}
