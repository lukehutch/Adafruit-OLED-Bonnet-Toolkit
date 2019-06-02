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
package aobtk.screen;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import aobtk.hw.Bonnet;
import aobtk.hw.HWButton;
import aobtk.oled.Display;
import aobtk.ui.element.UIElement;
import aobtk.util.TaskExecutor;
import aobtk.util.TaskExecutor.TaskResult;

public abstract class Screen {
    /** The current screen. */
    private static volatile Screen currScreen;

    /** Object used to hold lock when currScreen is being changed. */
    private static final Object currScreenLock = new Object();

    /** The parent screen of this screen. */
    protected volatile Screen parentScreen;

    /** The screen needs to be repainted */
    private static Semaphore repaint = new Semaphore(1);

    /** Every screen gets its own ThreadExecutor */
    protected final TaskExecutor taskExecutor = new TaskExecutor();

    /** The current UI. */
    private UIElement ui;

    /**
     * Hold this lock if you need to modify the UI non-atomically, to prevent the renderer from trying to render the
     * display while the UI is being updated.
     */
    protected static final Object uiLock = new Object();

    /** Start the screen repaint thread. */
    public static void init(Screen rootScreen) {
        // Only one display is supported currently
        Display display = Bonnet.display;
        new TaskExecutor().submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        // Block until one or more repaint requests have been queued.
                        // Get all available permits in case multiple repaints have been scheduled
                        // since the last repaint.
                        repaint.acquire(Math.max(1, repaint.availablePermits()));

                        // Hold currScreenLock so that currScreen doesn't change while rendering
                        synchronized (currScreenLock) {
                            if (currScreen != null) {
                                // Hold the UI lock so UI doesn't change while rendering
                                synchronized (uiLock) {
                                    // Clear the display buffer
                                    display.clear();

                                    // Render the UI into the display buffer
                                    if (currScreen.ui != null) {
                                        currScreen.ui.render(display);
                                    }

                                    // Update the display with the contents of the display buffer
                                    try {
                                        display.update();
                                    } catch (IOException e) {
                                        System.out.println("Failed to update display");
                                        e.printStackTrace();
                                        System.exit(1);
                                    }
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
        setCurrScreen(rootScreen);
    }

    /** Set the UI, then call {@link #repaint()}. */
    protected void setUI(UIElement newUI) {
        synchronized (currScreenLock) {
            synchronized (uiLock) {
                ui = newUI;
                // Only repaint if this screen is the current screen
                // (this prevents repaint() from being called from the constructor,
                // which is not needed, since repaint() will be called as soon as
                // setCurrScreen() is called with the new object).
                if (currScreen == this) {
                    repaint();
                }
            }
        }
    }

    public Screen(Screen parentScreen) {
        this.parentScreen = parentScreen;
    }

    /** Called when {@link Screen} is first opened. Implement to initialize subclasses. */
    public abstract void open();

    /** Called when {@link Screen} is first opened. Implement to tear down subclasses. */
    public abstract void close();

    /** Set current screen. */
    public static void setCurrScreen(Screen newCurrScreen) {
        synchronized (currScreenLock) {
            if (newCurrScreen != currScreen && newCurrScreen != null) {
                // Hold UI lock so that screen switching doesn't happen in the middle of a screen update
                synchronized (uiLock) {
                    if (currScreen != null) {
                        // Close current screen
                        currScreen.close();

                        // Cancel any pending jobs in current screen that currScreen.close() did not cancel
                        currScreen.taskExecutor.cancelAllPendingJobs();
                    }
                }

                // Set currScreen to newCurrScreen
                currScreen = newCurrScreen;

                // Open newCurrScreen
                currScreen.open();

                // Schedule initial repaint of newCurrScreen 
                repaint();
            }
        }
    }

    public void goToParentScreen() {
        synchronized (currScreenLock) {
            if (currScreen != null) {
                setCurrScreen(currScreen.parentScreen);
            }
        }
    }

    /**
     * Schedule a wait, then schedule a call to {@link #setCurrScreen(Screen)}. Cancel the returned
     * {@link TaskResult} to cancel the wait.
     * 
     * @return the scheduled {@link TaskResult}.
     */
    public TaskResult<Void> waitThenSetCurrScreen(int milliseconds, Screen newCurrScreen) {
        return currScreen.taskExecutor.submitWait(milliseconds).then(() -> {
            synchronized (currScreenLock) {
                // Check currScreen has not changed while waiting
                if (currScreen == Screen.this) {
                    setCurrScreen(newCurrScreen);
                }
            }
        });
    }

    public TaskResult<Void> waitThenGoToParentScreen(int milliseconds) {
        return currScreen.taskExecutor.submitWait(milliseconds).then(() -> {
            synchronized (currScreenLock) {
                // Check currScreen has not changed while waiting
                if (currScreen == Screen.this) {
                    setCurrScreen(currScreen.parentScreen);
                }
            }
        });
    }

    /** Schedule a repaint. */
    public static void repaint() {
        repaint.release();
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
        // Only send button down events to current screen
        if (down) {
            synchronized (currScreenLock) {
                if (button == HWButton.A && !currScreen.acceptsButtonA()) {
                    // Button A goes back up to parent (works as Cancel) unless accepted by Screen instance
                    currScreen.goToParentScreen();
                } else {
                    // Other button
                    currScreen.buttonDown(button);
                }
                // Button events always trigger a repaint
                repaint();
            }
        }
    }

    /** A button was pressed. */
    public abstract void buttonDown(HWButton button);
}
