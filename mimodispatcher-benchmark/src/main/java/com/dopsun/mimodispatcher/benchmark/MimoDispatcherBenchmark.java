/*
 * Copyright (c) 2017 Dop Sun. All rights reserved.
 */

package com.dopsun.mimodispatcher.benchmark;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.dopsun.mimodispatcher.MimoDispatcher;
import com.dopsun.mimodispatcher.MimoDispatcher.Builder;

/**
 * @author Dop Sun
 * @since 1.0.0
 */
@SuppressWarnings("javadoc")
public class MimoDispatcherBenchmark {
    private static final int MAX_WAIT_TIME = 3;
    private static final int TOTAL_SIZE = 50;

    private static void sleepInDispatcher(int timeoutInMillis) throws InterruptedException {
        if (timeoutInMillis < 0) {
            return;
        }

        if (timeoutInMillis == 0) {
            Thread.sleep(0);
        } else if (timeoutInMillis == 1) {
            Thread.sleep(1);
        } else {
            for (int i = 0; i < timeoutInMillis; i++) {
                Thread.sleep(1);
            }
        }
    }

    @State(Scope.Thread)
    public static class MimoDispatcherBenchmarkState {
        private CountDownLatch latch = new CountDownLatch(TOTAL_SIZE);
        private AtomicInteger nextTaskId = new AtomicInteger(0);
        private MimoDispatcher<Integer> dispatcher;

        private int timeoutInMillis = 0;

        @Setup(Level.Iteration)
        public void setUp() {
            latch = new CountDownLatch(TOTAL_SIZE);
            nextTaskId = new AtomicInteger(0);

            /** @formatter:off */
            Builder<Integer> builder = MimoDispatcher.<Integer> builder()
                    
                    .setNumOfExecutors(1)
                    
                    .setTaskExecutor((taskId) -> {
                        try {
                            sleepInDispatcher(timeoutInMillis);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        
                        if (nextTaskId.compareAndSet(taskId, taskId + 1)) {
                            latch.countDown();
                        }
                    });
            /** @formatter:on */

            this.dispatcher = builder.build();
        }

        @TearDown(Level.Iteration)
        public void tearDown() throws Exception {
            this.dispatcher.close();
        }

        public void reset(int timeoutInMillis) {
            this.timeoutInMillis = timeoutInMillis;
            latch = new CountDownLatch(TOTAL_SIZE);
            nextTaskId = new AtomicInteger(0);
        }
    }

    @Benchmark
    public void SingleInputSingleOutputBenchmark(MimoDispatcherBenchmarkState state)
            throws Exception {
        state.reset(-1);

        for (int i = 0; i < TOTAL_SIZE; i++) {
            state.dispatcher.put(i);
        }

        boolean succeed = state.latch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);

        if (!succeed) {
            throw new RuntimeException("Failed to wait for completed.");
        }
    }

    @Benchmark
    public void SingleInputSingleOutputBenchmarkWithSleep0(MimoDispatcherBenchmarkState state)
            throws Exception {
        state.reset(0);

        for (int i = 0; i < TOTAL_SIZE; i++) {
            state.dispatcher.put(i);
        }

        boolean succeed = state.latch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);

        if (!succeed) {
            throw new RuntimeException("Failed to wait for completed.");
        }
    }

    @Benchmark
    public void SingleInputSingleOutputBenchmarkWithSleep1(MimoDispatcherBenchmarkState state)
            throws Exception {
        state.reset(1);

        for (int i = 0; i < TOTAL_SIZE; i++) {
            state.dispatcher.put(i);
        }

        boolean succeed = state.latch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);

        if (!succeed) {
            throw new RuntimeException("Failed to wait for completed.");
        }
    }

    @Benchmark
    public void SingleInputSingleOutputBenchmarkWithSleep10(MimoDispatcherBenchmarkState state)
            throws Exception {
        state.reset(10);

        for (int i = 0; i < TOTAL_SIZE; i++) {
            state.dispatcher.put(i);
        }

        boolean succeed = state.latch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);

        if (!succeed) {
            throw new RuntimeException("Failed to wait for completed.");
        }
    }

}