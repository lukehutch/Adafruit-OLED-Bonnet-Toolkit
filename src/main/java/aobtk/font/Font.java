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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import aobtk.font.FontStyle.CharSpacing;

public class Font {
    /** The characters in the font. */
    protected Map<Character, FontChar> charToFontChar;

    /** The height of the tallest character in the font. */
    private int maxCharHeight;

    /** The width of the widest character in the font. */
    private int maxCharWidth;

    /** The default font style for this font. */
    private final FontStyle defaultStyle = new FontStyle(this);

    // -----------------------------------------------------------------------------------------------------------

    // Lazy-loaded font singletons:
    // https://www.journaldev.com/1377/java-singleton-design-pattern-best-practices-examples#lazy-initialization

    /**
     * <a href="https://github.com/Dalgona/neodgm">NeoDGM</a> font, with support for
     * <a href="https://dalgona.github.io/neodgm/">Latin1, Hangeul, box drawing characters and Braille</a>.
     * 
     * This font is in proportional mode by default (so that characters take as little horizontal space as
     * possible), but also works well with {@link FontStyle#setCharSpacing(CharSpacing)} of
     * {@link CharSpacing#PROPORTIONAL}, which renders Hangeul characters with size 16x16, and other characters with
     * size 16x8.
     */
    public static Font NeoDGM_16() {
        return NeoDGM_16.FONT;
    }

    private static class NeoDGM_16 {
        private static final Font FONT = new Font("fonts/neodgm-16-font");
        static {
            FONT.defaultStyle.setCharSpacing(CharSpacing.PROPORTIONAL);
            FONT.defaultStyle.setPadX(1);
            // Don't pad Y, since 16 pixels high fits 4 rows on a 48 pixel row display.
        }
    }

    /** Basic 4x5 ASCII font (uppercase only). */
    public static Font PiOLED_4x5() {
        return PiOLED_4x5.FONT;
    }

    private static class PiOLED_4x5 {
        private static final Font FONT = new Font("fonts/pi-oled-4x5-font");
        static {
            // Take as little space as possible
            FONT.defaultStyle.setCharSpacing(CharSpacing.PROPORTIONAL);
            // This font is illegible if not padded in both X and Y by default.
            FONT.defaultStyle.setPadX(1);
            FONT.defaultStyle.setPadY(1);
        }
    }

    /** Basic 5x8 IBM-437 font. */
    public static Font PiOLED_5x8() {
        return PiOLED_5x8.FONT;
    }

    private static class PiOLED_5x8 {
        private static final Font FONT = new Font("fonts/pi-oled-5x8-font");
        static {
            // Take as little space as possible
            FONT.defaultStyle.setCharSpacing(CharSpacing.PROPORTIONAL);
            // Need to pad X, but this font is designed to be compact, and its vertical axis nicely divides
            // 48 pixel rows, so don't pad Y by default.
            FONT.defaultStyle.setPadX(1);
        }
    }

    /**
     * <a href="http://wenq.org/wqy2/index.cgi?BitmapSong_en">WenQueanYi Song</a> 12x12 font, supporting many
     * Unicode characters, and CJK unified ideographs, but not Hangeul (only {@link Font#WenQuanYi_16()} and
     * {@link Font#WenQuanYi_16_bold()} support Hangeul).
     */
    public static Font WenQuanYi_12() {
        return WenQuanYi_12.FONT;
    }

    private static class WenQuanYi_12 {
        private static final Font FONT = new Font("fonts/wqy-12-font");
        static {
            // Make this font proportional, otherwise Latin1 chars are spaced way too far apart.
            FONT.defaultStyle.setCharSpacing(CharSpacing.PROPORTIONAL);
            FONT.defaultStyle.setPadX(1);
            // 12 nicely divides 48, so don't pad in Y by default.
        }
    }

    /**
     * <a href="http://wenq.org/wqy2/index.cgi?BitmapSong_en">WenQueanYi Song</a> 12x12 bold font, supporting many
     * Unicode characters, and CJK unified ideographs, but not Hangeul (only {@link Font#WenQuanYi_16()} and
     * {@link Font#WenQuanYi_16_bold()} support Hangeul).
     */
    public static Font WenQuanYi_12_bold() {
        return WenQuanYi_12_bold.FONT;
    }

