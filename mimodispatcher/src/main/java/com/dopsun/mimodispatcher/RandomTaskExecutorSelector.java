/*
 * Copyright (c) 2017 Dop Sun. All rights reserved.
 */

package com.dopsun.mimodispatcher;

import java.util.Objects;
import java.util.Random;

/**
 * @author Dop Sun
 * @since 1.0.0
 */
public final class RandomTaskExecutorSelector implements TaskExecutorSelector {
    private final Random random;

    /**
     * 
     */
    public RandomTaskExecutorSelector() {
        this(new Random());
    }

    /**
     * @param random
     */
    public RandomTaskExecutorSelector(Random random) {
        Objects.requireNonNull(random);

        this.random = random;
    }

    @Override
    public int apply(TaskExecutorSelectorContext context) {
        int executorCount = context.getExecutorCount();

        int randomId = random.nextInt(executorCount);
        for (int i = randomId; i < executorCount; i++) {
            if (context.getExecutorTaskCount(i) > context.getExecutorQueueMaxTaskSize()) {
                continue;
            }
            if (context.getExecutorSynchronizerCount(i) > context.getExecutorQueueMaxTaskSize()) {
                continue;
            }

            return i;
        }

        for (int i = randomId - 1; i >= 0; i--) {
            if (context.getExecutorTaskCount(i) > context.getExecutorQueueMaxTaskSize()) {
                continue;
            }
            if (context.getExecutorSynchronizerCount(i) > context.getExecutorQueueMaxTaskSize()) {
                continue;
            }

            return i;
        }

        return -1;
    }

}
