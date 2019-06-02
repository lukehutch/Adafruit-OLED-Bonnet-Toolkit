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
import java.util.stream.Collectors;

import aobtk.font.Font;
import aobtk.i18n.Str;
import aobtk.oled.Display;
import aobtk.oled.Display.Highlight;
import aobtk.ui.measurement.Size;

public class Menu extends UIElement {
    private Font font;
    private int spacing;
    private List<Str> items = new ArrayList<>();
    private Layout layout;
    private List<TextElement> textElements = new ArrayList<>();
    private volatile int selectedIdx = -1;
    private Highlight highlightType = Highlight.BLOCK;

    public Menu(Font font, int spacing, boolean hLayout, Str... items) {
        this.font = font;
        this.spacing = spacing;
        this.items = new ArrayList<>();
        this.layout = hLayout ? new HLayout() : new VLayout();
        for (Str item : items) {
            add(item);
        }
    }

    public void add(Str... items) {
        for (Str item : items) {
            this.items.add(item);
            int n = this.items.size();
            if (n > 1 && spacing > 0) {
                layout.addSpace(spacing);
            }
            TextElement textElement = new TextElement(font, item);
            textElements.add(textElement);
            layout.add(textElement);
            if (n == 1) {
                // Select first item added
                setSelectedIdx(0);
            }
        }
    }

    public void add(String... items) {
        for (String item : items) {
            add(new Str(item));
        }
    }

    public int getNumItems() {
        return textElements.size();
    }

    public int getSelectedIdx() {
        return selectedIdx;
    }

    public Str getSelectedItem() {
        return selectedIdx >= 0 && selectedIdx < items.size() ? items.get(selectedIdx) : null;
    }

    public void setSelectedIdx(int newSelectedIdx) {
        // Unhighlight old selection
        if (selectedIdx >= 0 && selectedIdx < textElements.size()) {
            textElements.get(selectedIdx).setHighlight(Highlight.NONE);
        }
        // Highlight new selection
        if (newSelectedIdx >= 0 && newSelectedIdx < textElements.size()) {
            textElements.get(newSelectedIdx).setHighlight(highlightType);
        }
        selectedIdx = newSelectedIdx;
    }

    public void setSelectedItem(Str selectedItem) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).equals(selectedItem)) {
                setSelectedIdx(i);
                break;
            }
        }
    }

    public void unsetSelection() {
        setSelectedIdx(-1);
    }

    public void incSelectedIdx() {
        if (selectedIdx == -1) {
            selectedIdx = textElements.size() - 1;
        } else if (selectedIdx < textElements.size() - 1) {
            setSelectedIdx(selectedIdx + 1);
        }
    }

    public void decSelectedIdx() {
        if (selectedIdx == -1) {
            selectedIdx = 0;
        } else if (selectedIdx > 0) {
            setSelectedIdx(selectedIdx - 1);
        }
    }

    public void clear() {
        selectedIdx = -1;
        items.clear();
        textElements.clear();
        layout.clear();
    }
    
    public void setHighlightType(Highlight highlightType) {
        this.highlightType = highlightType;
    }

    @Override
    protected Size measure(int maxW, int maxH) {
        return size = hide ? Size.ZERO : layout.measure(maxW, maxH);
    }

    @Override
    protected void render(int x, int y, int maxW, int maxH, Display display) {
        if (!hide) {
            layout.render(x, y, maxW, maxH, display);
        }
    }

    @Override
    public String toString() {
        List<String> strs = items.stream().map(Object::toString).collect(Collectors.toList());
        return "Menu(" + String.join(", ", strs) + ")";
    }
}
