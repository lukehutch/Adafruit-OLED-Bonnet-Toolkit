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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import aobtk.util.TaskExecutor.TaskResult;

public class Command {

    private static final TaskExecutor CMD_EXECUTOR = new TaskExecutor();

    public static class CommandException extends Exception {
        public CommandException() {
            super();
        }

        public CommandException(String message, Throwable cause, boolean enableSuppression,
                boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }

        public CommandException(String message, Throwable cause) {
            super(message, cause);
        }

        public CommandException(String message) {
            super(message);
        }

        public CommandException(Throwable cause) {
            super(cause);
        }

        private static final long serialVersionUID = 1L;
    }

    private static String join(String[] cmd) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < cmd.length; i++) {
            if (buf.length() > 0) {
                buf.append(' ');
            }
            buf.append(cmd[i]);
        }
        return buf.toString();
    }

    /**
     * Run a command, passing each line of stdout or stderr to the given consumer. If the thread is interrupted, the
     * child process is killed.
     */
    public static TaskResult<Integer> commandWithConsumer(String[] cmd, TaskExecutor executor,
            boolean consumeStderr, Consumer<String> lineConsumer) throws CommandException {
        Process process;
        try {
            System.out.println("CMD: \"" + join(cmd) + "\"");
            process = Runtime.getRuntime().exec(cmd);
        } catch (IOException | SecurityException e) {
            throw new CommandException(e);
        }

        TaskExecutor cancellationCheckerExecutor = new TaskExecutor();

        // Schedule task to read from command's stdout
        AtomicBoolean interrupted = new AtomicBoolean();
        TaskResult<Integer> taskResult = executor.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        consumeStderr ? process.getErrorStream() : process.getInputStream()))) {
                    for (String line; (line = reader.readLine()) != null;) {
                        lineConsumer.accept(line);
                        if (Thread.currentThread().isInterrupted()) {
                            interrupted.set(true);
                            System.out.println(
                                    "Child process was destroyed by thread interruption: \"" + join(cmd) + "\"");
                            process.destroy();
                            cancellationCheckerExecutor.shutdown();
                        }
                    }
                } catch (IOException e) {
                    if (!interrupted.get()) {
                        // IOException was thrown, but process was not interrupted -- should not happen?
                        throw new CommandException(e);
                    } else {
                        // Stream was closed because process was destroyed -- ignore
                    }
                } finally {
                    if (!consumeStderr) {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(process.getErrorStream()))) {
                            for (String line; (line = reader.readLine()) != null;) {
                                System.out.println(
                                        "  **** stderr output from \"" + join(cmd) + "\": " + line);
                            }
                        } catch (IOException e2) {
                            // Ignore
                        }
                    }
                }
                int exitCode = process.waitFor();

                // Shut down cancellation checker executor, which will cancel cancellation checker
                cancellationCheckerExecutor.shutdown();

                return exitCode;
            }
        });

        // Start cancellation checker -- this is needed since BufferedReader is based on the
        // non-interruptible read() call.
        cancellationCheckerExecutor.submit(() -> {
            while (!taskResult.isDone() && !taskResult.isCanceled()) {
                // Sleep (this is cancellable)
                Thread.sleep(1000);
            }
            if (taskResult.isCanceled()) {
                interrupted.set(true);
                System.out.println("Child process was destroyed by thread interruption: \"" + join(cmd) + "\"");
                process.destroy();
            }
            return null;
        });

        return taskResult;
    }

    /**
     * Run a command, passing each line of stdout or stderr to the given consumer. If the thread is interrupted, the
     * child process is killed.
     */
    public static TaskResult<Integer> commandWithConsumer(String[] cmd, boolean consumeStderr,
            Consumer<String> lineConsumer) throws CommandException {
        return commandWithConsumer(cmd, CMD_EXECUTOR, consumeStderr, lineConsumer);
    }

    public static List<String> command(String[] cmd, TaskExecutor executor)
            throws CommandException, InterruptedException, CancellationException {
        // Call command, and collect result lines
        List<String> result = new ArrayList<>();
        TaskResult<Integer> statusCodeResult = commandWithConsumer(cmd, executor, /* consumeStderr = */ false,
                result::add);

        // Wait for termination, and get status code
        int statusCode;
        try {
            statusCode = statusCodeResult.get();
        } catch (ExecutionException e) {
            throw new CommandException(e.getCause());
        }
        if (statusCode != 0) {
            // If status code was not 0, throw CommandException
            throw new CommandException("Got exit code " + statusCode + " for command \"" + join(cmd) + "\"");
        }

        // If status code was 0, return result lines
        return result;
    }

    public static List<String> command(String... cmd)
            throws CommandException, InterruptedException, CancellationException {
        return command(cmd, CMD_EXECUTOR);
    }
}
