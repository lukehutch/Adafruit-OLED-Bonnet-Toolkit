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
package hw;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

import oled.Display;
import oled.OLEDDriver;
import util.TaskExecutor;

public class Bonnet {

    public static final OLEDDriver oledDriver;

    static {
        try {
            // Initialize OLED screen
            oledDriver = new OLEDDriver();
        } catch (IOException | UnsupportedBusNumberException e) {
            throw new RuntimeException("Could not start OLED driver", e);
        }
    }

    public static final Display display = new Display(oledDriver);

    public static final GpioController GPIO = GpioFactory.getInstance();

    /** Whether or not each button is currently down. (Updated by GPIO pin state change listener thread.) */
    public static final Map<HWButton, Boolean> buttonDownMap = new ConcurrentHashMap<HWButton, Boolean>();

    /**
     * Initialize the hardwarer bonnet, adding a {@link HWButtonListener} for button events.
     */
    public static void init(HWButtonListener listener) {
        // Wire up buttons to listener
        for (HWButton b : HWButton.values()) {
            // Provision GPIO pin as an input pin with its internal pull up resistor enabled
            b.digitalInput = Bonnet.GPIO.provisionDigitalInputPin(b.pin, PinPullResistance.PULL_UP);

            // Set shutdown state for this input pin
            b.digitalInput.setShutdownOptions(true);

            // Register gpio pin listener
            b.digitalInput.addListener(new GpioPinListenerDigital() {
                @Override
                public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                    boolean buttonDown = event.getState() == PinState.LOW;
                    buttonDownMap.put(b, buttonDown);
                    if (listener != null) {
                        listener.onButtonEvent(b, buttonDown);
                    }
                }
            });
        }

        // Add shutdown hook for clean shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown();
            }
        });
    }

    public static void shutdown() {
        // Shut down all task executors
        TaskExecutor.shutdownAll();

        // Remove button listeners
        for (HWButton b : HWButton.values()) {
            if (b.digitalInput != null) {
                b.digitalInput.removeAllListeners();
                b.digitalInput = null;
            }
        }

        // Stop writing to display before shutting down the display hardware
        display.shutdown();

        // Shut down display
        oledDriver.shutdown();

        // Stop all GPIO activity/threads by shutting down the GPIO controller
        // (forcefully shuts down all GPIO monitoring threads and scheduled tasks)
        GPIO.shutdown();
    }
}