/*
 * Copyright (c) 2017 Dop Sun. All rights reserved.
 */

package com.dopsun.mimodispatcher;

import java.util.List;
import java.util.function.Function;

import javax.annotation.concurrent.ThreadSafe;

/**
 * A thread safe task synchronizer resolver.
 * 
 * @author Dop Sun
 * @param <T>
 * @since 1.0.0
 */
@ThreadSafe
@FunctionalInterface
public interface TaskSynchronizerResolver<T> extends Function<T, List<Object>> {
}
