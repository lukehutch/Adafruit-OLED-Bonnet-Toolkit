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
package aobtk.hw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.exception.LifecycleException;
import com.pi4j.exception.Pi4JException;
import com.pi4j.io.gpio.digital.DigitalState;

import aobtk.oled.Display;
import aobtk.oled.OLEDDriver;
import aobtk.ui.screen.Screen;

public class Bonnet {
    private static final Logger LOGGER = Logger.getLogger(Bonnet.class.getCanonicalName());

    /** Start new threads in daemon mode so they are killed when JVM tries to shut down. */
    public static ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    // Initialize the class -- for some reason a static initializer block is never called (JDK bug?)
    @SuppressWarnings("unused")
    private static Bonnet bonnet = new Bonnet();

    public static Context pi4j;

    private static HWButton[] hwButtons;

    /** Whether or not each button is currently down. (Updated by GPIO pin state change listener thread.) */
    public static final Map<HWButton, Boolean> buttonDownMap = new ConcurrentHashMap<HWButton, Boolean>();

    static Set<I2CDevice> i2cDevices = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static OLEDDriver oledDriver = new OLEDDriver();
    public static Display display = new Display(oledDriver);

    private Bonnet() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "TRACE");
        LOGGER.log(Level.INFO, "Initializing Bonnet");
        try {
            pi4j = Pi4J.newAutoContext();
            if (pi4j == null) {
                throw new Pi4JException("Could not get new auto context");
            }
        } catch (Pi4JException e) {
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, "Could not get Pi4J context", e);
            shutdown();
            System.exit(1);
        }

        try {
            hwButtons = HWButton.values();
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, "Could not open GPIO bus", e);
            shutdown();
            System.exit(1);
        }
        
        // Wire up buttons to listener
        for (HWButton button : HWButton.values()) {
            // Register gpio pin listener
            button.digitalInput.addListener(e -> {
                boolean down = e.state() == DigitalState.LOW;
                buttonDownMap.put(button, down);
                Screen.buttonPressed(button, down);
            });
        }

        // add shutdown hook that clears the display
        // and closes the bus correctly when the software
        // is terminated.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Bonnet shutdown hook");
                shutdown();
            }
        });
    }

    public static I2CDevice openI2CDevice(int busNum, int deviceAddr) {
        var i2cDevice = new I2CDevice(busNum, deviceAddr);
        return i2cDevice;
    }

    public static void shutdown() {
        LOGGER.log(Level.INFO, "Shutting down OLED Bonnet");

        executor.shutdownNow();
        try {
            executor.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e1) {
            // Ignore
        }
        
        // Shut down display driver
        if (display != null) {
            try {
                display.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
            display = null;
        }

        // Shut down display
        if (oledDriver != null) {
            try {
                oledDriver.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
            oledDriver = null;
        }

        // Shut down I2C devices
        if (i2cDevices != null) {
            for (var dev : new ArrayList<>(i2cDevices)) {
                dev.shutdown();
            }
            i2cDevices = null;
        }

        // Remove all GPIO button listeners
        if (hwButtons != null) {
            for (HWButton b : hwButtons) {
                if (b.digitalInput != null) {
                    try {
                        b.removeAllListeners();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    b.digitalInput = null;
                }
            }
        }

        // Shut down Pi4J
        if (pi4j != null) {
            try {
                pi4j.shutdown();
            } catch (LifecycleException e) {
                e.printStackTrace();
            }
            pi4j = null;
        }
    }
}