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
package fontimport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aobtk.font.FontChar;

class ImportWQYFonts {

    public static Map<String, Character> loadFontGlyphNameMap() throws IOException {
        Map<String, Character> map = new HashMap<>();
        // This map was derived from http://www.jdawiseman.com/papers/trivia/character-entities.html
        // By adding HTML entity to character mappings, then overwriting with Postscript to character mappings,
        // and adding the unicode character codepoint names used by the WQY fonts. 
        for (String line : Files.readAllLines(Paths.get("src/main/java/fontimport/wqy-charname-to-char-map"))) {
            if (!line.isBlank()) {
                String[] parts = line.split("\t");
                if (parts.length == 2) {
                    map.put(parts[0], parts[1].charAt(0));
                }
            }
        }
        return map;
    }

    public static class Alignment {
        private static final int ALIGNMENT_RADIUS = 4;
        private static final int N = ALIGNMENT_RADIUS * 2 + 1;
        int[] alignmentCountX = new int[N];
        int[] alignmentCountY = new int[N];
        List<FontChar> chars = new ArrayList<>();

        public void add(FontChar c, int fontDim) {
            chars.add(c);
            if (c.glyphPosX < 0) {
                alignmentCountX[c.glyphPosX + ALIGNMENT_RADIUS]++;
            } else if (c.glyphPosX + c.glyphW > fontDim) {
                alignmentCountX[c.glyphPosX + c.glyphW - fontDim + ALIGNMENT_RADIUS]++;
            } else {
                alignmentCountX[ALIGNMENT_RADIUS]++;
            }
            if (c.glyphPosY < 0) {
                alignmentCountY[c.glyphPosY + ALIGNMENT_RADIUS]++;
            } else if (c.glyphPosY + c.glyphH > fontDim) {
                alignmentCountY[c.glyphPosY + c.glyphH - fontDim + ALIGNMENT_RADIUS]++;
            } else {
                alignmentCountY[ALIGNMENT_RADIUS]++;
            }
        }

        @Override
        public String toString() {
            return Arrays.toString(alignmentCountX) + "\n" + Arrays.toString(alignmentCountY);
        }

