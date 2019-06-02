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
package aobtk.ui.element;

import aobtk.oled.Display;
import aobtk.ui.measurement.Size;

public class ProgressBar extends UIElement {
    Size expectedSize;
    int numer;
    int denom;

    public ProgressBar(int w, int h) {
        expectedSize = new Size(w, h);
    }

    public void setProgress(int numer, int denom) {
        this.denom = Math.max(1, denom);
        this.numer = Math.max(0, Math.min(numer, denom));
    }

    public boolean isComplete() {
        return numer == denom;
    }

    @Override
    protected Size measure(int maxW, int maxH) {
        return size = hide ? Size.ZERO
                : expectedSize.w <= maxW && expectedSize.h <= maxH //
                        ? expectedSize
                        : new Size(Math.min(expectedSize.w, maxW), Math.min(expectedSize.h, maxH));
    }

    @Override
    protected void render(int x, int y, int w, int h, Display display) {
        if (!hide) {
            int n = numer;
            int d = denom >= 1 ? denom : 1;
            if (w > 5 && h > 2) {
                // Draw solid bar inside single-pixel box for long progress bar
                display.drawRect(x, y, w, h, true);
                int drawW = n * (w - 2) / d;
                display.drawRect(x + 1 + drawW, y + 1, w - 2 - drawW, h - 2, false);
            } else {
                // Draw solid bar for short progress bar
                int drawW = n * w / d;
                display.drawRect(x, y, drawW, h, false);
            }
        }
    }

    @Override
    public String toString() {
        return "ProgressBar";
    }
}
