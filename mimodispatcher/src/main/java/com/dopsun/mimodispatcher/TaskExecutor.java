/*
 * Copyright (c) 2017 Dop Sun. All rights reserved.
 */

package com.dopsun.mimodispatcher;

import java.util.function.Consumer;

/**
 * @param <T>
 *            task
 * @author Dop Sun
 * @since 1.0.0
 */
@FunctionalInterface
public interface TaskExecutor<T> extends Consumer<T> {

}
