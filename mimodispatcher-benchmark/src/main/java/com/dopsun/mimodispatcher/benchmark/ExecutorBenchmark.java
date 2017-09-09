/*
 * Copyright (c) 2017 Dop Sun. All rights reserved.
 */

package com.dopsun.mimodispatcher.benchmark;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/**
 * @author Dop Sun
 * @since 1.0.0
 */
@SuppressWarnings("javadoc")
public class ExecutorBenchmark {
    private static final int MAX_WAIT_TIME = 3;
    private static final int TOTAL_SIZE = 50;

    private static void sleepInExecutor(ExecutorService executor, int timeoutInMillis)
            throws InterruptedException {
        if (timeoutInMillis < 0) {
            return;
        }

        if (timeoutInMillis == 0) {
            Thread.sleep(0);
        } else if (timeoutInMillis == 1) {
            Thread.sleep(1);
        } else {
            for (int i = 0; i < timeoutInMillis; i++) {
                if (executor.isTerminated() || executor.isShutdown()) {
                    return;
                }

                Thread.sleep(1);
            }
        }
    }

    @State(Scope.Thread)
    public static class ExecutorBenchmarkState {
        private CountDownLatch latch = new CountDownLatch(TOTAL_SIZE);
        private AtomicInteger nextTaskId = new AtomicInteger(0);
        private ExecutorService executor;

        @Setup(Level.Iteration)
        public void setUp() {
            this.executor = Executors.newSingleThreadExecutor();
        }

        @TearDown(Level.Iteration)
        public void tearDown() throws Exception {
            this.executor.shutdown();
        }

        public void reset() {
            latch = new CountDownLatch(TOTAL_SIZE);
            nextTaskId = new AtomicInteger(0);
        }
    }

    @Benchmark
    public void SingleInputSingleOutputBenchmark(ExecutorBenchmarkState state) throws Exception {
        state.reset();

        for (int i = 0; i < TOTAL_SIZE; i++) {
            final int taskId = i;
            state.executor.execute(() -> {
                if (state.nextTaskId.compareAndSet(taskId, taskId + 1)) {
                    state.latch.countDown();
                }
            });
        }

        boolean succeed = state.latch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);

        if (!succeed) {
            throw new RuntimeException("Failed to wait for completed.");
        }
    }

    @Benchmark
    public void SingleInputSingleOutputBenchmarkWithSleep0(ExecutorBenchmarkState state)
            throws Exception {
        state.reset();

        for (int i = 0; i < TOTAL_SIZE; i++) {
            final int taskId = i;
            state.executor.execute(() -> {
                try {
                    sleepInExecutor(state.executor, 0);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (state.nextTaskId.compareAndSet(taskId, taskId + 1)) {
                    state.latch.countDown();
                }
            });
        }

        boolean succeed = state.latch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);

        if (!succeed) {
            throw new RuntimeException("Failed to wait for completed.");
        }
    }

    @Benchmark
    public void SingleInputSingleOutputBenchmarkWithSleep1(ExecutorBenchmarkState state)
            throws Exception {
        state.reset();

        for (int i = 0; i < TOTAL_SIZE; i++) {
            final int taskId = i;
            state.executor.execute(() -> {
                try {
                    sleepInExecutor(state.executor, 1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (state.nextTaskId.compareAndSet(taskId, taskId + 1)) {
                    state.latch.countDown();
                }
            });
        }

        boolean succeed = state.latch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);

        if (!succeed) {
            throw new RuntimeException("Failed to wait for completed.");
        }
    }

    @Benchmark
    public void SingleInputSingleOutputBenchmarkWithSleep10(ExecutorBenchmarkState state)
            throws Exception {
        state.reset();

        for (int i = 0; i < TOTAL_SIZE; i++) {
            final int taskId = i;
            state.executor.execute(() -> {
                try {
                    sleepInExecutor(state.executor, 10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (state.nextTaskId.compareAndSet(taskId, taskId + 1)) {
                    state.latch.countDown();
                }
            });
        }

        boolean succeed = state.latch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);

        if (!succeed) {
            throw new RuntimeException("Failed to wait for completed.");
        }
    }

}