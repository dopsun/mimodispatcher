/*
 * Copyright (c) 2017 Dop Sun. All rights reserved.
 */

package com.dopsun.mimodispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @param <T>
 *            type of the task.
 * @author Dop Sun
 * @since 1.0.0
 */
@ThreadSafe
public final class MimoDispatcher<T> implements AutoCloseable {
    /**
     * @return a new builder for {@link MimoDispatcher}.
     */
    public static <T> Builder<T> builder() {
        return new Builder<T>();
    }

    private final MimoDispatcherContext context;
    private final MimoDispatcherStatsImpl stats;

    private final int blockingQueueMaxTaskSize;
    private final int blockingQueueMaxSynchronizerSize;

    private final int executorQueueMaxTaskSize;
    private final int executorQueueMaxSynchronizerSize;

    private final TaskExecutor<T> taskExecutor;
    private final TaskSynchronizerResolver<T> taskSynchronizerResolver;
    private final TaskExecutorSelector taskExecutorSelector;

    private final DispatcherThread<T> dispatcherThread;
    private final List<ExecutorThread<T>> executorThreadList;

    /**
     * @param builder
     */
    private MimoDispatcher(Builder<T> builder) {
        Objects.requireNonNull(builder);

        if (builder.taskExecutor == null) {
            throw new IllegalArgumentException("taskExecutor is null.");
        }
        if (builder.numOfExecutors <= 0) {
            throw new IllegalArgumentException(
                    "numOfExecutors is invalid: " + builder.numOfExecutors);
        }

        this.taskExecutor = builder.taskExecutor;

        if (builder.taskSynchronizerResolver == null) {
            this.taskSynchronizerResolver = t -> Collections.emptyList();
        } else {
            this.taskSynchronizerResolver = builder.taskSynchronizerResolver;
        }

        if (builder.taskExecutorSelector == null) {
            this.taskExecutorSelector = new RandomTaskExecutorSelector();
        } else {
            this.taskExecutorSelector = builder.taskExecutorSelector;
        }

        this.blockingQueueMaxTaskSize = builder.blockingQueueMaxTaskSize;
        this.blockingQueueMaxSynchronizerSize = builder.blockingQueueMaxSynchronizerSize;
        this.executorQueueMaxTaskSize = builder.executorQueueMaxTaskSize;
        this.executorQueueMaxSynchronizerSize = builder.executorQueueMaxSynchronizerSize;

        this.context = new MimoDispatcherContext();
        this.stats = new MimoDispatcherStatsImpl();

        this.dispatcherThread = new DispatcherThread<>(this.context);
        this.executorThreadList = new ArrayList<>(builder.numOfExecutors);
        for (int i = 0; i < builder.numOfExecutors; i++) {
            this.executorThreadList.add(new ExecutorThread<>(this.context, i));
        }
    }

    @Override
    public void close() throws Exception {
        this.stats.close();
        this.dispatcherThread.close();

        for (ExecutorThread<T> thread : executorThreadList) {
            thread.close();
        }
    }

    /**
     * @return
     */
    public MimoDispatcherStats getStats() {
        return stats;
    }

    /**
     * Puts a task from any thread into this dispatcher, waiting if necessary for space to become
     * available.
     * 
     * @param task
     *            a task to be dispatched.
     * @throws InterruptedException
     *             if interrupted while waiting
     */
    public void put(T task) throws InterruptedException {
        Objects.requireNonNull(task);

        this.dispatcherThread.put(task);
    }

    final class MimoDispatcherStatsImpl implements MimoDispatcherStats, AutoCloseable {
        private final AtomicInteger blockingQueueLastSize = new AtomicInteger();
        private final AtomicInteger blockingQueueMaxSize = new AtomicInteger();
        private final AtomicInteger executorQueueMaxSize = new AtomicInteger();

        private final ConcurrentHashMap<ExecutorThread<T>, Integer> executorQueueSizes;

        public MimoDispatcherStatsImpl() {
            this.executorQueueSizes = new ConcurrentHashMap<>();
        }

        @Override
        public void close() throws Exception {
        }

        public void notifyBlockingQueueSizeChanged(int newSize) {
            blockingQueueLastSize.set(newSize);
            blockingQueueMaxSize.getAndUpdate(old -> {
                return Math.max(old, newSize);
            });
        }

        public void notifyExecutorQueueSizeChanged(ExecutorThread<T> executor, int newSize) {
            executorQueueMaxSize.getAndUpdate(old -> {
                return Math.max(old, newSize);
            });

            executorQueueSizes.put(executor, newSize);
        }

        @Override
        public int getBlockingQueueLastSize() {
            return blockingQueueLastSize.get();
        }

        @Override
        public int getBlockingQueueMaxSize() {
            return blockingQueueMaxSize.get();
        }

        @Override
        public int getExecutorQueueMaxSize() {
            return executorQueueMaxSize.get();
        }

