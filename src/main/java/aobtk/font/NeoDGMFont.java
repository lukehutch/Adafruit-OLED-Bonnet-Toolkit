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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import aobtk.oled.Display;
import aobtk.oled.Display.Highlight;

class NeoDGMFont extends Font {
    /**
     * Mapping from character to pixel bytes (1bpp), in column-major order, with two bytes per column.
     */
    private static final Map<Character, byte[]> charToBytes = new HashMap<>();

    private static final int PADDING_X = 0;
    private static final int PADDING_Y = 0;

    static final Font FONT_NEODGM = new NeoDGMFont();

    private NeoDGMFont() {
        super(/* height = */ 16, /* outerHeight = */ 16 + PADDING_Y);

        // Read font file created by NeoDGMFontCreator.java
        URL url = NeoDGMFont.class.getClassLoader().getResource("neodgm-font");
        try (InputStream is = url.openStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                DataInputStream in = new DataInputStream(bis)) {
            int numChars = in.readInt();
            List<Entry<Character, Entry<Integer, Integer>>> chars = new ArrayList<>();
            int totLen = 0;
            for (int i = 0; i < numChars; i++) {
                Character c = Character.valueOf(in.readChar());
                int offset = in.readInt();
                int len = in.readByte();
                chars.add(new SimpleEntry<>(c, new SimpleEntry<>(offset, len)));
                totLen += len;
            }
            byte[] allBytes = new byte[totLen];
            for (int off = 0, remaining = totLen; remaining > 0;) {
                int numRead = in.read(allBytes, off, remaining);
                if (numRead <= 0) {
                    throw new IOException("Premature EOF");
                }
                remaining -= numRead;
            }
            for (Entry<Character, Entry<Integer, Integer>> ent : chars) {
                Character c = ent.getKey();
                int offset = ent.getValue().getKey();
                int len = ent.getValue().getValue();
                byte[] bytes = new byte[len];
                for (int i = 0; i < len; i++) {
                    bytes[i] = allBytes[offset + i];
                }
                charToBytes.put(c, bytes);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] getCharBytes(char c) {
        byte[] bytes = charToBytes.get(c);
        if (bytes == null) {
            bytes = charToBytes.get('?');
            if (bytes == null) {
                throw new RuntimeException("Could not find fallback char '?'");
            }
        }
        return bytes;
    }

    private static boolean getCharPixel(byte[] bytes, int x, int y) {
        return (bytes[x * 2 + (y >> 3)] & (1 << (y & 7))) != 0;
    }

    @Override
    public int getWidth(char chr) {
        return getCharBytes(chr).length / 2;
    }

    @Override
    public int getOuterWidth(char chr) {
        return getWidth(chr) + PADDING_X;
    }

    @Override
    protected int renderChar(char c, int x, int y, int maxW, int maxH, boolean on, Highlight highlight,
            Display display) {
        byte[] bytes = getCharBytes(c);
        int w = bytes.length / 2;
        for (int r = 0; r < 16 && r < maxH; r++) {
            int pix_y = y + r;
            for (c = 0; c < w && c < maxW; c++) {
                boolean bit = getCharPixel(bytes, c, r);
                if (bit) {
                    int pix_x = x + c;
                    display.setPixel(pix_x, pix_y, on, highlight);
                }
            }
        }
        return w + PADDING_X;
    }
}