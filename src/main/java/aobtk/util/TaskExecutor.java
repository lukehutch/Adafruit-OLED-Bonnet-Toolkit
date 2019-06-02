/*
 * This file is part of the Adafruit OLED Bonnet Toolkit: a Java toolkit for the Adafruit 128x64 OLED bonnet,
 * with support for the screen, D-pad/buttons, UI layout, and task scheduling.
 *
 * Author: Luke Hutchison
 *
 * Hosted at: https://github.com/lukehutch/Adafruit-OLED-Bonnet-Toolkit
 * 
 * This code is not associated with or endorsed by Adafruit. Adafruit is a trademark of Limor "Ladyada" Fried. 
 *
 * --
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Luke Hutchison
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package aobtk.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * ThreadExecutor, an {@link ExecutorService} for execution of single tasks in sequential order.
 */
public class TaskExecutor {

    /** The executor service. */
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    /** Pending jobs. */
    private Set<TaskResult<?>> pendingTasks = Collections
            .newSetFromMap(new ConcurrentHashMap<TaskResult<?>, Boolean>());

    /** All ThreadExecutors. */
    private static final Set<TaskExecutor> allThreadExecutors = Collections
            .newSetFromMap(new ConcurrentHashMap<TaskExecutor, Boolean>());

    /** True when all ThreadExecutors have been shut down. */
    private static boolean isShutDown;

    /**
     * An version of {@link Function} that may throw an exception (or alternatively, a version of {@link Callable}
     * that takes an argument}.
     */
    @FunctionalInterface
    public static interface CallableFunction<S, T> {
        /**
         * Call the function.
         *
         * @param value the parameter value.
         * @return the result value.
         */
        public T call(S value) throws Exception;
    }

    /** An version of {@link Runnable} that takes an argument and can throw an Exception. */
    @FunctionalInterface
    public static interface RunnableConsumer<T> {
        /**
         * Call the function.
         *
         * @param value the parameter value.
         * @return the result value.
         */
        public void consume(T value) throws Exception;
    }

    /**
     * The Class Job.
     *
     * @param <T> job result type.
     */
    public class TaskResult<T> {
        /** The result. */
        Future<T> result;

        /** Any previous task this task was scheduled to follow, using then(). */
        private AtomicReference<TaskResult<?>> previousTask = new AtomicReference<>();

        /** The handler for any exceptions that are thrown. */
        private RunnableConsumer<? extends Exception> exceptionHandler;

        /** The handler for cancellation/interruption. */
        private Runnable cancellationHandler;

        /**
         * Instantiate a new job.
         *
         * @param callable the {@link Callable}
         */
        private TaskResult(Callable<T> callable) {
            ExecutorService es = executorService;
            if (es == null) {
                throw new CancellationException("ExecutorService has already been shut down");
            }

            // Wrap the Callable in another Callable that detects problems as soon as they happen,
            // so that the throwableHandler can be called immediately, rather than on get()
            try {
                this.result = es.submit(() -> {
                    try {
                        return callable.call();
                    } catch (InterruptedException | CancellationException e) {
                        Runnable r = getTaskChainRoot().cancellationHandler;
                        if (r != null) {
                            r.run();
                        }
                        // Re-throw the exception, so that it can be caught if get() is called
                        throw e;
                    } catch (Exception e) {
                        e.printStackTrace();
                        @SuppressWarnings("unchecked")
                        RunnableConsumer<Exception> eh = (RunnableConsumer<Exception>) getTaskChainRoot().exceptionHandler;
                        if (eh != null) {
                            try {
                                eh.consume(e);
                            } catch (Exception e2) {
                                System.out.println("Exception handler threw an exception");
                                e2.printStackTrace();
                                e.addSuppressed(e2);
                            }
                        }
                        // Re-throw the exception, so that it can be caught if get() is called
                        throw e;
                    }
                });
                pendingTasks.add(this);
            } catch (RejectedExecutionException e) {
                // Executor was already shut down
                this.result = CompletableFuture.failedFuture(e);
            }
        }

        /**
         * Instantiate a new job with the specificed {@link Future}.
         *
         * @param result the result
         */
        private TaskResult(Future<T> result) {
            this.result = result;
        }

        /**
         * Block until this task has finished, then return the result of the task.
         *
         * @return the result.
         * @throws InterruptedException if the thread was interrupted (while running) or canceled (before it had a
         *                              chance to run).
         * @throws ExecutionException   if the job threw an exception.
         */
        public T get() throws InterruptedException, ExecutionException {
            try {
                return result.get();
            } catch (CancellationException e) {
                throw new InterruptedException();
            } finally {
                // Remove this task from the set of pending tasks, since its result is now known
                pendingTasks.remove(this);
            }
        }

