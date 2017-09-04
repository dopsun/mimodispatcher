/*
 * Copyright (c) 2017 Dop Sun. All rights reserved.
 */

package com.dopsun.mimodispatcher;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.dopsun.mimodispatcher.MimoDispatcher.Builder;

/**
 * @author Dop Sun
 * @since 1.0.0
 */
@SuppressWarnings("javadoc")
public class MimoDispatcherBasicTest {
    @Test
    public void whenSingleExecutorThenAllTasksExecutedInSequence() throws Exception {
        final int TOTAL_SIZE = 50;

        final CountDownLatch latch = new CountDownLatch(TOTAL_SIZE);
        final AtomicInteger nextTaskId = new AtomicInteger(0);

        /** @formatter:off */
        Builder<Integer> builder = MimoDispatcher.<Integer> builder()
                
                .setNumOfExecutors(1)
                
                .setTaskExecutor((taskId) -> {
                    if (nextTaskId.compareAndSet(taskId, taskId + 1)) {
                        latch.countDown();
                    }
                });
        /** @formatter:on */

        try (MimoDispatcher<Integer> dispatcher = builder.build()) {
            for (int i = 0; i < TOTAL_SIZE; i++) {
                dispatcher.put(i);
            }

            boolean succeed = latch.await(3, TimeUnit.SECONDS);

            assertTrue(succeed);
        }
    }

    @Test
    public void whenThreeExecutorsThenAllTasksExecutedSuccessfully() throws Exception {
        final int TOTAL_SIZE = 50;

        final CountDownLatch latch = new CountDownLatch(TOTAL_SIZE);

        /** @formatter:off */
        Builder<Integer> builder = MimoDispatcher.<Integer> builder()
                
                .setNumOfExecutors(3)
                
                .setTaskExecutor((taskId) -> {
                    latch.countDown();
                });
        /** @formatter:on */

        try (MimoDispatcher<Integer> dispatcher = builder.build()) {
            for (int i = 0; i < TOTAL_SIZE; i++) {
                dispatcher.put(i);
            }

            boolean succeed = latch.await(3, TimeUnit.SECONDS);

            assertTrue(succeed);
        }
    }
}
