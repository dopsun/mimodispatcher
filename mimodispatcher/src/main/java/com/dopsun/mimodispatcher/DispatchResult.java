/*
 * Copyright (c) 2017 Dop Sun. All rights reserved.
 */

package com.dopsun.mimodispatcher;

/**
 * @author Dop Sun
 * @since 1.0.0
 */
enum DispatchResult {
    /**
     * Delivered.
     */
    OK,

    /**
     * Blocked.
     */
    BLOCKED,

    /**
     * All executors are busy.
     */
    ALL_EXECUTORS_BUSY,

    /**
     * Selected executor busy.
     */
    EXECUTOR_SELECTED_BUSY
}