        /**
         * Cancel the {@link TaskResult}, and any task this task was scheduled to follow (via then()). N.B. waits
         * for task to complete, unlike {@link Future#cancel(boolean)}.
         *
         * @return false if the task could not be cancelled, typically because it has already completed normally;
         *         true otherwise.
         */
        public boolean cancel() {
            boolean cancelResult = false;
            try {
                // Cancel this task
                cancelResult = result.cancel(/* mayInterruptIfRunning = */ true);

                // Wait for task termination
                try {
                    result.get();
                } catch (InterruptedException | CancellationException e) {
                    cancelResult = true;
                } catch (ExecutionException e) {
                    // Exception will be caught when/if result.get() is called again,
                    // so ignore it here
                    cancelResult = true;
                }
            } finally {
                // Remove this task from the list of pending tasks
                pendingTasks.remove(this);
            }
            // Cancel previous task, if any
            TaskResult<?> prevTask = previousTask.get();
            if (prevTask != null) {
                cancelResult |= prevTask.cancel();
            }
            return cancelResult;
        }

        /**
         * Schedule the given {@link CallableFunction} to run after this {@link TaskResult}, but only if this
         * {@link TaskResult} was not interrupted/canceled, and if it did not throw an exception. The returned
         * {@link TaskResult} will pass the result of this {@link TaskResult} to the {@link CallableFunction}, and
         * will return the result of the {@link CallableFunction}.
         *
         * @param                      <U> the return type
         * @param nextCallableFunction the {@link CallableFunction} to schedule after this {@link TaskResult} has
         *                             run.
         * @return the {@link TaskResult} that wraps nextCallable.
         * @throws ExecutionException   if either this {@link TaskResult} or the scheduled {@link CallableFunction}
         *                              throws an exception.
         * @throws InterruptedException if this {@link TaskResult} was canceled, or if either this
         *                              {@link TaskResult} or the scheduled {@link CallableFunction} is interrupted.
         */
        public <V> TaskResult<V> then(CallableFunction<T, V> nextCallableFunction) {
            TaskResult<V> newTask = submit(() -> {
                T result1;
                try {
                    // Get result of this job
                    result1 = TaskResult.this.get();
                } catch (CancellationException e) {
                    throw new InterruptedException();
                }
                if (Thread.interrupted()) {
                    // Check for thread interruption after first task runs
                    throw new InterruptedException();
                }
                try {
                    // Pass result of this job to the next job
                    V result2 = nextCallableFunction.call(result1);
                    if (Thread.interrupted()) {
                        // Check for thread interruption at end of second task
                        throw new InterruptedException();
                    }
                    return result2;
                } catch (InterruptedException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ExecutionException(e);
                }
            });
            // Set previousTask field, so that canceling new task also cancels previous task
            TaskResult<?> old = newTask.previousTask.getAndSet(TaskResult.this);
            if (old != null) {
                throw new RuntimeException("Task cannot follow more than one other task");
            }
            return newTask;
        }

        /**
         * Schedule the given {@link Callable} to run after this {@link TaskResult}, but only if this
         * {@link TaskResult} was not interrupted/canceled, and if it did not throw an exception. The returned
         * {@link TaskResult} will drop the result of this {@link TaskResult} and will return the result of the
         * passed {@link Callable}.
         *
         * @param              <U> the return type
         * @param nextCallable the {@link Callable} to schedule after this {@link TaskResult} has run.
         * @return the {@link TaskResult} that wraps nextCallable.
         * @throws ExecutionException   if either this {@link TaskResult} or the scheduled {@link Callable} throws
         *                              an exception.
         * @throws InterruptedException if this {@link TaskResult} was canceled, or if either this
         *                              {@link TaskResult} or the scheduled {@link Callable} is interrupted.
         */
        public <U> TaskResult<U> then(Callable<U> nextCallable) {
            return then((CallableFunction<T, U>) ignoredValue -> nextCallable.call());
        }

        /**
         * Schedule the given {@link Runnable} to run after this {@link TaskResult}, but only if this
         * {@link TaskResult} was not interrupted/canceled, and if it did not throw an exception. The returned
         * {@link TaskResult} will drop the result of this {@link TaskResult}.
         * 
         * @throws ExecutionException   if either this {@link TaskResult} or the scheduled {@link Runnable} throws
         *                              an exception.
         * @throws InterruptedException if this {@link TaskResult} was canceled, or if either this
         *                              {@link TaskResult} or the scheduled {@link Runnable} is interrupted.
         */
        public TaskResult<Void> then(Runnable nextRunnable) {
            return then((CallableFunction<T, Void>) ignoredValue -> {
                nextRunnable.run();
                return null;
            });
        }

