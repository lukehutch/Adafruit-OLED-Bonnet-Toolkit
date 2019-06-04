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

public class FontChar {
    /** X-offset to plot character's defined pixels at, relative to character start position. */
    public int glyphPosX;

    /** Y-offset to plot character's defined pixels at, relative to character start position. */
    public int glyphPosY;

    /** Width of pixels defined in character. */
    public final int glyphW;

    /** Height of pixels defined in character. */
    public final int glyphH;

    /** The nominal display width of the character. */
    public int nominalW;

    /** The width to the rightmost drawn pixel. */
    int measuredW;

    /** The height to the bottommost drawn pixel. */
    int measuredH;

    /** Packed pixels, row-major order, big endian. */
    private byte[] charPixBits;

    /** Start index of character data in charPixBits. */
    private int charPixBitsStartIdx;

    public FontChar(int glyphPosX, int glyphPosY, int glyphW, int glyphH, int nominalW, int charPixBitsStartIdx) {
        this.glyphPosX = glyphPosX;
        this.glyphPosY = glyphPosY;
        this.glyphW = glyphW;
        this.glyphH = glyphH;
        this.nominalW = nominalW;
        this.charPixBitsStartIdx = charPixBitsStartIdx;
    }

    /** Initialize a character from a larger int[] pixel grid, with one pixel per int. */
    public FontChar(int glyphPosX, int glyphPosY, int glyphW, int glyphH, int nominalW, int[] pixels, int stride) {
        super();

        // Find outermost pixels of glyph. This allows the glyph to be compressed by storing only the contents
        // of the innermost bounding box of the glyph. It also allows the font to be displayed as proportional
        // (removing spacing on the left and right of the char), even if it wasn't defined as such.
        boolean firstPixelFound = false;
        int minX = 0;
        int maxX = 0;
        int minY = 0;
        int maxY = 0;
        for (int y = 0; y < glyphH; y++) {
            for (int x = 0; x < glyphW; x++) {
                boolean pixelSet = pixels[x + y * stride] != 0;
                if (pixelSet) {
                    if (!firstPixelFound || x < minX) {
                        minX = x;
                    }
                    if (!firstPixelFound || x > maxX) {
                        maxX = x;
                    }
                    if (!firstPixelFound || y < minY) {
                        minY = y;
                    }
                    if (!firstPixelFound || y > maxY) {
                        maxY = y;
                    }
                    firstPixelFound = true;
                }
            }
        }

        // Add first pixel row/column position to glyph position
        this.glyphPosX = glyphPosX + minX;
        this.glyphPosY = glyphPosY + minY;
        this.glyphW = maxX - minX + (firstPixelFound ? 1 : 0);
        this.glyphH = maxY - minY + (firstPixelFound ? 1 : 0);
        this.nominalW = nominalW;
        this.charPixBits = new byte[getCharPixBitsLen()];

        // Pack pixels into bit array
        for (int y = minY, yy = y + this.glyphH; y < yy; y++) {
            for (int x = minX, xx = x + this.glyphW; x < xx; x++) {
                boolean pixelSet = pixels[x + y * stride] != 0;
                if (pixelSet) {
                    int absBitIdx = (x - minX) + (y - minY) * this.glyphW;
                    int byteIdx = absBitIdx >> 3;
                    int relBitIdx = 7 - (absBitIdx & 7);
                    charPixBits[byteIdx] |= (1 << relBitIdx);
                }
            }
        }
    }

    public byte[] getCharPixBits() {
        return charPixBits;
    }

    public int getCharPixBitsLen() {
        return (glyphW * glyphH + 7) / 8;
    }

    public void setPixBits(byte[] pixBits) {
        this.charPixBits = pixBits;
    }

    public int getCharPixBitsStartIdx() {
        return charPixBitsStartIdx;
    }

    public int getWidth() {
        return measuredW;
    }

    /** Measure width and height to rightmost and bottom-most drawn pixel. */
    void measure() {
        int numPix = glyphW * glyphH;
        for (int bitIdx = 0; bitIdx < numPix; bitIdx++) {
            byte b = charPixBits[(bitIdx >> 3) + charPixBitsStartIdx];
            boolean bit = (b & (1 << 7 - (bitIdx & 7))) != 0;
            if (bit) {
                int pix_offx = glyphPosX + bitIdx % glyphW;
                int pix_offy = glyphPosY + bitIdx / glyphW;
                measuredW = Math.max(measuredW, pix_offx + 1);
                measuredH = Math.max(measuredH, pix_offy + 1);
            }
        }
    }

    public void draw(int x, int y, int maxW, int maxH, FontStyle fontStyle, Display display) {
        int numPix = glyphW * glyphH;
        for (int bitIdx = 0; bitIdx < numPix; bitIdx++) {
            byte b = charPixBits[(bitIdx >> 3) + charPixBitsStartIdx];
            boolean bit = (b & (1 << 7 - (bitIdx & 7))) != 0;
            if (bit) {
                int pix_offx = glyphPosX + bitIdx % glyphW;
                int pix_offy = glyphPosY + bitIdx / glyphW;
                if (pix_offx < maxW && pix_offy < maxH) {
                    int pix_x = x + pix_offx;
                    int pix_y = y + pix_offy;
                    display.setPixel(pix_x, pix_y, fontStyle.getDrawColor(), fontStyle.getHighlight());
                }
            }
        }
    }

    public void draw(int x, int y, FontStyle fontStyle, Display display) {
        int numPix = glyphW * glyphH;
        for (int bitIdx = 0; bitIdx < numPix; bitIdx++) {
            byte b = charPixBits[(bitIdx >> 3) + charPixBitsStartIdx];
            boolean bit = (b & (1 << 7 - (bitIdx & 7))) != 0;
            if (bit) {
                int pix_offx = glyphPosX + bitIdx % glyphW;
                int pix_offy = glyphPosY + bitIdx / glyphW;
                int pix_x = x + pix_offx;
                int pix_y = y + pix_offy;
                display.setPixel(pix_x, pix_y, fontStyle.getDrawColor(), fontStyle.getHighlight());
            }
        }
    }

    public void printGrid() {
        System.out.print('┌');
        for (int i = 0; i < glyphW; i++) {
            System.out.print("──");
        }
        System.out.println('┐');
        for (int r = 0; r < glyphH; r++) {
            System.out.print('│');
            for (int c = 0; c < glyphW; c++) {
                int bitIdx = c + r * glyphW;
                byte b = charPixBits[(bitIdx >> 3) + charPixBitsStartIdx];
                boolean bit = (b & (1 << (7 - (bitIdx & 7)))) != 0;
                System.out.print(bit ? "██" : "  ");
            }
            System.out.println('│');
        }
        System.out.print('└');
        for (int i = 0; i < glyphW; i++) {
            System.out.print("──");
        }
        System.out.println('┘');
    }
}