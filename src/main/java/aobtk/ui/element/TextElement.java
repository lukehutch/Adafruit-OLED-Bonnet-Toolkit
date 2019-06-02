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

import aobtk.font.Font;
import aobtk.i18n.Str;
import aobtk.oled.Display;
import aobtk.oled.Display.Highlight;
import aobtk.ui.measurement.Size;

public class TextElement extends UIElement {
    private volatile Str str;
    private volatile Font font;
    private volatile boolean drawWhite = true;
    volatile Highlight highlight;

    public TextElement(Font font, Str str) {
        this.str = str;
        this.font = font;
    }

    public TextElement(Font font, String string) {
        this.str = new Str(string);
        this.font = font;
    }

    public TextElement(Font font, Highlight highlight, Str str) {
        this.str = str;
        this.font = font;
        this.drawWhite = true;
        this.highlight = highlight;
    }

    public TextElement(Font font, Highlight highlight, String string) {
        this.str = new Str(string);
        this.font = font;
        this.drawWhite = true;
        this.highlight = highlight;
    }

    public TextElement(Font font, boolean drawWhite, Highlight highlight, Str str) {
        this.str = str;
        this.font = font;
        this.drawWhite = drawWhite;
        this.highlight = highlight;
    }

    public TextElement(Font font, boolean drawWhite, Highlight highlight, String string) {
        this.str = new Str(string);
        this.font = font;
        this.drawWhite = drawWhite;
        this.highlight = highlight;
    }

    public void setStr(Str str) {
        this.str = str;
    }

    public void setString(String string) {
        this.str = new Str(string);
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public void setDrawWhite(boolean drawWhite) {
        this.drawWhite = drawWhite;
    }

    public void setHighlight(Highlight highlight) {
        this.highlight = highlight;
    }

    @Override
    protected Size measure(int maxW, int maxH) {
        return size = hide ? Size.ZERO : new Size(font.getOuterWidth(str.toString()), font.getOuterHeight());
    }

    @Override
    protected void render(int x, int y, int maxW, int maxH, Display display) {
        if (!hide) {
            font.drawString(str.toString(), x, y, maxW, maxH, drawWhite, highlight, display);
        }
    }

    @Override
    public String toString() {
        return "TextElement(" + str + ")";
    }
}