    private static class WenQuanYi_12_bold {
        private static final Font FONT = new Font("fonts/wqy-12-bold-font");
        static {
            // Make this font proportional, otherwise Latin1 chars are spaced way too far apart
            FONT.defaultStyle.setCharSpacing(CharSpacing.PROPORTIONAL);
            FONT.defaultStyle.setPadX(1);
            // 12 nicely divides 48, so don't pad in Y by default.
        }
    }

    /**
     * <a href="http://wenq.org/wqy2/index.cgi?BitmapSong_en">WenQueanYi Song</a> 13x13 font, supporting many
     * Unicode characters, and CJK unified ideographs, but not Hangeul (only {@link Font#WenQuanYi_16()} and
     * {@link Font#WenQuanYi_16_bold()} support Hangeul).
     */
    public static Font WenQuanYi_13() {
        return WenQuanYi_13.FONT;
    }

    private static class WenQuanYi_13 {
        private static final Font FONT = new Font("fonts/wqy-13-font");
        static {
            // Make this font proportional, otherwise Latin1 chars are spaced way too far apart
            FONT.defaultStyle.setCharSpacing(CharSpacing.PROPORTIONAL);
            FONT.defaultStyle.setPadX(1);
            // Don't pad in Y -- most chars have built-in visual padding
        }
    }

    /**
     * <a href="http://wenq.org/wqy2/index.cgi?BitmapSong_en">WenQueanYi Song</a> 13x13 bold font, supporting many
     * Unicode characters, and CJK unified ideographs, but not Hangeul (only {@link Font#WenQuanYi_16()} and
     * {@link Font#WenQuanYi_16_bold()} support Hangeul).
     */
    public static Font WenQuanYi_13_bold() {
        return WenQuanYi_13_bold.FONT;
    }

    private static class WenQuanYi_13_bold {
        private static final Font FONT = new Font("fonts/wqy-13-bold-font");
        static {
            // Make this font proportional, otherwise Latin1 chars are spaced way too far apart
            FONT.defaultStyle.setCharSpacing(CharSpacing.PROPORTIONAL);
            FONT.defaultStyle.setPadX(1);
            // Don't pad in Y -- most chars have built-in visual padding
        }
    }

    /**
     * <a href="http://wenq.org/wqy2/index.cgi?BitmapSong_en">WenQueanYi Song</a> 15x15 font, supporting many
     * Unicode characters, and CJK unified ideographs, but not Hangeul (only {@link Font#WenQuanYi_16()} and
     * {@link Font#WenQuanYi_16_bold()} support Hangeul).
     */
    public static Font WenQuanYi_15() {
        return WenQuanYi_15.FONT;
    }

    private static class WenQuanYi_15 {
        private static final Font FONT = new Font("fonts/wqy-15-font");
        static {
            // Make this font proportional, otherwise Latin1 chars are spaced way too far apart
            FONT.defaultStyle.setCharSpacing(CharSpacing.PROPORTIONAL);
            FONT.defaultStyle.setPadX(1);
            // Don't pad in Y -- most chars have built-in visual padding
        }
    }

    /**
     * <a href="http://wenq.org/wqy2/index.cgi?BitmapSong_en">WenQueanYi Song</a> 15x15 bold font, supporting many
     * Unicode characters, and CJK unified ideographs, but not Hangeul (only {@link Font#WenQuanYi_16()} and
     * {@link Font#WenQuanYi_16_bold()} support Hangeul).
     */
    public static Font WenQuanYi_15_bold() {
        return WenQuanYi_15_bold.FONT;
    }

    private static class WenQuanYi_15_bold {
        private static final Font FONT = new Font("fonts/wqy-15-bold-font");
        static {
            // Make this font proportional, otherwise Latin1 chars are spaced way too far apart
            FONT.defaultStyle.setCharSpacing(CharSpacing.PROPORTIONAL);
            FONT.defaultStyle.setPadX(1);
            // Don't pad in Y -- most chars have built-in visual padding
        }
    }

    public static Font WenQuanYi_16() {
        return WenQuanYi_16.FONT;
    }

