/*
 * Copyright (c) 2017 Dop Sun. All rights reserved.
 */

package com.dopsun.mimodispatcher;

import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Dop Sun
 * @since 1.0.0
 */
@ThreadSafe
public interface MimoDispatcherStats {
    /**
     * @return the maximum size reached.
     */
    int getBlockingQueueMaxSize();

    /**
     * @return the maximum size of single executor queue.
     */
    int getExecutorQueueMaxSize();

    /**
     * @return the difference of queue sizes across all executors.
     */
    int getExecutorQueueMaxSizeDifference();
}