        @Override
        public int getExecutorQueueMaxSizeDifference() {
            int max = 0;
            int min = 0;

            for (int size : executorQueueSizes.values()) {
                max = Math.max(max, size);
                min = Math.min(min, size);
            }

            return max - min;
        }
    }

    final class MimoDispatcherContext implements DispatcherThreadContext<T>,
            ExecutorThreadContext<T>, TaskExecutorSelectorContext {
        private final AtomicBoolean blockingQueueDispatching = new AtomicBoolean(true);

        @Override
        public int getBlockingQueueMaxSynchronizerSize() {
            return MimoDispatcher.this.blockingQueueMaxSynchronizerSize;
        }

        @Override
        public int getBlockingQueueMaxTaskSize() {
            return MimoDispatcher.this.blockingQueueMaxTaskSize;
        }

        @Override
        public int getExecutorQueueMaxSynchronizerSize() {
            return MimoDispatcher.this.executorQueueMaxSynchronizerSize;
        }

        @Override
        public int getExecutorQueueMaxTaskSize() {
            return MimoDispatcher.this.executorQueueMaxTaskSize;
        }

        public void notifyBlockingQueueSizeChanged(int newSize) {
            MimoDispatcher.this.stats.notifyBlockingQueueSizeChanged(newSize);
        }

        public void notifyExecutorQueueSizeChanged(ExecutorThread<T> executor, int newSize) {
            MimoDispatcher.this.stats.notifyExecutorQueueSizeChanged(executor, newSize);
        }

        @Override
        public TaskSynchronizerResolver<T> getTaskSynchronizerResolver() {
            return MimoDispatcher.this.taskSynchronizerResolver;
        }

        @Override
        public TaskExecutor<T> getTaskExecutor() {
            return MimoDispatcher.this.taskExecutor;
        }

        @Override
        public boolean getAndResetBlockingQueueDispatching() {
            return blockingQueueDispatching.getAndSet(false);
        }

        @Override
        public void notifyTaskSynchronizerReleased(ExecutorThread<T> executorThread,
                Object taskSynchronizer) {
        }

        @Override
        public void notifyTaskExecutorException(ExecutorThread<T> executorThread, Throwable cause) {
            blockingQueueDispatching.set(true);
        }

        private DispatchResult putToExecutor(T task) throws InterruptedException {
            int executorId = taskExecutorSelector.apply(this);
            if (executorId < 0) {
                return DispatchResult.ALL_EXECUTORS_BUSY;
            }

            executorThreadList.get(executorId).put(task);

            return DispatchResult.OK;
        }

        private DispatchResult putToExecutor(T task, Object synchronizer)
                throws InterruptedException {
            Objects.requireNonNull(task);
            Objects.requireNonNull(synchronizer);

            for (ExecutorThread<T> et : executorThreadList) {
                if (et.hasSynchronizer(synchronizer)) {
                    et.put(task);
                    return DispatchResult.OK;
                }
            }

            return putToExecutor(task);
        }

        @Override
        public DispatchResult putToExecutor(T task, List<Object> synchronizers)
                throws InterruptedException {
            Objects.requireNonNull(task);
            Objects.requireNonNull(synchronizers);

            if (synchronizers.size() == 0) {
                return this.putToExecutor(task);
            }

            if (synchronizers.size() == 1) {
                return this.putToExecutor(task, synchronizers.get(0));
            }

            boolean blocked = false;
            ExecutorThread<T> selectedThread = null;
            for (Object sync : synchronizers) {
                for (ExecutorThread<T> et : executorThreadList) {
                    if (!et.hasSynchronizer(sync)) {
                        continue;
                    }

                    if (selectedThread == null) {
                        selectedThread = et;
                        continue;
                    }

                    if (et != selectedThread) {
                        blocked = true;
                        break;
                    }
                }
            }

            if (blocked) {
                return DispatchResult.BLOCKED;
            }

            if (selectedThread != null) {
                if (selectedThread.getQueueSize() >= getExecutorQueueMaxTaskSize()) {
                    return DispatchResult.EXECUTOR_SELECTED_BUSY;
                }
                if (selectedThread
                        .getSynchronizerCount() >= getExecutorQueueMaxSynchronizerSize()) {
                    return DispatchResult.EXECUTOR_SELECTED_BUSY;
                }

                selectedThread.put(task);

                return DispatchResult.OK;
            }

            return putToExecutor(task);
        }

        @Override
        public int getExecutorCount() {
            return MimoDispatcher.this.executorThreadList.size();
        }

        @Override
        public int getExecutorTaskCount(int executorIndex) {
            return MimoDispatcher.this.executorThreadList.get(executorIndex).getQueueSize();
        }

        @Override
        public int getExecutorSynchronizerCount(int executorIndex) {
            return MimoDispatcher.this.executorThreadList.get(executorIndex).getSynchronizerCount();
        }
    }

