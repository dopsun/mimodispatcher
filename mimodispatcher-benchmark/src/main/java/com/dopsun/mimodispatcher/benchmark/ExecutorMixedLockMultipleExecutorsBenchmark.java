/*
 * Copyright (c) 2017 Dop Sun. All rights reserved.
 */

package com.dopsun.mimodispatcher.benchmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.google.common.collect.ImmutableList;

/**
 * @author Dop Sun
 * @since 1.0.0
 */
@SuppressWarnings("javadoc")
public class ExecutorMixedLockMultipleExecutorsBenchmark {
    private static final int MAX_WAIT_TIME = 3;
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
    public static class ExecutorBenchmarkState {
        private Random random;
        private List<Lock> allLocks;
        private Map<Integer, List<Lock>> taskLocks;

        private Lock extraLock;

        private CountDownLatch latch = new CountDownLatch(TOTAL_SIZE);
        private ExecutorService executor;

        private int timeoutInMillis = 0;

        private TransferQueue<Integer> taskQueue;

        /**
         * @param taskId
         * @return
         */
        public List<Lock> getLocks(int taskId) {
            return taskLocks.get(taskId);
        }

        @Setup(Level.Iteration)
        public void setUp() {
            random = new Random();
            allLocks = new ArrayList<>();
            for (int i = 0; i < MAX_LOCK_COUNT; i++) {
                allLocks.add(new ReentrantLock());
            }

            extraLock = new ReentrantLock();

            latch = new CountDownLatch(TOTAL_SIZE);

            taskQueue = new LinkedTransferQueue<>();

            taskLocks = new HashMap<>();
            for (int i = 0; i < TOTAL_SIZE; i++) {
                boolean hasExtraLock = random.nextBoolean();
                if (hasExtraLock) {
                    taskLocks.put(i, ImmutableList.of(extraLock,
                            allLocks.get(random.nextInt(MAX_LOCK_COUNT))));
                } else {
                    taskLocks.put(i,
                            ImmutableList.of(allLocks.get(random.nextInt(MAX_LOCK_COUNT))));
                }
            }

            this.executor = Executors.newFixedThreadPool(EXECUTOR_COUNT);

            for (int i = 0; i < EXECUTOR_COUNT; i++) {
                this.executor.submit(() -> {
                    while (!executor.isShutdown() && !executor.isTerminated()) {
                        try {
                            Integer taskId = taskQueue.poll(100, TimeUnit.MILLISECONDS);
                            runTask(taskId);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                });
            }
        }

        @TearDown(Level.Iteration)
        public void tearDown() throws Exception {
            this.executor.shutdown();
        }

        private void runTask(int taskId) {
            List<Lock> locks = taskLocks.get(taskId);
            for (Lock lock : locks) {
                lock.lock();
            }

            try {
                try {
                    sleepInDispatcher(timeoutInMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                latch.countDown();
            } finally {
                for (Lock lock : locks) {
                    lock.unlock();
                }
            }
        }

        public void reset(int timeoutInMillis) {
            this.timeoutInMillis = timeoutInMillis;
            latch = new CountDownLatch(TOTAL_SIZE);
        }

        public void putToQueue(int taskId) throws InterruptedException {
            if (!taskQueue.tryTransfer(taskId)) {
                taskQueue.put(taskId);
            }
        }
    }

    @Benchmark
    public void NoSleepTaskBenchmark(ExecutorBenchmarkState state) throws Exception {
        state.reset(-1);

        for (int i = 0; i < TOTAL_SIZE; i++) {
            state.putToQueue(i);
        }

        boolean succeed = state.latch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);

        if (!succeed) {
            throw new RuntimeException("Failed to wait for completed.");
        }
    }

    @Benchmark
    public void Sleep0TaskBenchmark(ExecutorBenchmarkState state) throws Exception {
        state.reset(0);

        for (int i = 0; i < TOTAL_SIZE; i++) {
            state.putToQueue(i);
        }

        boolean succeed = state.latch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);

        if (!succeed) {
            throw new RuntimeException("Failed to wait for completed.");
        }
    }

    @Benchmark
    public void Sleep1TaskBenchmark(ExecutorBenchmarkState state) throws Exception {
        state.reset(1);

        for (int i = 0; i < TOTAL_SIZE; i++) {
            state.putToQueue(i);
        }

        boolean succeed = state.latch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);

        if (!succeed) {
            throw new RuntimeException("Failed to wait for completed.");
        }
    }

    @Benchmark
    public void Sleep10TaskBenchmark(ExecutorBenchmarkState state) throws Exception {
        state.reset(10);

        for (int i = 0; i < TOTAL_SIZE; i++) {
            state.putToQueue(i);
        }

        boolean succeed = state.latch.await(MAX_WAIT_TIME, TimeUnit.SECONDS);

        if (!succeed) {
            throw new RuntimeException("Failed to wait for completed.");
        }
    }

}