        public void align(int fontDim) {
            // Find alignment that brings the majority of characters within the (fontDim x fontDim) grid
            int maxAlignmentCountX = 0;
            int maxAlignmentCountDX = 0;
            for (int i = 0; i < N; i++) {
                if (alignmentCountX[i] > maxAlignmentCountX) {
                    maxAlignmentCountX = alignmentCountX[i];
                    maxAlignmentCountDX = i - ALIGNMENT_RADIUS;
                }
            }
            int maxAlignmentCountY = 0;
            int maxAlignmentCountDY = 0;
            for (int i = 0; i < N; i++) {
                if (alignmentCountY[i] > maxAlignmentCountY) {
                    maxAlignmentCountY = alignmentCountY[i];
                    maxAlignmentCountDY = i - ALIGNMENT_RADIUS;
                }
            }
            for (FontChar c : chars) {
                // Align character to the majority alignment
                c.glyphPosX -= maxAlignmentCountDX;
                c.glyphPosY -= maxAlignmentCountDY;

                // Check if this specific char is still outside grid, and if it is, bring it back in
                if (c.glyphPosX < 0) {
                    c.glyphPosX = 0;
                } else if (c.glyphPosX + c.glyphW > fontDim) {
                    c.glyphPosX = fontDim - c.glyphW;
                }
                if (c.glyphPosY < 0) {
                    c.glyphPosY = 0;
                } else if (c.glyphPosY + c.glyphH > fontDim) {
                    c.glyphPosY = fontDim - c.glyphH;
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Map<String, Character> fontGlyphNameMap = loadFontGlyphNameMap();

        new File("src/main/resources/fonts").mkdir();

        File dir = new File("font-src/wqy-bitmapfont");
        if (!dir.exists()) {
            throw new FileNotFoundException(dir.toString());
        }
        for (File file : dir.listFiles()) {
            // Read font files
            if (file.getName().startsWith("wenquanyi_")) {
                // Get font size and bold status
                String fontDesignation = file.getName().substring(10, file.getName().length() - 4);
                int fontDim = fontDesignation.startsWith("9") ? 12
                        : fontDesignation.startsWith("10") ? 13
                                : fontDesignation.startsWith("11") ? 15
                                        : fontDesignation.startsWith("12") ? 16 : -1;
                if (fontDim == -1) {
                    throw new RuntimeException("Bad font name: " + file.getName());
                }
                boolean isBold = fontDesignation.endsWith("b");

                System.out.println("Reading " + file + " : " + fontDim + "px" + (isBold ? " (bold)" : ""));

                // Read chars
                int fontBBDX = 0;
                int fontBBDY = 0;
                int[] charPix = new int[32 * 32];

                Map<Character, FontChar> charToCharInfo = new HashMap<>();

                Alignment alignmentASCII = new Alignment();
                Alignment alignmentLatin1 = new Alignment();
                Alignment alignmentCJKUnifiedIdeograph = new Alignment();
                Alignment alignmentHangeul = new Alignment();
                Alignment alignmentOther = new Alignment();

                List<String> lines = Files.readAllLines(file.toPath());
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line.startsWith("FONTBOUNDINGBOX")) {
                        String[] parts = line.substring(16).split(" ");
                        fontBBDX = Integer.parseInt(parts[2]);
                        fontBBDY = Integer.parseInt(parts[3]);
                    }
                    if (line.startsWith("STARTCHAR")) {
                        String charName = line.substring(10);
                        Character c = fontGlyphNameMap.get(charName);
                        if (c == null) {
                            // System.out.println("Unknown character: " + charName);
                        } else {
                            int charW = 0;
                            int charH = 0;
                            int charDX = 0;
                            int charDY = 0;
                            while (++i < lines.size() && !(line = lines.get(i)).startsWith("BITMAP")
                                    && !line.startsWith("ENDCHAR")) {
                                if (line.startsWith("BBX")) {
                                    String[] parts = line.substring(4).split(" ");
                                    charW = Integer.parseInt(parts[0]);
                                    charH = Integer.parseInt(parts[1]);
                                    charDX = Integer.parseInt(parts[2]);
                                    charDY = Integer.parseInt(parts[3]);
                                }
                            }
                            if (line.startsWith("BITMAP")) {
                                Arrays.fill(charPix, 0);
                                for (int row = 0; ++i < lines.size()
                                        && !(line = lines.get(i)).startsWith("ENDCHAR"); row++) {
                                    // Data is big endian, row-major
                                    for (int j = 0; j < line.length(); j++) {
                                        int bits = Integer.parseInt(line.substring(j, j + 1), 16);
                                        for (int k = 0; k < 4; k++) {
                                            charPix[j * 4 + k + row * 32] = (bits & (1 << (3 - k))) != 0 ? 1 : 0;
                                        }
                                    }
                                }
                                //                                System.out.println();
                                //                                System.out.println(c);
                                //                                for (int row = 0; row < charH; row++) {
                                //                                    for (int col = 0; col < charW; col++) {
                                //                                        System.out.print(pixBuf[col + row * 32] == 1 ? '#' : ' ');
                                //                                    }
                                //                                    System.out.println();
                                //                                }

                                // A few characters are wider than their monospaced font dimension allows (by one pixel).
                                // Just OR together their last column. (They are mostly drawing characters.)
                                if (charW == fontDim + 1) {
                                    for (int row = 0; row < charH; row++) {
                                        charPix[fontDim - 1 + row * 32] |= charPix[fontDim + row * 32];
                                    }
                                    charW--;
                                }

                                // Spot check char size
                                if (charW > fontDim || charH > fontDim) {
                                    throw new RuntimeException("Character too big: " + c);
                                }

                                // Create char
                                FontChar charData = new FontChar(charDX - fontBBDX,
                                        // Invert dy, since dy is Cartesian, but pixels are in screen row order
                                        fontDim - (charH + (charDY - fontBBDY)), //
                                        charW, charH, fontDim, charPix, 32);
                                charToCharInfo.put(c, charData);

                                // Separately find optimal alignment of different character types to the (fontDim x fontDim) grid
                                if (c >= 32 && c <= 126) {
                                    alignmentASCII.add(charData, fontDim);
                                } else if (c <= 0xff) {
                                    alignmentLatin1.add(charData, fontDim);
                                } else if (c >= 0x4E00 && c <= 0x9FA5) {
                                    alignmentCJKUnifiedIdeograph.add(charData, fontDim);
                                } else if (c >= 0xAC00 && c <= 0xD7A3) {
                                    alignmentHangeul.add(charData, fontDim);
                                } else {
                                    alignmentOther.add(charData, fontDim);
                                }
                            }
                        }
                    }
                }

                //                System.out.println("Alignments:");
                //                System.out.println("ASCII:\n" + alignmentASCII);
                //                System.out.println("Latin1:\n" + alignmentLatin1);
                //                System.out.println("CJK:\n" + alignmentCJKUnifiedIdeograph);
                //                System.out.println("Hangeul:\n" + alignmentHangeul);
                //                System.out.println("Other:\n" + alignmentOther);

                // Find best alignment of characters to the (fontDim x fontDim) grid, within each character class
                alignmentASCII.align(fontDim);
                alignmentLatin1.align(fontDim);
                alignmentCJKUnifiedIdeograph.align(fontDim);
                alignmentHangeul.align(fontDim);
                alignmentOther.align(fontDim);

                //                Alignment testAlignment = new Alignment();
                //                for (CharInfo ci : charToCharInfo.values()) {
                //                    testAlignment.add(ci, fontDim);
                //                }
                //                System.out.println(testAlignment.toString());

                // Save font to disk
                String outputPath = "src/main/resources/fonts/wqy-" + fontDim + (isBold ? "-bold" : "") + "-font";
                System.out.println("Saving " + outputPath);
                new SaveableFont(charToCharInfo).save(outputPath);
            }
        }
        System.out.println("Finished");
    }
}
