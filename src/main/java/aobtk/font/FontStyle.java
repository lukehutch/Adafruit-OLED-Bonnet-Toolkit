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

import aobtk.oled.Display;
import aobtk.ui.measurement.Size;

public class FontStyle {
    private Font font;
    private boolean drawWhite = true;
    private CharSpacing charSpacing = CharSpacing.NOMINAL;
    private int padX = 0;
    private int padY = 0;
    private FontStyle.Highlight highlight = FontStyle.Highlight.NONE;

    public static enum Highlight {
        NONE, HALO, BLOCK;
    }

    public static enum CharSpacing {
        PROPORTIONAL, NOMINAL, MONOSPACE;
    }

    public FontStyle(Font font) {
        this.font = font;
    }

    public FontStyle(Font font, boolean drawWhite, CharSpacing charSpacing, int padX, int padY,
            FontStyle.Highlight highlight) {
        this(font);
        this.drawWhite = drawWhite;
        this.charSpacing = charSpacing;
        this.padX = padX;
        this.padY = padY;
        this.highlight = highlight;
    }

    /** Return a mutable copy of this {@link FontStyle}. */
    public FontStyle copy() {
        return new FontStyle(this.font, this.drawWhite, this.charSpacing, this.padX, this.padY, this.highlight);
    }

    /** Get the width of a space character (' ') in the font style. */
    public int spaceWidth() {
        // For packed char spacing, make the space 1/4 the row height, for nominal char spacing, make the space
        // half the row height, and for monospaced char spacing, make the space the same as the row height.
        return charSpacing == CharSpacing.PROPORTIONAL ? (font.getMaxCharHeight() + 3) / 4
                : charSpacing == CharSpacing.NOMINAL ? font.getMaxCharHeight() / 2 : font.getMaxCharHeight();
    }

    /**
     * Draw a character, cropping it to a maximum width and height, and return the (possibly cropped) width of the
     * character.
     */
    public int drawChar(char c, int x, int y, int maxW, int maxH, Display display) {
        int charWidth = 0;
        if (maxW > 0 && maxH > 0) {
            if (c == ' ') {
                charWidth = spaceWidth();
            } else {
                FontChar fontChar = font.getFontChar(c);
                // If chars are packed, remove any horizontal space at beginning of char
                int xShiftBack = charSpacing == CharSpacing.PROPORTIONAL ? fontChar.glyphPosX : 0;
                int drawnWidth = Math.min(maxW, fontChar.getWidth() - xShiftBack);
                if (display != null) {
                    fontChar.draw(x - xShiftBack, y, maxW + xShiftBack, maxH, this, display);
                }
                charWidth = charSpacing == CharSpacing.PROPORTIONAL ? drawnWidth
                        : charSpacing == CharSpacing.NOMINAL ? fontChar.nominalW : font.getMaxCharWidth();
            }
            if (display != null) {
                if (highlight == FontStyle.Highlight.BLOCK) {
                    display.invertBlock(x - 1, y - 1, charWidth + 2, Math.min(maxH, font.getMaxCharHeight()) + 2);
                }
            }
        }
        return charWidth;
    }

    /** Draw a character, and return the outer width of the character. */
    public int drawChar(char c, int x, int y, Display display) {
        int charWidth;
        if (c == ' ') {
            charWidth = spaceWidth();
        } else {
            FontChar fontChar = font.getFontChar(c);
            // If chars are packed, remove any horizontal space at beginning of char
            int xShiftBack = charSpacing == CharSpacing.PROPORTIONAL ? fontChar.glyphPosX : 0;
            int drawnWidth = fontChar.getWidth() - xShiftBack;
            if (display != null) {
                fontChar.draw(x - xShiftBack, y, this, display);
            }
            charWidth = charSpacing == CharSpacing.PROPORTIONAL ? drawnWidth
                    : charSpacing == CharSpacing.NOMINAL ? fontChar.nominalW : font.getMaxCharWidth();
        }
        if (display != null) {
            if (highlight == FontStyle.Highlight.BLOCK) {
                display.invertBlock(x - 1, y - 1, charWidth + 2, font.getMaxCharHeight() + 2);
            }
        }
        return charWidth;
    }

