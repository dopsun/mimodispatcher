/*
 * Copyright (c) 2017 Dop Sun. All rights reserved.
 */

package com.dopsun.mimodispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Service;

/**
 * @param <T>
 *            task
 * @author Dop Sun
 * @since 1.0.0
 */
public class DispatcherThread<T> implements AutoCloseable {
    private final DispatcherThreadContext<T> context;
    private final TransferQueue<T> taskQueue;

    private final List<T> blockedTaskQueue;
    private final Map<Object, Long> blockedTaskSynchronizers;

    private final Service dispatchService;

    /**
     * @param context
     */
    public DispatcherThread(DispatcherThreadContext<T> context) {
        Objects.requireNonNull(context);

        this.context = context;

        this.taskQueue = new LinkedTransferQueue<>();
        this.blockedTaskQueue = new ArrayList<>();
        this.blockedTaskSynchronizers = new ConcurrentHashMap<>();

        this.dispatchService = new AbstractExecutionThreadService() {
            @Override
            protected void run() throws Exception {
                while (this.isRunning()) {
                    tryDispatchOnce(100, TimeUnit.MILLISECONDS);

                    context.notifyBlockingQueueSizeChanged(blockedTaskQueue.size());
                }
            }
        };
        this.dispatchService.startAsync().awaitRunning();
    }

    @Override
    public void close() throws Exception {
        this.dispatchService.stopAsync().awaitTerminated();
    }

    /**
     * @param task
     * @throws InterruptedException
     */
    public void put(T task) throws InterruptedException {
        this.taskQueue.put(task);
    }

    private void tryDispatchOnce(long timeout, TimeUnit unit) throws InterruptedException {
        // Always checking blocked queue size and dispatching if available.
        //
        // Assume the dispatcher always much faster than the executor, this should not impact
        // overall throughput, and size of blocked queue should be controlled anyway.
        //
        // This can be potentially improved based on hits triggered from executor thread.
        // Since the executor threads are triggering in separate threads, care should be
        // taken to ensure that there are no split-brain events put the blocked queue forever.
        //
        // boolean bqDispatching = context.getAndResetBlockingQueueDispatching();
        //

        if (blockedTaskQueue.size() > 0) {
            dispatchBlockingQueue();
        }

        T task = taskQueue.poll(timeout, unit);
        if (task == null) {
            return;
        }

        dispatchOneTask(task);
    }

    private void dispatchBlockingQueue() throws InterruptedException {
        if (blockedTaskQueue.isEmpty()) {
            return;
        }

        Map<Object, Long> tempSynchronizers = new ConcurrentHashMap<>();
        List<T> tempBlockingTasks = new ArrayList<>();

        for (T task : blockedTaskQueue) {
            List<Object> syncList = context.getTaskSynchronizerResolver().apply(task);

            boolean blocked = false;
            for (Object sync : syncList) {
                if (tempSynchronizers.containsKey(sync)) {
                    blocked = true;
                    break;
                }
            }

            if (!blocked) {
                blocked = !context.putToExecutor(task, syncList);
            }

            if (blocked) {
                for (Object sync : syncList) {
                    tempSynchronizers.compute(sync, (k, v) -> {
                        if (v == null) {
                            return 1L;
                        } else {
                            return v + 1;
                        }
                    });
                }

                tempBlockingTasks.add(task);
            }
        }

        this.blockedTaskQueue.clear();
        this.blockedTaskSynchronizers.clear();

        this.blockedTaskQueue.addAll(tempBlockingTasks);
        this.blockedTaskSynchronizers.putAll(tempSynchronizers);
    }

    private void dispatchOneTask(T task) throws InterruptedException {
        List<Object> synchronizers = context.getTaskSynchronizerResolver().apply(task);
        if (synchronizers.isEmpty()) {
            context.putToExecutor(task, synchronizers);
            return;
        }

        boolean blockedByBlockedQueue = false;
        for (Object sync : synchronizers) {
            if (blockedTaskQueue.contains(sync)) {
                blockedByBlockedQueue = true;
                break;
            }
        }

        if (blockedByBlockedQueue) {
            for (Object sync : synchronizers) {
                blockedTaskSynchronizers.computeIfPresent(sync, (k, v) -> {
                    return v + 1;
                });
            }

            blockedTaskQueue.add(task);
        }

        if (synchronizers.size() == 1) {
            context.putToExecutor(task, synchronizers);
            return;
        }

        boolean eqAdded = context.putToExecutor(task, synchronizers);
        if (!eqAdded) {
            for (Object sync : synchronizers) {
                blockedTaskSynchronizers.compute(sync, (k, v) -> {
                    if (v == null) {
                        return 1L;
                    } else {
                        return v + 1;
                    }
                });
            }

            blockedTaskQueue.add(task);
        }
    }
}
