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
import java.util.Arrays;
import java.util.List;

import aobtk.oled.Display;
import aobtk.ui.measurement.Size;

public class TableLayout extends UIElement {
    private int spacingY;
    private int spacingX;
    private List<List<UIElement>> tableElements = new ArrayList<>();

    private Size tableDim;
    private List<Integer> rowHeights;
    private List<Integer> colWidths;

    public static enum Align {
        NW, N, NE, W, CENTER, E, SW, S, SE
    };

    private Align tableGravity = Align.CENTER;
    private List<Align> columnGravity;
    private List<Align> rowGravity;

    public static enum HAlign {
        LEFT, CENTER, RIGHT
    }

    public TableLayout(int spacingX, int spacingY) {
        this.spacingY = spacingY;
        this.spacingX = spacingX;
    }

    public TableLayout(int spacingX, int spacingY, List<List<UIElement>> tableElements) {
        this(spacingX, spacingY);
        this.tableElements = tableElements;
    }

    protected TableLayout(int spacingX, int spacingY, UIElement[]... tableElements) {
        this(spacingX, spacingY);
        for (UIElement[] rowElements : tableElements) {
            this.tableElements.add(Arrays.asList(rowElements));
        }
    }

    public TableLayout() {
        this(0, 0);
    }

    public TableLayout(List<List<UIElement>> tableElements) {
        this(0, 0, tableElements);
    }

    public TableLayout(UIElement[]... tableElements) {
        this(0, 0, tableElements);
    }

    public void clear() {
        tableElements.clear();
    }

    public void add(int cellX, int cellY, UIElement uiElement) {
        while (tableElements.size() < cellY + 1) {
            tableElements.add(new ArrayList<>());
        }
        List<UIElement> rowElements = tableElements.get(cellY);
        while (rowElements.size() < cellX + 1) {
            rowElements.add(null);
        }
        rowElements.set(cellX, uiElement);
    }

    /** Add a row of UIElements at the specified row in the table. */
    public void add(int rowY, UIElement... uiElements) {
        for (int x = 0; x < uiElements.length; x++) {
            add(x, rowY, uiElements[x]);
        }
    }

    /** Add a row of UIElements at the bottom of the table. */
    public void add(UIElement... uiElements) {
        add(tableElements.size(), uiElements);
    }

    public void addSpace(int cellX, int cellY, int w, int h) {
        add(cellX, cellY, new Spacer(w, h));
    }

    /** Set the default layout gravity for the whole table, for items that don't fill an entire table cell. */
    public void setGravity(Align gravity) {
        this.tableGravity = gravity;
    }

    /**
     * Set the default layout gravity for a column, for items that don't fill an entire table cell. Column gravity
     * overrides row gravity and table gravity settings.
     */
    public void setColumnGravity(int columnX, Align gravity) {
        if (columnGravity == null) {
            columnGravity = new ArrayList<>();
        }
        while (columnGravity.size() < columnX + 1) {
            columnGravity.add(null);
        }
        columnGravity.set(columnX, gravity);
    }

    /**
     * Set the default layout gravity for a row, for items that don't fill an entire table cell. Row gravity
     * overrides table gravity settings.
     */
    public void setRowGravity(int rowY, Align gravity) {
        if (rowGravity == null) {
            rowGravity = new ArrayList<>();
        }
        while (rowGravity.size() < rowY + 1) {
            rowGravity.add(null);
        }
        rowGravity.set(rowY, gravity);
    }

    /** Get the number of columns and rows in the table. */
    public Size getTableDim() {
        int maxRowWidth = 0;
        for (List<UIElement> row : tableElements) {
            maxRowWidth = Math.max(maxRowWidth, row.size());
        }
        return new Size(maxRowWidth, tableElements.size());
    }

    /** Get the table element at the given column and row. */
    public UIElement get(int cellX, int cellY) {
        if (cellY >= 0 && cellY < tableElements.size()) {
            List<UIElement> rowElements = tableElements.get(cellY);
            if (cellX >= 0 && cellX < rowElements.size()) {
                return rowElements.get(cellX);
            }
        }
        return null;
    }