    /**
     * Draw a string (splitting into lines at the newline character), and return the width and height of the drawn
     * area, in pixels.
     */
    public Size drawString(String string, int x, int y, int maxW, int maxH, Display display) {
        int posX = x;
        int posY = y;
        int maxX = x + maxW;
        int maxY = y + maxH;
        int renderedW = 0;
        int renderedH = 0;
        int rowHeight = font.getMaxCharHeight();
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == '\n') {
                posX = x;
                posY += rowHeight + padY;
            } else {
                if (posX > x) {
                    posX += padX;
                }
                posX += drawChar(c, posX, posY, Math.max(0, maxX - posX), Math.max(0, maxY - posY), display);
                // Only non-whitespace characters affect max width and max height
                // (this handles a terminal space or newline without increasing the bounding box)
                if (c > ' ') {
                    renderedW = Math.max(renderedW, posX - x);
                    renderedH = Math.max(renderedH, posY + rowHeight - y);
                }
            }
        }
        return new Size(Math.min(renderedW, maxW), Math.min(renderedH, maxH));
    }

    /**
     * Draw a string (splitting into lines at the newline character), and return the width and height of the drawn
     * area, in pixels.
     */
    public Size drawString(String string, int x, int y, Display display) {
        int posX = x;
        int posY = y;
        int renderedW = 0;
        int renderedH = 0;
        int rowHeight = font.getMaxCharHeight();
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == '\n') {
                posX = x;
                posY += padY + rowHeight;
            } else {
                if (posX > x) {
                    posX += padX;
                }
                posX += drawChar(c, posX, posY, display);
                // Only non-whitespace characters affect max width and max height
                // (this handles a terminal space or newline without increasing the bounding box)
                if (c > ' ') {
                    renderedW = Math.max(renderedW, posX - x);
                    renderedH = Math.max(renderedH, posY + rowHeight - y);
                }
            }
        }
        return new Size(renderedW, renderedH);
    }

    /** Measure the size of a block of text, breaking at newlines. */
    public Size measure(String str) {
        return drawString(str, 0, 0, /* display = */ null);
    }

    /** Measure the size of a block of text, breaking at newlines. */
    public Size measure(String str, int maxW, int maxH) {
        return drawString(str, 0, 0, maxW, maxH, /* display = */ null);
    }

    /**
     * @return the highlight mode
     */
    public FontStyle.Highlight getHighlight() {
        return highlight;
    }

    /**
     * @param highlight the highlight mode to set
     * @return this (for method chaining)
     */
    public FontStyle setHighlight(FontStyle.Highlight highlight) {
        this.highlight = highlight;
        return this;
    }

    /**
     * @return the vertical padding between characters, in pixels
     */
    public int getPadY() {
        return padY;
    }

    /**
     * @param padY the vertical padding between characters, in pixels
     * @return this (for method chaining)
     */
    public FontStyle setPadY(int padY) {
        this.padY = padY;
        return this;
    }

    /**
     * @return the horizontal padding between characters, in pixels
     */
    public int getPadX() {
        return padX;
    }

    /**
     * @param padX the horizontal padding between characters, in pixels
     * @return this (for method chaining)
     */
    public FontStyle setPadX(int padX) {
        this.padX = padX;
        return this;
    }

    /** @return ther char spacing mode. */
    public CharSpacing getCharSpacing() {
        return charSpacing;
    }

    /**
     * @param charSpacing the new char spacing mode.
     * @return this (for method chaining)
     */
    public FontStyle setCharSpacing(CharSpacing charSpacing) {
        this.charSpacing = charSpacing;
        return this;
    }

    /**
     * @return if true, draw the font in white (else draw it in black)
     */
    public boolean getDrawColor() {
        return drawWhite;
    }

    /**
     * @param drawWhite if true, draw the font in white (else draw it in black)
     * @return this (for method chaining)
     */
    public FontStyle setDrawColor(boolean drawWhite) {
        this.drawWhite = drawWhite;
        return this;
    }

    /**
     * @return the font
     */
    public Font getFont() {
        return font;
    }

    /**
     * @param font the font to use
     * @return this (for method chaining)
     */
    public FontStyle setFont(Font font) {
        this.font = font;
        return this;
    }
}
