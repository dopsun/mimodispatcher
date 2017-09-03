/*
 * Copyright (c) 2017 Dop Sun. All rights reserved.
 */

package com.dopsun.mimodispatcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.dopsun.mimodispatcher.MimoDispatcher.Builder;
import com.google.common.collect.ImmutableList;

/**
 * @author Dop Sun
 * @since 1.0.0
 */
@SuppressWarnings("javadoc")
public class MimoDispatcherSingleLockTest {
    @Test
    public void oneExecutor_OneOrTwoSynchronizer() throws Exception {
        final int TOTAL_SIZE = 50;

        final int LOCK_COUNT = 10;
        final int EXTRA_LOCK = 101;

        final Random random = new Random();
        final Map<Integer, List<Object>> syncMap = new HashMap<>();
        for (int i = 0; i < TOTAL_SIZE; i++) {
            boolean extraLock = random.nextBoolean();
            if (extraLock) {
                syncMap.put(i, ImmutableList.of(random.nextInt(LOCK_COUNT)));
            } else {
                syncMap.put(i, ImmutableList.of(random.nextInt(LOCK_COUNT), EXTRA_LOCK));
            }
        }

        final CountDownLatch latch = new CountDownLatch(TOTAL_SIZE);
        final AtomicInteger nextTaskId = new AtomicInteger(0);

        /** @formatter:off */
        Builder<Integer> builder = MimoDispatcher.<Integer> builder()
                
                .setNumOfExecutors(1)
                
                .setTaskSynchronizerResolver(t -> {
                    return syncMap.get(t);
                })
                
                .setTaskExecutor((taskId) -> {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    
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

            assertEquals(0, dispatcher.getStats().getBlockingQueueMaxSize());
        }
    }

    //@Test
    public void twoExecutors_OneOrTwoSynchronizer() throws Exception {
        //TODO: to make this pass.
        
        final int TOTAL_SIZE = 50;

        final int LOCK_COUNT = 10;
        final int EXTRA_LOCK = 101;

        final Random random = new Random();
        final Map<Integer, List<Object>> syncMap = new HashMap<>();
        for (int i = 0; i < TOTAL_SIZE; i++) {
            boolean extraLock = random.nextBoolean();
            if (extraLock) {
                syncMap.put(i, ImmutableList.of(random.nextInt(LOCK_COUNT)));
            } else {
                syncMap.put(i, ImmutableList.of(random.nextInt(LOCK_COUNT), EXTRA_LOCK));
            }
        }

        final CountDownLatch latch = new CountDownLatch(TOTAL_SIZE);

        /** @formatter:off */
        Builder<Integer> builder = MimoDispatcher.<Integer> builder()
                
                .setNumOfExecutors(3)
                
                .setTaskSynchronizerResolver(t -> {
                    return syncMap.get(t);
                })
                
                .setTaskExecutor((taskId) -> {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    
                    latch.countDown();
                    
                    System.out.println("Count: " + latch.getCount() + ", TaskId: " + taskId);
                });
        /** @formatter:on */

        try (MimoDispatcher<Integer> dispatcher = builder.build()) {
            for (int i = 0; i < TOTAL_SIZE; i++) {
                dispatcher.put(i);
            }

            boolean succeed = latch.await(6, TimeUnit.SECONDS);
            assertTrue(succeed);

            assertEquals(0, dispatcher.getStats().getBlockingQueueMaxSize());
        }
    }
}
