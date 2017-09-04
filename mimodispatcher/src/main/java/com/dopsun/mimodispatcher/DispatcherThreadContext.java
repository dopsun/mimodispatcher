/*
 * Copyright (c) 2017 Dop Sun. All rights reserved.
 */

package com.dopsun.mimodispatcher;

import java.util.List;

/**
 * @param <T>
 * @author Dop Sun
 * @since 1.0.0
 */
public interface DispatcherThreadContext<T> {
    /**
     * @return
     */
    TaskSynchronizerResolver<T> getTaskSynchronizerResolver();

    /**
     * @param newSize
     */
    void notifyBlockingQueueSizeChanged(int newSize);

    /**
     * @return <code>true</code> if blocking queue need to be checking.
     */
    boolean getAndResetBlockingQueueDispatching();

    /**
     * @param task
     * @param synchronizers
     * @return <code>true</code> if this synchronizers are not blocked by more than one executors.
     * @throws InterruptedException
     */
    boolean putToExecutor(T task, List<Object> synchronizers) throws InterruptedException;
}
