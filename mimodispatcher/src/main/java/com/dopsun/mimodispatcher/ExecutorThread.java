/*
 * Copyright (c) 2017 Dop Sun. All rights reserved.
 */

package com.dopsun.mimodispatcher;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Service;

/**
 * @author Dop Sun
 * @param <T>
 * @since 1.0.0
 */
@ThreadSafe
class ExecutorThread<T> implements AutoCloseable {
    private final ExecutorThreadContext<T> context;
    private final String executorName;

    private final TransferQueue<T> taskQueue;

    private final ConcurrentHashMap<Object, Long> taskSynchronizers;

    private final Service executorService;

    /**
     * @param context
     * @param index
     */
    public ExecutorThread(ExecutorThreadContext<T> context, int index) {
        Objects.requireNonNull(context);

        this.context = context;
        this.executorName = "Executor-" + index;

        this.taskQueue = new LinkedTransferQueue<>();
        this.taskSynchronizers = new ConcurrentHashMap<>();

        this.executorService = new AbstractExecutionThreadService() {
            @Override
            protected String serviceName() {
                return executorName;
            }

            @Override
            protected void run() throws Exception {
                while (this.isRunning()) {
                    tryExecuteOnce(100, TimeUnit.MILLISECONDS);

                    context.notifyExecutorQueueSizeChanged(ExecutorThread.this, taskQueue.size());
                }
            }
        };

        this.executorService.startAsync().awaitRunning();
    }

    @Override
    public void close() throws Exception {
        this.executorService.stopAsync().awaitTerminated();
    }

    /**
     * @return
     */
    public int getQueueSize() {
        return taskQueue.size();
    }

    /**
     * @return
     */
    public int getSynchronizerCount() {
        return taskSynchronizers.size();
    }

    /**
     * @param synchronizer
     * @return
     */
    public boolean hasSynchronizer(Object synchronizer) {
        Long counter = taskSynchronizers.get(synchronizer);

        return counter != null && counter.longValue() > 0;
    }

    /**
     * @param task
     * @throws InterruptedException
     */
    public void put(T task) throws InterruptedException {
        Objects.requireNonNull(task);

        List<Object> synchronizers = context.getTaskSynchronizerResolver().apply(task);
        for (Object sync : synchronizers) {
            addOrIncrementTaskSynchronizerCounter(sync);
        }

        try {
            taskQueue.put(task);

            context.notifyExecutorQueueSizeChanged(this, taskQueue.size());
        } catch (InterruptedException e) {
            // Undo the synchronizer counter while put is interrupted.
            for (Object taskSynchronizer : synchronizers) {
                decrementTaskSynchronizerCounter(taskSynchronizer);
            }

            throw e;
        }
    }

    /**
     * @param timeout
     * @param unit
     * @return <code>true</code> if one task executed.
     * @throws InterruptedException
     */
    public boolean tryExecuteOnce(long timeout, TimeUnit unit) throws InterruptedException {
        T task = this.taskQueue.poll(timeout, unit);
        if (task == null) {
            return false;
        }

        try {
            context.getTaskExecutor().accept(task);
        } catch (Throwable e) {
            this.context.notifyTaskExecutorException(this, e);
        }

        List<Object> synchronizers = context.getTaskSynchronizerResolver().apply(task);
        for (Object taskSynchronizer : synchronizers) {
            long newCounter = decrementTaskSynchronizerCounter(taskSynchronizer);

            if (newCounter == 0) {
                context.notifyTaskSynchronizerReleased(this, taskSynchronizer);
            }
        }

        return true;
    }

    private void addOrIncrementTaskSynchronizerCounter(Object synchronizer) {
        taskSynchronizers.compute(synchronizer, (sync, counter) -> {
            if (counter == null) {
                return 1L;
            } else {
                return counter + 1;
            }
        });
    }

    private long decrementTaskSynchronizerCounter(Object synchronizer) {
        Long newCounter = taskSynchronizers.computeIfPresent(synchronizer, (sync, counter) -> {
            if (counter > 0) {
                return counter - 1;
            }

            return 0L;
        });

        return newCounter.longValue();
    }

}
