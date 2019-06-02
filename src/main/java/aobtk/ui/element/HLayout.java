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

import java.util.ArrayList;
import java.util.List;

import aobtk.oled.Display;
import aobtk.ui.element.VLayout.VAlign;
import aobtk.ui.measurement.Size;

public class HLayout extends Layout {
    private List<UIElement> leftElements = new ArrayList<>();
    private int leftTotWidth;
    private List<UIElement> centerElements = new ArrayList<>();
    private int centerTotWidth;
    private List<UIElement> rightElements = new ArrayList<>();
    private int rightTotWidth;
    private VAlign gravity = VAlign.CENTER;

    public static enum HAlign {
        LEFT, CENTER, RIGHT
    }

    public HLayout() {
    }

    public HLayout(UIElement... elements) {
        for (UIElement elt : elements) {
            add(elt);
        }
    }

    @Override
    public void clear() {
        leftElements.clear();
        centerElements.clear();
        rightElements.clear();
    }

    public void add(UIElement uiElement, HAlign hAlign) {
        (hAlign == HAlign.LEFT ? leftElements : hAlign == HAlign.RIGHT ? rightElements : centerElements)
                .add(uiElement);
    }

    @Override
    public void add(UIElement uiElement) {
        add(uiElement, HAlign.CENTER);
    }

    public void addSpace(int w, HAlign hAlign) {
        add(new Spacer(w, 0), hAlign);
    }

    @Override
    public void addSpace(int w) {
        addSpace(w, HAlign.CENTER);
    }

    /** Set vertical layout gravity for items within HLayout that don't fill all vertical space. */
    public void setGravity(VAlign gravity) {
        this.gravity = gravity;
    }

    @Override
    public Size measure(int maxW, int maxH) {
        if (hide) {
            return size = Size.ZERO;
        }
        int eltMaxH = 0;
        int remainingMaxW = maxW;
        leftTotWidth = 0;
        for (UIElement elt : leftElements) {
            elt.measure(remainingMaxW, maxH);
            leftTotWidth += elt.size.w;
            remainingMaxW = Math.max(0, remainingMaxW - elt.size.w);
            eltMaxH = Math.max(eltMaxH, elt.size.h);
        }
        rightTotWidth = 0;
        for (UIElement elt : rightElements) {
            elt.measure(remainingMaxW, maxH);
            rightTotWidth += elt.size.w;
            remainingMaxW = Math.max(0, remainingMaxW - elt.size.w);
            eltMaxH = Math.max(eltMaxH, elt.size.h);
        }
        centerTotWidth = 0;
        for (UIElement elt : centerElements) {
            elt.measure(remainingMaxW, maxH);
            centerTotWidth += elt.size.w;
            remainingMaxW = Math.max(0, remainingMaxW - elt.size.w);
            eltMaxH = Math.max(eltMaxH, elt.size.h);
        }
        return size = new Size(Math.min(leftTotWidth + centerTotWidth + rightTotWidth, maxW),
                Math.min(eltMaxH, maxH));
    };

    @Override
    protected void render(int x, int y, int maxW, int maxH, Display display) {
        if (!hide) {
            int xCurr = x;
            for (UIElement lElt : leftElements) {
                int renderH = Math.min(maxH, lElt.size.h);
                int yCurr = gravity == VAlign.TOP ? y
                        : gravity == VAlign.CENTER ? y + (maxH - renderH) / 2 : y + maxH - lElt.size.h;
                int renderW = Math.max(0, Math.min(maxW - (xCurr - x), lElt.size.w));
                if (renderW > 0 && renderH > 0) {
                    lElt.render(xCurr, yCurr, renderW, renderH, display);
                }
                xCurr += renderW;
            }
            xCurr = x + (maxW - leftTotWidth - centerTotWidth - rightTotWidth) / 2 + leftTotWidth;
            for (UIElement cElt : centerElements) {
                int renderH = Math.min(maxH, cElt.size.h);
                int yCurr = gravity == VAlign.TOP ? y
                        : gravity == VAlign.CENTER ? y + (maxH - renderH) / 2 : y + maxH - cElt.size.h;
                int renderW = Math.max(0, Math.min(maxW - (xCurr - x), cElt.size.w));
                if (renderW > 0 && renderH > 0) {
                    cElt.render(xCurr, yCurr, renderW, renderH, display);
                }
                xCurr += renderW;
            }
            xCurr = x + maxW - rightTotWidth;
            for (UIElement rElt : rightElements) {
                int renderH = Math.min(maxH, rElt.size.h);
                int yCurr = gravity == VAlign.TOP ? y
                        : gravity == VAlign.CENTER ? y + (maxH - renderH) / 2 : y + maxH - rElt.size.h;
                int renderW = Math.max(0, Math.min(maxW - (xCurr - x), rElt.size.w));
                if (renderW > 0 && renderH > 0) {
                    rElt.render(xCurr, yCurr, renderW, renderH, display);
                }
                xCurr += renderW;
            }
        }
    }

    @Override
    public String toString() {
        return "VLayout(" + leftElements + ", " + centerElements + ", " + rightElements + ")";
    }
}
