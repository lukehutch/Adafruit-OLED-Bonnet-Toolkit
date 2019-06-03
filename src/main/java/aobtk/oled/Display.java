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
package aobtk.oled;

import java.io.IOException;
import java.util.Arrays;

import aobtk.font.FontStyle;

/**
 * A display class for the OLED driver.
 *
 * @author Luke Hutchison
 */
public class Display {
    /** The image buffer. */
    private final byte[] pixBuffer = new byte[OLEDDriver.DISPLAY_WIDTH * OLEDDriver.DISPLAY_HEIGHT];

    /** Pixels to invert and add a dilated halo around. */
    private final byte[] invertBuffer = new byte[OLEDDriver.DISPLAY_WIDTH * OLEDDriver.DISPLAY_HEIGHT];

    /** The buffer that is actually sent to the display on {@link Display#update()}. */
    private final byte[] bitBuffer = new byte[(OLEDDriver.DISPLAY_WIDTH * OLEDDriver.DISPLAY_HEIGHT) / 8];

    private final OLEDDriver driver;

    private volatile boolean shutdown;

    public Display(OLEDDriver driver) {
        this.driver = driver;
    }

    public synchronized void clear() {
        Arrays.fill(pixBuffer, (byte) 0x00);
        Arrays.fill(invertBuffer, (byte) 0x00);
    }

    public int getWidth() {
        return OLEDDriver.DISPLAY_WIDTH;
    }

    public int getHeight() {
        return OLEDDriver.DISPLAY_HEIGHT;
    }

    public synchronized void setPixel(int x, int y, boolean drawWhite, FontStyle.Highlight highlight) {
        if (x >= 0 && x < OLEDDriver.DISPLAY_WIDTH && y >= 0 && y < OLEDDriver.DISPLAY_HEIGHT) {
            pixBuffer[x + y * OLEDDriver.DISPLAY_WIDTH] = drawWhite ? (byte) 1 : (byte) 0;
            if (highlight == FontStyle.Highlight.HALO) {
                // Surround pixel with inverted region, if pixel is highlighted
                for (int yo = y - 1, yoMax = y + 1; yo <= yoMax; yo++) {
                    for (int xo = x - 1, xoMax = x + 1; xo <= xoMax; xo++) {
                        if (xo >= 0 && xo < OLEDDriver.DISPLAY_WIDTH && yo >= 0 && yo < OLEDDriver.DISPLAY_HEIGHT) {
                            invertBuffer[xo + yo * OLEDDriver.DISPLAY_WIDTH] = (byte) 1;
                        }
                    }
                }
            }
        }
    }

    public synchronized void invertBlock(int x, int y, int w, int h) {
        for (int yo = y, y1 = y + h; yo < y1; yo++) {
            for (int xo = x, x1 = x + w; xo < x1; xo++) {
                if (xo >= 0 && xo < OLEDDriver.DISPLAY_WIDTH && yo >= 0 && yo < OLEDDriver.DISPLAY_HEIGHT) {
                    int addr = xo + yo * OLEDDriver.DISPLAY_WIDTH;
                    invertBuffer[addr] = (byte) 1;
                }
            }
        }
    }

    public synchronized void setPixel(int x, int y, boolean drawWhite) {
        setPixel(x, y, drawWhite, FontStyle.Highlight.NONE);
    }

    /** Set or clear all pixels in a rectangle. */
    public void drawRect(int x, int y, int width, int height, boolean drawWhite) {
        for (int posX = x; posX < x + width; ++posX) {
            for (int posY = y; posY < y + height; ++posY) {
                setPixel(posX, posY, drawWhite);
            }
        }
    }

    /**
     * Send the current buffer to the display.
     * 
     * @throws IOException
     */
    public void update() throws IOException {
        Arrays.fill(bitBuffer, (byte) 0);
        for (int y = 0; y < OLEDDriver.DISPLAY_HEIGHT; y++) {
            for (int x = 0; x < OLEDDriver.DISPLAY_WIDTH; x++) {
                int addr = x + y * OLEDDriver.DISPLAY_WIDTH;
                int pix = pixBuffer[addr];
                int inverted = invertBuffer[addr];
                if (inverted != 0) {
                    // Write a black inverted pixel
                    pix = 1 - pix;
                }
                // Write pixels to bit array
                if (pix == 1) {
                    bitBuffer[x + (y >> 3) * OLEDDriver.DISPLAY_WIDTH] |= (1 << (y & 0x07));
                }
            }
        }
        if (!shutdown) {
            // Update display
            driver.update(bitBuffer);
        }
    }
    
    public void shutdown() {
        this.shutdown = true;
    }
}
