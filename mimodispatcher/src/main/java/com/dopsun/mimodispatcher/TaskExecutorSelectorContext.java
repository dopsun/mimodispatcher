/*
 * Copyright (c) 2017 Dop Sun. All rights reserved.
 */

package com.dopsun.mimodispatcher;

/**
 * @author Dop Sun
 * @since 1.0.0
 */
public interface TaskExecutorSelectorContext {
    /**
     * @return executor count.
     */
    int getExecutorCount();

    /**
     * @param executorId
     * @return number of tasks current in the executor queue
     */
    int getExecutorTaskCount(int executorId);

    /**
     * @param executorId
     * @return number of tasks current in the executor queue
     */
    int getExecutorSynchronizerCount(int executorId);

    /**
     * @return the executorQueueMaxTaskSize
     */
    int getExecutorQueueMaxTaskSize();

    /**
     * @return the executorQueueMaxSynchronizerSize
     */
    int getExecutorQueueMaxSynchronizerSize();
}