    /**
     * <a href="http://wenq.org/wqy2/index.cgi?BitmapSong_en">WenQueanYi Song</a> 16x16 font, supporting many
     * Unicode characters, CJK unified ideographs, and Hangeul.
     */
    private static class WenQuanYi_16 {
        private static final Font FONT = new Font("fonts/wqy-16-font");
        static {
            // Make this font proportional, otherwise Latin1 chars are spaced way too far apart
            FONT.defaultStyle.setCharSpacing(CharSpacing.PROPORTIONAL);
            FONT.defaultStyle.setPadX(1);
            // Don't pad Y, since 16 pixels high fits 4 rows on a 48 pixel row display
        }
    }

    public static Font WenQuanYi_16_bold() {
        return WenQuanYi_16_bold.FONT;
    }

    /**
     * <a href="http://wenq.org/wqy2/index.cgi?BitmapSong_en">WenQueanYi Song</a> 16x16 bold font, supporting many
     * Unicode characters, CJK unified ideographs, and Hangeul.
     */
    private static class WenQuanYi_16_bold {
        private static final Font FONT = new Font("fonts/wqy-16-bold-font");
        static {
            // Make this font proportional, otherwise Latin1 chars are spaced way too far apart
            FONT.defaultStyle.setCharSpacing(CharSpacing.PROPORTIONAL);
            FONT.defaultStyle.setPadX(1);
            // Don't pad Y, since 16 pixels high fits 4 rows on a 48 pixel row display
        }
    }

    // -----------------------------------------------------------------------------------------------------------

    /**
     * Initialize the font from a map. All characters should already be present in the map, since the characters
     * will be measured immediately.
     */
    protected Font(Map<Character, FontChar> charToCharInfo) {
        this.charToFontChar = charToCharInfo;
        measureFont();
    }

    /** Load the font from disk. */
    public Font(String path) {
        try {
            URL url = Font.class.getClassLoader().getResource(path);
            if (url == null) {
                throw new FileNotFoundException(path);
            }
            try (InputStream is = url.openStream();
                    BufferedInputStream bis = new BufferedInputStream(is);
                    DataInputStream in = new DataInputStream(bis)) {
                int numChars = in.readInt();
                charToFontChar = new HashMap<>(numChars);
                int totPixBitsLen = 0;
                for (int i = 0; i < numChars; i++) {
                    char c = in.readChar();
                    int x = in.readByte() & 0xff;
                    int y = in.readByte() & 0xff;
                    int w = in.readByte() & 0xff;
                    int h = in.readByte() & 0xff;
                    int nominalWidth = in.readByte() & 0xff;
                    int byteOffset = in.readInt();
                    FontChar charInfo = new FontChar(x, y, w, h, nominalWidth, byteOffset);
                    charToFontChar.put(c, charInfo);
                    totPixBitsLen += charInfo.getCharPixBitsLen();
                }
                byte[] allPixBits = new byte[totPixBitsLen];
                for (int off = 0, remaining = totPixBitsLen; remaining > 0;) {
                    int numRead = in.read(allPixBits, off, remaining);
                    if (numRead <= 0) {
                        throw new IOException("Premature EOF");
                    }
                    remaining -= numRead;
                }
                for (FontChar charInfo : charToFontChar.values()) {
                    charInfo.setPixBits(allPixBits);
                }
            }
            measureFont();
        } catch (IOException e) {
            throw new RuntimeException("Could not load font " + path, e);
        }
    }

    protected void measureFont() {
        for (FontChar fontChar : charToFontChar.values()) {
            fontChar.measure();
            maxCharWidth = Math.max(maxCharWidth, fontChar.measuredW);
            maxCharHeight = Math.max(maxCharHeight, fontChar.measuredH);
        }
    }

    public int getMaxCharHeight() {
        return maxCharHeight;
    }

    public int getMaxCharWidth() {
        return maxCharWidth;
    }

    public FontChar getFontChar(char c) {
        FontChar fontChar = charToFontChar.get(c);
        if (fontChar == null) {
            fontChar = charToFontChar.get('?');
            if (fontChar == null) {
                throw new RuntimeException(
                        "Could not render character '" + c + "' in font, and couldn't find fallback character '?'");
            }
        }
        return fontChar;
    }

    public FontStyle newStyle() {
        return defaultStyle.copy();
    }
}
