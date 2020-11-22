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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.pi4j.exception.ShutdownException;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalStateChangeListener;
import com.pi4j.io.gpio.digital.PullResistance;

public enum HWButton {
    //    // For RPi Zero, with Pi4J v1:
    //    A(21), B(22), L(2), R(4), U(0), D(3), C(7);

    // For RPi 4B, with Pi4J v2:
    C(04), A(5), B(6), U(17), R(23), L(27), D(22);

    DigitalInput digitalInput;

    private Queue<DigitalStateChangeListener> listeners = new ConcurrentLinkedDeque<>();
    private DigitalStateChangeListener metaListener;
    private boolean isShutDown;

    /** Test whether a given button is currently down. */
    public boolean isDown() {
        return Bonnet.buttonDownMap.getOrDefault(this, Boolean.FALSE);
    }

    HWButton(int pin) {
        try {
            digitalInput = Bonnet.pi4j.create(DigitalInput.newConfigBuilder(Bonnet.pi4j).id("gpio-pin-" + pin)
                    .name("Pin #" + pin).address(pin).pull(PullResistance.PULL_UP).build());
        } catch (Exception e) {
            throw new RuntimeException("Could not set up digital input " + pin, e);
        }
        metaListener = e -> {
            if (!isShutDown) {
                for (var listener : listeners) {
                    listener.onDigitalStateChange(e);
                }
            }
        };
        digitalInput.addListener(metaListener);
    }

    public void addListener(DigitalStateChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(DigitalStateChangeListener listener) {
        listeners.remove(listener);
    }

    public void removeAllListeners() {
        listeners.clear();
    }

    public void shutdown() {
        removeAllListeners();
        if (digitalInput != null) {
            digitalInput.removeListener(metaListener);
            try {
                digitalInput.shutdown(Bonnet.pi4j);
                isShutDown = true;
            } catch (ShutdownException e) {
                // Ignore
            }
            metaListener = null;
        }
    }
}