/*
 * Copyright (c) 2017 Dop Sun. All rights reserved.
 */

package com.dopsun.mimodispatcher.benchmark;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.dopsun.mimodispatcher.MimoDispatcher;
import com.dopsun.mimodispatcher.MimoDispatcher.Builder;
import com.google.common.collect.ImmutableList;

/**
 * @author Dop Sun
 * @since 1.0.0
 */
@SuppressWarnings("javadoc")
public class MimoDispatcherSingleLockMultipleExecutorsBenchmark {
    private static final int MAX_WAIT_TIME = 10;
    private static final int TOTAL_SIZE = 50;
    private static final int MAX_LOCK_COUNT = 10;
    private static final int EXECUTOR_COUNT = 3;

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

        private Random random;
        private Map<Integer, List<Object>> taskLocks;

        private CountDownLatch latch = new CountDownLatch(TOTAL_SIZE);
        private MimoDispatcher<Integer> dispatcher;

        private int timeoutInMillis = 0;

        @Setup(Level.Invocation)
        public void setUp() {
            random = new Random();
            taskLocks = new HashMap<>();
            for (int i = 0; i < TOTAL_SIZE; i++) {
                taskLocks.put(i, ImmutableList.of(random.nextInt(MAX_LOCK_COUNT)));
            }

            latch = new CountDownLatch(TOTAL_SIZE);

            /** @formatter:off */
            Builder<Integer> builder = MimoDispatcher.<Integer> builder()
                    
                    .setTaskSynchronizerResolver(taskId -> taskLocks.get(taskId) )
                    
                    .setNumOfExecutors(EXECUTOR_COUNT)
                    
                    .setTaskExecutor((taskId) -> {
                        try {
                            sleepInDispatcher(timeoutInMillis);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        
                        latch.countDown();
                    });
            /** @formatter:on */

            this.dispatcher = builder.build();
        }

        @TearDown(Level.Invocation)
        public void tearDown() throws Exception {
            this.dispatcher.close();
        }

        public void reset(int timeoutInMillis) {
            this.timeoutInMillis = timeoutInMillis;
            latch = new CountDownLatch(TOTAL_SIZE);
        }
    }

    @Benchmark
    public void NoSleepTaskBenchmark(MimoDispatcherBenchmarkState state) throws Exception {
        state.reset(-1);

        for (int i = 0; i < TOTAL_SIZE; i++) {
            state.dispatcher.put(i);
        }

        boolean succeed = state.latch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);

        if (!succeed) {
            System.out.println(state.latch.getCount());

            throw new RuntimeException("Failed to wait for completed.");
        }
    }

    @Benchmark
    public void Sleep0TaskBenchmark(MimoDispatcherBenchmarkState state) throws Exception {
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
    public void Sleep1TaskBenchmark(MimoDispatcherBenchmarkState state) throws Exception {
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
    public void Sleep10TaskBenchmark(MimoDispatcherBenchmarkState state) throws Exception {
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