    @Override
    public Size measure(int maxW, int maxH) {
        if (hide) {
            return size = Size.ZERO;
        }

        tableDim = getTableDim();
        if (rowHeights == null || rowHeights.size() < tableDim.h) {
            rowHeights = new ArrayList<>(tableDim.h);
            for (int row = 0; row < tableDim.h; row++) {
                rowHeights.add(Integer.valueOf(0));
            }
        }
        if (colWidths == null || colWidths.size() < tableDim.w) {
            colWidths = new ArrayList<>(tableDim.w);
            for (int col = 0; col < tableDim.w; col++) {
                colWidths.add(Integer.valueOf(0));
            }
        }

        // Measure max column widths and row heights
        int remainingMaxW = maxW;
        for (int col = 0; col < tableDim.w; col++) {
            int maxWidthForCol = 0;
            for (int row = 0; row < tableDim.h; row++) {
                UIElement elt = get(col, row);
                if (elt != null) {
                    elt.measure(remainingMaxW, maxH);
                    maxWidthForCol = Math.max(maxWidthForCol, elt.size.w);
                }
            }
            colWidths.set(col, maxWidthForCol);
            remainingMaxW = Math.max(0, remainingMaxW - maxWidthForCol);
            if (col < tableDim.w - 1) {
                remainingMaxW = Math.max(0, remainingMaxW - spacingX);
            }
        }
        int remainingMaxH = maxH;
        for (int row = 0; row < tableDim.h; row++) {
            int maxHeightForRow = 0;
            for (int col = 0; col < tableDim.w; col++) {
                UIElement elt = get(col, row);
                if (elt != null) {
                    elt.measure(maxW, remainingMaxH);
                    maxHeightForRow = Math.max(maxHeightForRow, elt.size.h);
                }
            }
            rowHeights.set(row, maxHeightForRow);
            remainingMaxH = Math.max(0, remainingMaxH - maxHeightForRow);
            if (row < tableDim.h - 1) {
                remainingMaxH = Math.max(0, remainingMaxH - spacingY);
            }
        }

        // Go back and re-measure elements, constraining them to max row height and max col width
        for (int row = 0; row < tableDim.h; row++) {
            int rowHeight = rowHeights.get(row);
            for (int col = 0; col < tableDim.w; col++) {
                UIElement elt = get(col, row);
                if (elt != null) {
                    int colWidth = colWidths.get(col);
                    elt.measure(colWidth, rowHeight);
                }
            }
        }

        // Get size of whole table
        return size = new Size(maxW - remainingMaxW, maxH - remainingMaxH);
    };

    @Override
    protected void render(int x, int y, int maxW, int maxH, Display display) {
        if (!hide) {
            int yCurr = y;
            for (int row = 0; row < tableDim.h; row++) {
                int xCurr = x;
                int rowHeight = rowHeights.get(row);
                for (int col = 0; col < tableDim.w; col++) {
                    UIElement elt = get(col, row);
                    if (elt != null) {
                        int colWidth = colWidths.get(col);
                        Align eltGravity = null;
                        if (columnGravity != null && col < columnGravity.size()) {
                            eltGravity = columnGravity.get(col);
                        }
                        if (eltGravity == null && rowGravity != null && row < rowGravity.size()) {
                            eltGravity = rowGravity.get(row);
                        }
                        if (eltGravity == null) {
                            eltGravity = tableGravity;
                        }

                        int cropW = Math.min(colWidth, elt.size.w);
                        int cropH = Math.min(rowHeight, elt.size.h);
                        int leftoverW = Math.max(0, colWidth - elt.size.w);
                        int leftoverH = Math.max(0, rowHeight - elt.size.h);
                        int xDisp = xCurr;
                        if (eltGravity == Align.N || eltGravity == Align.CENTER || eltGravity == Align.S) {
                            xDisp += leftoverW / 2;
                        } else if (eltGravity == Align.NE || eltGravity == Align.E || eltGravity == Align.SE) {
                            xDisp += leftoverW;
                        }
                        int yDisp = yCurr;
                        if (eltGravity == Align.W || eltGravity == Align.CENTER || eltGravity == Align.E) {
                            yDisp += leftoverH / 2;
                        } else if (eltGravity == Align.SW || eltGravity == Align.S || eltGravity == Align.SE) {
                            yDisp += leftoverH;
                        }
                        elt.render(xDisp, yDisp, cropW, cropH, display);
                        xCurr += colWidth;
                        if (col < tableDim.w - 1 && xCurr - x < maxW) {
                            xCurr += spacingX;
                        }
                    }
                }
                yCurr += rowHeight;
                if (row < tableDim.h - 1 && yCurr - y < maxH) {
                    yCurr += spacingY;
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Table(" + tableElements + ")";
    }
}