    /**
     * Builder for {@link MimoDispatcher}.
     * 
     * @param <T>
     * @author Dop Sun
     * @since 1.0.0
     */
    public static class Builder<T> {
        @Nullable
        private TaskSynchronizerResolver<T> taskSynchronizerResolver;

        private TaskExecutor<T> taskExecutor;

        private TaskExecutorSelector taskExecutorSelector;

        private int numOfExecutors = Runtime.getRuntime().availableProcessors() * 2;

        private int blockingQueueMaxTaskSize = Integer.MAX_VALUE;
        private int blockingQueueMaxSynchronizerSize = Integer.MAX_VALUE;

        private int executorQueueMaxTaskSize = Integer.MAX_VALUE;
        private int executorQueueMaxSynchronizerSize = Integer.MAX_VALUE;

        /**
         * @return a new instance of {@link MimoDispatcher}.
         */
        public MimoDispatcher<T> build() {
            return new MimoDispatcher<>(this);
        }

        /**
         * @return the taskSynchronizerResolver
         */
        public TaskSynchronizerResolver<T> getTaskSynchronizerResolver() {
            return taskSynchronizerResolver;
        }

        /**
         * @param taskSynchronizerResolver
         *            the taskSynchronizerResolver to set
         * @return
         */
        public Builder<T> setTaskSynchronizerResolver(
                TaskSynchronizerResolver<T> taskSynchronizerResolver) {
            this.taskSynchronizerResolver = taskSynchronizerResolver;

            return this;
        }

        /**
         * @return the taskExecutor
         */
        public TaskExecutor<T> getTaskExecutor() {
            return taskExecutor;
        }

        /**
         * @param taskExecutor
         *            the taskExecutor to set
         * @return
         */
        public Builder<T> setTaskExecutor(TaskExecutor<T> taskExecutor) {
            this.taskExecutor = taskExecutor;

            return this;
        }

        /**
         * @return the numOfExecutors
         */
        public int getNumOfExecutors() {
            return numOfExecutors;
        }

        /**
         * @param numOfExecutors
         *            the numOfExecutors to set
         * @return
         */
        public Builder<T> setNumOfExecutors(int numOfExecutors) {
            this.numOfExecutors = numOfExecutors;
            return this;
        }

        /**
         * @return the blockingQueueMaxTaskSize
         */
        public int getBlockingQueueMaxTaskSize() {
            return blockingQueueMaxTaskSize;
        }

        /**
         * @param blockingQueueMaxTaskSize
         *            the blockingQueueMaxTaskSize to set
         * @return
         */
        public Builder<T> setBlockingQueueMaxTaskSize(int blockingQueueMaxTaskSize) {
            this.blockingQueueMaxTaskSize = blockingQueueMaxTaskSize;
            return this;
        }

        /**
         * @return the blockingQueueMaxSynchronizerSize
         */
        public int getBlockingQueueMaxSynchronizerSize() {
            return blockingQueueMaxSynchronizerSize;
        }

        /**
         * @param blockingQueueMaxSynchronizerSize
         *            the blockingQueueMaxSynchronizerSize to set
         * @return
         */
        public Builder<T> setBlockingQueueMaxSynchronzerSize(int blockingQueueMaxSynchronizerSize) {
            this.blockingQueueMaxSynchronizerSize = blockingQueueMaxSynchronizerSize;
            return this;
        }

        /**
         * @return the executorQueueMaxTaskSize
         */
        public int getExecutorQueueMaxTaskSize() {
            return executorQueueMaxTaskSize;
        }

        /**
         * @param executorQueueMaxTaskSize
         *            the executorQueueMaxTaskSize to set
         * @return
         */
        public Builder<T> setExecutorQueueMaxTaskSize(int executorQueueMaxTaskSize) {
            this.executorQueueMaxTaskSize = executorQueueMaxTaskSize;
            return this;
        }

        /**
         * @return the executorQueueMaxSynchronizerSize
         */
        public int getExecutorQueueMaxSynchronizerSize() {
            return executorQueueMaxSynchronizerSize;
        }

        /**
         * @param executorQueueMaxSynchronizerSize
         *            the executorQueueMaxSynchronizerSize to set
         * @return
         */
        public Builder<T> setExecutorQueueMaxSynchronizerSize(
                int executorQueueMaxSynchronizerSize) {
            this.executorQueueMaxSynchronizerSize = executorQueueMaxSynchronizerSize;
            return this;
        }

        /**
         * @return the taskExecutorSelector
         */
        public TaskExecutorSelector getTaskExecutorSelector() {
            return taskExecutorSelector;
        }

        /**
         * @param taskExecutorSelector
         *            the taskExecutorSelector to set
         * @return
         */
        public Builder<T> setTaskExecutorSelector(TaskExecutorSelector taskExecutorSelector) {
            this.taskExecutorSelector = taskExecutorSelector;
            return this;
        }
    }
}
