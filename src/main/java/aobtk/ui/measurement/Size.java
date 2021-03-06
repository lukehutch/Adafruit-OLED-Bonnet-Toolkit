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
package aobtk.ui.measurement;

public class Size {
    public final int w;
    public final int h;

    public static final Size ZERO = new Size(0, 0);

    public Size(int w, int h) {
        this.w = w;
        this.h = h;
    }

    public Size(Size... sizes) {
        int wTot = 0;
        int hTot = 0;
        for (Size size : sizes) {
            wTot += size.w;
            hTot += size.h;
        }
        this.w = wTot;
        this.h = hTot;
    }
    
    @Override
    public String toString() {
        return "(" + w + " x " + h + ")";
    }
    
    public static Size min(Size size0, Size size1) {
        if (size0.w < size1.w && size0.h < size1.h) {
            return size0;
        } else if (size1.w < size0.w && size1.h < size0.h) {
            return size1;
        } else {
            return new Size(Math.min(size0.w, size1.w), Math.min(size0.h, size1.h));
        }
    }
    
    public static Size max(Size size0, Size size1) {
        if (size0.w > size1.w && size0.h > size1.h) {
            return size0;
        } else if (size1.w > size0.w && size1.h > size0.h) {
            return size1;
        } else {
            return new Size(Math.max(size0.w, size1.w), Math.max(size0.h, size1.h));
        }
    }
}
