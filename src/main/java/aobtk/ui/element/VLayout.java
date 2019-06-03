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
import aobtk.ui.element.HLayout.HAlign;
import aobtk.ui.measurement.Size;

public class VLayout extends HVLayout {
    private List<UIElement> topElements = new ArrayList<>();
    private int topTotHeight;
    private List<UIElement> centerElements = new ArrayList<>();
    private int centerTotHeight;
    private List<UIElement> bottomElements = new ArrayList<>();
    private int bottomTotHeight;
    private HAlign gravity = HAlign.CENTER;

    public static enum VAlign {
        TOP, CENTER, BOTTOM
    };

    public VLayout() {
    }

    public VLayout(UIElement... elements) {
        for (UIElement elt : elements) {
            add(elt);
        }
    }

    @Override
    public void clear() {
        topElements.clear();
        centerElements.clear();
        bottomElements.clear();
    }

    public void add(UIElement uiElement, VAlign vAlign) {
        (vAlign == VAlign.TOP ? topElements : vAlign == VAlign.BOTTOM ? bottomElements : centerElements)
                .add(uiElement);
    }

    @Override
    public void add(UIElement uiElement) {
        add(uiElement, VAlign.CENTER);
    }

    public void addSpace(int h, VAlign vAlign) {
        add(new Spacer(0, h), vAlign);
    }

    @Override
    public void addSpace(int h) {
        addSpace(h, VAlign.CENTER);
    }

    /** Set horizontal layout gravity for items within VLayout that don't fill all horizontal space. */
    public void setGravity(HAlign gravity) {
        this.gravity = gravity;
    }

    @Override
    public Size measure(int maxW, int maxH) {
        if (hide) {
            return size = Size.ZERO;
        }
        int eltMaxW = 0;
        int remainingMaxH = maxH;
        topTotHeight = 0;
        for (UIElement elt : topElements) {
            elt.measure(maxW, remainingMaxH);
            topTotHeight += elt.size.h;
            remainingMaxH = Math.max(0, remainingMaxH - elt.size.h);
            eltMaxW = Math.max(eltMaxW, elt.size.w);
        }
        bottomTotHeight = 0;
        for (UIElement elt : bottomElements) {
            elt.measure(maxW, remainingMaxH);
            bottomTotHeight += elt.size.h;
            remainingMaxH = Math.max(0, remainingMaxH - elt.size.h);
            eltMaxW = Math.max(eltMaxW, elt.size.w);
        }
        centerTotHeight = 0;
        for (UIElement elt : centerElements) {
            elt.measure(maxW, remainingMaxH);
            centerTotHeight += elt.size.h;
            remainingMaxH = Math.max(0, remainingMaxH - elt.size.h);
            eltMaxW = Math.max(eltMaxW, elt.size.w);
        }
        return size = new Size(Math.min(eltMaxW, maxW),
                Math.min(topTotHeight + centerTotHeight + bottomTotHeight, maxW));
    };

    @Override
    protected void render(int x, int y, int maxW, int maxH, Display display) {
        if (!hide) {
            int yCurr = y;
            for (UIElement tElt : topElements) {
                int renderW = Math.min(maxW, tElt.size.w);
                int xCurr = gravity == HAlign.LEFT ? x
                        : gravity == HAlign.CENTER ? x + (maxW - renderW) / 2 : x + maxW - tElt.size.w;
                int renderH = Math.max(0, Math.min(maxH - (yCurr - y), tElt.size.h));
                if (renderW > 0 && renderH > 0) {
                    tElt.render(xCurr, yCurr, renderW, renderH, display);
                }
                yCurr += renderH;
            }
            yCurr = y + (maxH - topTotHeight - centerTotHeight - bottomTotHeight) / 2 + topTotHeight;
            for (UIElement cElt : centerElements) {
                int renderW = Math.min(maxW, cElt.size.w);
                int xCurr = gravity == HAlign.LEFT ? x
                        : gravity == HAlign.CENTER ? x + (maxW - renderW) / 2 : x + maxW - cElt.size.w;
                int renderH = Math.max(0, Math.min(maxH - (yCurr - y), cElt.size.h));
                if (renderW > 0 && renderH > 0) {
                    cElt.render(xCurr, yCurr, renderW, renderH, display);
                }
                yCurr += renderH;
            }
            yCurr = y + maxH - bottomTotHeight;
            for (UIElement bElt : bottomElements) {
                int renderW = Math.min(maxW, bElt.size.w);
                int xCurr = gravity == HAlign.LEFT ? x
                        : gravity == HAlign.CENTER ? x + (maxW - renderW) / 2 : x + maxW - bElt.size.w;
                int renderH = Math.max(0, Math.min(maxH - (yCurr - y), bElt.size.h));
                if (renderW > 0 && renderH > 0) {
                    bElt.render(xCurr, yCurr, renderW, renderH, display);
                }
                yCurr += renderH;
            }
        }
    }

    @Override
    public String toString() {
        return "VLayout(" + topElements + ", " + centerElements + ", " + bottomElements + ")";
    }
}
