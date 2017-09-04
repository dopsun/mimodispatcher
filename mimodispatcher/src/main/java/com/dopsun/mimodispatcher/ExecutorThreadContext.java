/*
 * Copyright (c) 2017 Dop Sun. All rights reserved.
 */

package com.dopsun.mimodispatcher;

/**
 * @author Dop Sun
 * @param <T>
 * @since 1.0.0
 */
interface ExecutorThreadContext<T> {
    /**
     * @return
     */
    TaskSynchronizerResolver<T> getTaskSynchronizerResolver();

    /**
     * @return
     */
    TaskExecutor<T> getTaskExecutor();

    /**
     * @param executor
     * @param newSize
     */
    void notifyExecutorQueueSizeChanged(ExecutorThread<T> executor, int newSize);

    /**
     * @param executorThread
     * @param taskSynchronizer
     */
    void notifyTaskSynchronizerReleased(ExecutorThread<T> executorThread, Object taskSynchronizer);

    /**
     * @param executorThread
     * @param cause
     */
    void notifyTaskExecutorException(ExecutorThread<T> executorThread, Throwable cause);
}