        /**
         * Schedule the given {@link CallableFunction} to run after this {@link TaskResult}, but only if this
         * {@link TaskResult} was not interrupted/canceled, and if it did not throw an exception. The returned
         * {@link TaskResult} will pass the result of this {@link TaskResult} to the {@link CallableFunction}, and
         * will return the result of the {@link CallableFunction}.
         *
         * @param                      <U> the return type
         * @param nextRunnableConsumer the {@link CallableFunction} to schedule after this {@link TaskResult} has
         *                             run.
         * @return the {@link TaskResult} that wraps nextCallable.
         * @throws ExecutionException   if either this {@link TaskResult} or the scheduled {@link CallableFunction}
         *                              throws an exception.
         * @throws InterruptedException if this {@link TaskResult} was canceled, or if either this
         *                              {@link TaskResult} or the scheduled {@link CallableFunction} is interrupted.
         */
        public TaskResult<Void> then(RunnableConsumer<T> nextRunnableConsumer) {
            return then((CallableFunction<T, Void>) value -> {
                nextRunnableConsumer.consume(value);
                return null;
            });
        }

        /** Get the first TaskResult in the chain. */
        private TaskResult<?> getTaskChainRoot() {
            TaskResult<?> t = this;
            while (t.previousTask.get() != null) {
                t = t.previousTask.get();
            }
            return t;
        }

        /**
         * Set the exception handler for the task chain (a chain of tasks that follow each other using then()). Can
         * only be set once per task chain. Called if any task in the chain throws an exception.
         * 
         * @return this
         */
        public <E extends Exception> TaskResult<T> onException(RunnableConsumer<E> exceptionHandler) {
            getTaskChainRoot().exceptionHandler = exceptionHandler;
            return this;
        }

        /**
         * Set the cancellation/interruption handler for the task chain (a chain of tasks that follow each other
         * using then()). Can only be set once per task chain. Called if any task in the chain is cancelled or
         * interrupted.
         * 
         * @return this
         */
        public <E extends Exception> TaskResult<T> onCancel(Runnable cancellationHandler) {
            getTaskChainRoot().cancellationHandler = cancellationHandler;
            return this;
        }

        /** Return true if this task is completed. */
        public boolean isDone() {
            return result.isDone();
        }

        /** Return true if this task is canceled. */
        public boolean isCanceled() {
            return result.isCancelled();
        }
    }

    /**
     * Instantiate a new {@link TaskExecutor} for execution of single tasks in sequential order.
     */
    public TaskExecutor() {
        if (isShutDown) {
            // Assume System.exit() is about to be called
            System.err.println(
                    "ThreadExcecutor.shutdownAll() has already been called, cannot start a new ThreadExecutor");
        } else {
            allThreadExecutors.add(this);
        }
    }

    /**
     * Submit a {@link Callable} for execution.
     *
     * @param          <T> the result type.
     * @param callable the {@link Callable}.
     * @return the {@link TaskResult}.
     */
    public <T> TaskResult<T> submit(Callable<T> callable) {
        return new TaskResult<T>(callable);
    }

    /**
     * Submit a {@link Runnable} for execution.
     *
     * @param runnable the {@link Runnable}.
     * @return the {@link TaskResult}.
     */
    public TaskResult<Void> submit(Runnable runnable) {
        return new TaskResult<Void>(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Schedule a Job to wait a given number of milliseconds. The wait may be interrupted by calling
     * {@link TaskResult#cancel()}.
     *
     * @param milliseconds the number of milliseconds to wait.
     * @return the {@link TaskResult}.
     */
    public TaskResult<Void> submitWait(int milliseconds) {
        return new TaskResult<Void>(() -> {
            Thread.sleep(milliseconds);
            return null;
        });
    }

    /**
     * Instantiate a new completed {@link TaskResult} with the specified result value.
     *
     * @param future the {@link Future}.
     */
    public <T> TaskResult<T> completed(T result) {
        return new TaskResult<T>(CompletableFuture.completedFuture(result));
    }

    /**
     * Instantiate a new failed {@link TaskResult} with the specified {@link Throwable}.
     *
     * @param future the {@link Future}.
     */
    public <T> TaskResult<T> failed(Throwable e) {
        return new TaskResult<T>(CompletableFuture.failedFuture(e));
    }

    /**
     * Cancel all pending {@link TaskResult} instances.
     */
    public void cancelAllPendingJobs() {
        for (TaskResult<?> task : new ArrayList<>(pendingTasks)) {
            task.cancel();
        }
    }

    /**
     * Shut down this {@link TaskExecutor}.
     */
    public void shutdown() {
        cancelAllPendingJobs();
        allThreadExecutors.remove(this);
        if (executorService != null) {
            executorService.shutdownNow();
            try {
                executorService.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // Ignore
            }
            executorService = null;
        }
    }

    /**
     * Shut down all {@link TaskExecutor} instances.
     */
    public static synchronized void shutdownAll() {
        isShutDown = true;
        while (!allThreadExecutors.isEmpty()) {
            for (TaskExecutor threadExecutor : new ArrayList<>(allThreadExecutors)) {
                threadExecutor.shutdown();
            }
        }
    }
}
