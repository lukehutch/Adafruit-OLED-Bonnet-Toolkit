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
package aobtk.font;

import java.util.List;

import aobtk.oled.Display;
import aobtk.oled.Display.Highlight;
import aobtk.ui.measurement.Size;

/**
 *
 * @author Luke Hutchison
 * 
 */
public abstract class Font {

    protected final int height, outerHeight;

    public static final Font FONT_4X5 = MonospaceFont.FONT_4X5;
    public static final Font FONT_5X8 = MonospaceFont.FONT_5X8;
    public static final Font FONT_NEODGM = NeoDGMFont.FONT_NEODGM;

    protected Font(int height, int outerHeight) {
        this.height = height;
        this.outerHeight = outerHeight;
    }

    public abstract int getWidth(char chr);

    public abstract int getOuterWidth(char chr);

    public int getOuterWidth(String str) {
        int outerWidth = 0;
        for (int i = 0; i < str.length(); i++) {
            outerWidth += getOuterWidth(str.charAt(i));
        }
        return outerWidth;
    }

    public int getHeight() {
        return height;
    }

    public int getOuterHeight() {
        return outerHeight;
    }

    /** Draw a character, and return the width of the character, including any padding. */
    protected abstract int renderChar(char c, int x, int y, int maxW, int maxH, boolean on, Highlight highlight,
            Display display);

    /** Draw a character, and return outer width of the character. */
    public int drawChar(char c, int x, int y, int maxW, int maxH, boolean on, Highlight highlight,
            Display display) {
        if (maxW > 0 && maxH > 0) {
            int width = renderChar(c, x, y, maxW, maxH, on, highlight, display);
            if (highlight == Highlight.BLOCK) {
                display.invertBlock(x - 1, y - 1, Math.min(maxW, getWidth(c)) + 2, Math.min(maxH, getHeight()) + 2);
            }
            return width;
        } else {
            return 0;
        }
    }

    /** Draw a character, and return the outer width of the character. */
    public int drawChar(char c, int x, int y, Display display) {
        return drawChar(c, x, y, getWidth(c), getHeight(), /* on = */ true, Highlight.NONE, display);
    }

    /**
     * Draw a string (splitting into lines at the newline character), and return the width and height of the drawn
     * area, in pixels.
     */
    public Size drawString(String string, int x, int y, int maxW, int maxH, boolean on, Highlight highlight,
            Display display) {
        int posX = x;
        int posY = y;
        int maxX = x + maxW;
        int maxY = y + maxH;
        int renderW = 0;
        int renderH = 0;
        int outerHeight = getOuterHeight();
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == '\n') {
                posX = x;
                posY += outerHeight;
            } else {
                posX += drawChar(c, posX, posY, Math.max(0, maxX - posX), Math.max(0, maxY - posY), on, highlight,
                        display);
                // Only non-whitespace characters affect max width and max height
                // (this handles a terminal space or newline without increasing the bounding box)
                if (c != ' ') {
                    renderW = Math.max(renderW, posX - x);
                    renderH = Math.max(renderH, posY + outerHeight - y);
                }
            }
        }
        return new Size(Math.min(renderW, maxW), Math.min(renderH, maxH));
    }

    /**
     * Draw a string (splitting into lines at the newline character), and return the width and height of the drawn
     * area, in pixels.
     */
    public Size drawString(String string, int x, int y, boolean on, Highlight highlight, Display display) {
        return drawString(string, x, y, getOuterWidth(string), getHeight(), on, highlight, display);
    }

    /**
     * Draw a string (splitting into lines at the newline character), and return the width and height of thed rawn
     * area, in pixels.
     */
    public Size drawString(String string, int x, int y, Display display) {
        return drawString(string, x, y, getOuterWidth(string), getHeight(), /* on = */ true, Highlight.NONE,
                display);
    }

    /**
     * Draw a list of strings, one per line, additionally splitting each string into lines at newline characters,
     * and return the width and height of the drawn area, in pixels.
     */
    public Size drawLines(List<String> lines, int x, int y, boolean on, Highlight highlight, Display display) {
        int posY = y;
        int maxWidth = 0;
        int maxY = 0;
        int outerHeight = getOuterHeight();
        for (String line : lines) {
            Size size = drawString(line, x, posY, display);
            maxWidth = Math.max(maxWidth, size.w);
            // Handle blank lines (if the extent was zero pixels high, set to outerHeight)
            int lineHeight = Math.max(size.w, outerHeight);
            posY += lineHeight;
            if (!line.isBlank()) {
                maxY = posY;
            }
        }
        return new Size(maxWidth, maxY - y);
    }

    /**
     * Draw a list of strings, one per line, additionally splitting each string into lines at newline characters,
     * and return the width and height of the drawn area, in pixels.
     */
    public Size drawLines(List<String> lines, int x, int y, Display display) {
        return drawLines(lines, x, y, /* on = */ true, Highlight.NONE, display);
    }
}
