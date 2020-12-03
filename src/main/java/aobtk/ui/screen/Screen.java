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
package aobtk.ui.screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import aobtk.hw.Bonnet;
import aobtk.hw.HWButton;
import aobtk.i18n.Str;
import aobtk.oled.Display;
import aobtk.ui.element.UIElement;

public abstract class Screen {
    /** The current screen. */
    private static volatile Screen currScreen;

    /** The parent screen of this screen. */
    protected final Screen parentScreen;

    /** The current UI. */
    private volatile UIElement ui;

    protected Object uiLock = new Object();

    /** A set of possibly-active tasks, to allow all active tasks to be canceled. */
    private static Set<Future<?>> possiblyActiveTasks = Collections
            .newSetFromMap(new ConcurrentHashMap<Future<?>, Boolean>());

    /** Start the screen repaint thread. */
    public static void init(Screen rootScreen) {
        // Start the completed task cleaner thread
        Bonnet.executor.submit(() -> {
            List<Future<?>> completedTasks = new ArrayList<>();
            while (true) {
                Thread.sleep(1000);

                for (Future<?> task : possiblyActiveTasks) {
                    if (task.isCancelled() || task.isDone()) {
                        completedTasks.add(task);
                    }
                }
                if (!completedTasks.isEmpty()) {
                    possiblyActiveTasks.removeAll(completedTasks);
                    completedTasks.clear();
                }
            }
        });
        setCurrScreen(rootScreen);
    }

    /** Cancel all tasks that have not yet been canceled or completed. */
    public static void cancelAllPendingTasks() {
        for (Future<?> task : possiblyActiveTasks) {
            task.cancel(true);
            possiblyActiveTasks.remove(task);
        }
    }

    /** Schedule a task to run immediately. */
    public static Future<?> runTask(Runnable runnable) {
        Future<?> task = Bonnet.executor.submit(() -> {
            runnable.run();
            return null;
        });
        possiblyActiveTasks.add(task);
        return task;
    }

    /** Schedule a task to run immediately. */
    public static <T> Future<T> runTask(Callable<T> callable) {
        Future<T> task = Bonnet.executor.submit(callable);
        possiblyActiveTasks.add(task);
        return task;
    }

    /** Schedule a task to run after a delay. */
    public static <T> Future<T> scheduleTaskAfterDelay(int delayMillis, Callable<T> callable) {
        Future<T> task = Bonnet.executor.submit(() -> {
            Thread.sleep(delayMillis);
            return callable.call();
        });
        possiblyActiveTasks.add(task);
        return task;
    }

    /** Schedule a task to run after a delay. */
    public static Future<?> scheduleTaskAfterDelay(int delayMillis, Runnable runnable) {
        Future<?> task = Bonnet.executor.submit(() -> {
            Thread.sleep(delayMillis);
            runnable.run();
            return null;
        });
        possiblyActiveTasks.add(task);
        return task;
    }

    public Screen(Screen parentScreen) {
        this.parentScreen = parentScreen;
    }

    /**
     * Called when {@link Screen} is first opened. Implement to initialize subclasses, and to call
     * {@link #setUI(UIElement)} with the initial toplevel {@link UIElement}.
     */
    public abstract void open();

    /** Called when {@link Screen} is closed. Override if necessary. */
    public void close() {
    }

    /** Set current screen. */
    public static void setCurrScreen(Screen newCurrScreen) {
        // Run on UI thread so that changing the screen is ordered with respect to UI updates and redraws
        Bonnet.runOnUIThread(() -> {
            if (newCurrScreen != null) {
                Screen currScr = currScreen;
                if (newCurrScreen != null && currScr != newCurrScreen) {
                    if (currScr != null) {
                        // Close current screen
                        currScr.close();
                    }

                    // Cancel any pending jobs in current screen that currScreen.close() did not cancel
                    cancelAllPendingTasks();

                    // Open newCurrScreen (any repaint() will be ignored until currScreen is set below,
                    // since currScreen != newCurrScreen at this point)
                    newCurrScreen.open();

                    // Set currScreen to newCurrScreen
                    currScreen = newCurrScreen;

                    // Schedule initial repaint of newCurrScreen
                    newCurrScreen.repaint();
                }
            }
        });
    }

    public void goToParentScreen() {
        Screen currScr = currScreen;
        if (currScr != null && currScr.parentScreen != null) {
            setCurrScreen(currScr.parentScreen);
        }
    }

    /**
     * Schedule a wait, then schedule a call to {@link #setCurrScreen(Screen)}. Cancel the returned {@link Future}
     * to cancel the wait.
     * 
     * @return the scheduled {@link Future}.
     */
    public Future<?> waitThenSetCurrScreen(int milliseconds, Screen newCurrScreen) {
        return scheduleTaskAfterDelay(milliseconds, () -> {
            // Check currScreen has not changed while waiting
            if (currScreen == Screen.this) {
                setCurrScreen(newCurrScreen);
            }
        });
    }

    public Future<?> waitThenGoToParentScreen(int milliseconds) {
        return scheduleTaskAfterDelay(milliseconds, () -> {
            // Check currScreen has not changed while waiting
            goToParentScreen();
        });
    }

    /** Sets the UI, then schedules a repaint. */
    protected void setUI(UIElement newUI) {
        synchronized (uiLock) {
            this.ui = newUI;
            repaint();
        }
    }

    /** Schedule a repaint. */
    public void repaint() {
        Display display;
        synchronized (uiLock) {
            // Take a screenshot of current UI
            display = Bonnet.render(ui);
        }

        // Schedule OLED panel to display the screenshot, as long as curr screen doesn't change
        Bonnet.scheduleDrawIf(display, () -> currScreen == this);
    }

    /**
     * If returns true, this screen should be passed the HWButton.A event, otherwise the button just moves up to the
     * parent screen.
     */
    public boolean acceptsButtonA() {
        return false;
    }

    /** A button was pressed. */
    public static void buttonPressed(HWButton button, boolean down) {
        Screen currScr = currScreen;
        if (currScr != null) {
            // System.out.println(button + (down ? " pressed" : " released"));
            if (button == HWButton.C && down) {
                Str.lang = (Str.lang + 1) % 3; // @@TODO temp

            } else if (down) {
                // Only send button down events to current screen
                if (button == HWButton.A && !currScreen.acceptsButtonA()) {
                    // Button A goes back up to parent (works as Cancel) unless accepted by Screen instance
                    currScreen.goToParentScreen();
                } else {
                    // Other button
                    currScreen.buttonDown(button);
                }
            }
            currScr.repaint();
        }
    }

    /** A button was pressed. */
    public abstract void buttonDown(HWButton button);
}
