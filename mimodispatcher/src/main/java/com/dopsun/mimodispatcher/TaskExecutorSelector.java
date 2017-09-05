/*
 * Copyright (c) 2017 Dop Sun. All rights reserved.
 */

package com.dopsun.mimodispatcher;

/**
 * Select an executor for a task.
 * 
 * @author Dop Sun
 * @since 1.0.0
 */
public interface TaskExecutorSelector {
    /**
     * @param context
     * @return executor id if present. -1 if not found.
     */
    int apply(TaskExecutorSelectorContext context);
}
