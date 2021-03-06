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

import aobtk.font.FontStyle;
import aobtk.i18n.Str;
import aobtk.oled.Display;
import aobtk.ui.measurement.Size;

public class TextElement extends UIElement {
    private volatile Str str;
    private volatile FontStyle fontStyle;

    public TextElement(FontStyle fontStyle, Str str) {
        this.str = str;
        this.fontStyle = fontStyle;
    }

    public TextElement(FontStyle fontStyle, String string) {
        this.str = new Str(string);
        this.fontStyle = fontStyle;
    }

    public void setStr(Str str) {
        this.str = str;
    }

    public void setString(String string) {
        this.str = new Str(string);
    }

    public void setFontStyle(FontStyle fontStyle) {
        this.fontStyle = fontStyle;
    }

    public FontStyle getFontStyle() {
        return fontStyle;
    }
    
    @Override
    protected Size measure(int maxW, int maxH) {
        return size = hide ? Size.ZERO : fontStyle.measure(str.toString());
    }

    @Override
    protected void render(int x, int y, int maxW, int maxH, Display display) {
        if (!hide) {
            fontStyle.drawString(str.toString(), x, y, maxW, maxH, display);
        }
    }

    @Override
    public String toString() {
        return "TextElement(" + str + ")";
    }
}
