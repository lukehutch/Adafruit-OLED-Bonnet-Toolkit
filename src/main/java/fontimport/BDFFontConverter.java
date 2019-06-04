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

class BDFFontConverter {

    public static Map<String, Character> loadFontGlyphNameMap() throws IOException {
        Map<String, Character> map = new HashMap<>();
        // This map was derived from http://www.jdawiseman.com/papers/trivia/character-entities.html
        // By adding HTML entity to character mappings, then overwriting with Postscript to character mappings,
        // and adding the unicode character codepoint names used by the WQY fonts. 
        for (String line : Files.readAllLines(Paths.get("src/main/java/fontimport/font-charname-to-char-map"))) {
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
        private static final int ALIGNMENT_RADIUS = 15;
        private static final int N = ALIGNMENT_RADIUS * 2 + 1;
        int[] alignmentCountX = new int[N];
        int[] alignmentCountY = new int[N];
        List<FontChar> chars = new ArrayList<>();

        public void add(FontChar c, int maxGlyphW, int maxGlyphH) {
            chars.add(c);
            if (c.glyphPosX < 0) {
                alignmentCountX[c.glyphPosX + ALIGNMENT_RADIUS]++;
            } else if (c.glyphPosX + c.glyphW > maxGlyphW) {
                alignmentCountX[c.glyphPosX + c.glyphW - maxGlyphW + ALIGNMENT_RADIUS]++;
            } else {
                alignmentCountX[ALIGNMENT_RADIUS]++;
            }
            if (c.glyphPosY < 0) {
                alignmentCountY[c.glyphPosY + ALIGNMENT_RADIUS]++;
            } else if (c.glyphPosY + c.glyphH > maxGlyphH) {
                alignmentCountY[c.glyphPosY + c.glyphH - maxGlyphH + ALIGNMENT_RADIUS]++;
            } else {
                alignmentCountY[ALIGNMENT_RADIUS]++;
            }
        }

        @Override
        public String toString() {
            return Arrays.toString(alignmentCountX) + "\n" + Arrays.toString(alignmentCountY);
        }

        public void align(int maxGlyphW, int maxGlyphH) {
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
                c.glyphPosX -= maxAlignmentCountDX;
                c.glyphPosY -= maxAlignmentCountDY;
                c.glyphPosY -= maxAlignmentCountDY;

                // Check if this specific char is still outside grid, and if it is, bring it back in
                if (c.glyphPosX < 0) {
                    c.glyphPosX = 0;
                } else if (c.glyphPosX + c.glyphW > maxGlyphW) {
                    c.glyphPosX = maxGlyphW - c.glyphW;
                }
                if (c.glyphPosY < 0) {
                    c.glyphPosY = 0;
                } else if (c.glyphPosY + c.glyphH > maxGlyphH) {
                    c.glyphPosY = maxGlyphH - c.glyphH;
                }
            }
        }

        /** Make sure all chars are aligned to fit within the (0,0)-(maxGlyphW,maxGlyphH) grid. */
        public boolean isAligned() {
            for (int i = 0; i < N; i++) {
                if (alignmentCountX[i] > 0 && i != ALIGNMENT_RADIUS) {
                    return false;
                }
                if (alignmentCountY[i] > 0 && i != ALIGNMENT_RADIUS) {
                    return false;
                }
            }
            return true;
        }
    }

    // Spec: https://www.adobe.com/content/dam/acom/en/devnet/font/pdfs/5005.BDF_Spec.pdf
    public static void importBDFFont(File inputFile, String outputName, int maxGlyphW, int maxGlyphH)
            throws IOException {
        Map<String, Character> fontGlyphNameMap = loadFontGlyphNameMap();

        if (!inputFile.exists()) {
            throw new FileNotFoundException(inputFile.toString());
        }

        System.out.println("Reading " + inputFile);

        Map<Character, FontChar> charToCharInfo = new HashMap<>();

        Alignment alignmentASCII = new Alignment();
        Alignment alignmentLatin1 = new Alignment();
        Alignment alignmentCJKUnifiedIdeograph = new Alignment();
        Alignment alignmentHangeul = new Alignment();
        Alignment alignmentOther = new Alignment();

        // Pixel buffer -- make it bigger than the max glyph size, since some glyphs are oversized
        int charPixStride = maxGlyphW * 2;
        int[] charPix = new int[charPixStride * maxGlyphH * 2];

        // Read chars
        int fontBBH = 0;
        int fontBBDX = 0;
        int fontBBDY = 0;
        int fontAscent = 0;
        int fontDescent = 0;
        List<String> lines = Files.readAllLines(inputFile.toPath());
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("FONTBOUNDINGBOX")) {
                String[] parts = line.substring(16).split(" ");
                fontBBH = Integer.parseInt(parts[1]);
                fontBBDX = Integer.parseInt(parts[2]);
                fontBBDY = Integer.parseInt(parts[3]);

            } else if (line.startsWith("FONT_DESCENT")) {
                fontDescent = Integer.parseInt(line.substring(13));

            } else if (line.startsWith("FONT_ASCENT")) {
                fontAscent = Integer.parseInt(line.substring(12));

            } else if (line.startsWith("STARTCHAR")) {
                String charName = line.substring(10);
                Character c = fontGlyphNameMap.get(charName);
                if (c == null && charName.startsWith("U+") || charName.startsWith("U_")
                        || charName.startsWith("uni")) {
                    try {
                        c = (char) Integer.parseInt(charName.substring(charName.startsWith("uni") ? 3 : 2), 16);
                    } catch (NumberFormatException e) {
                    }
                }
                if (c == null) {
                    // System.out.println("Unknown character: " + charName);
                } else {
                    int charW = 0;
                    int charH = 0;
                    int charDX = 0;
                    int charDY = 0;
                    int dWidth = 0;
                    while (++i < lines.size() && !(line = lines.get(i)).startsWith("BITMAP")
                            && !line.startsWith("ENDCHAR")) {

                        if (line.startsWith("BBX")) {
                            String[] parts = line.substring(4).split(" ");
                            charW = Integer.parseInt(parts[0]);
                            charH = Integer.parseInt(parts[1]);
                            charDX = Integer.parseInt(parts[2]);
                            charDY = Integer.parseInt(parts[3]);

                        } else if (line.startsWith("DWIDTH")) {
                            String[] parts = line.substring(7).split(" ");
                            dWidth = Integer.parseInt(parts[0]);
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
                                    charPix[j * 4 + k + row * charPixStride] = (bits & (1 << (3 - k))) != 0 ? 1 : 0;
                                }
                            }
                        }

                        //    System.out.println();
                        //    System.out.println(c);
                        //    for (int row = 0; row < charH; row++) {
                        //        for (int col = 0; col < charW; col++) {
                        //            System.out.print(charPix[col + row * stride] == 1 ? '#' : ' ');
                        //        }
                        //        System.out.println();
                        //    }

                        // A few characters are wider or taller than their max font dimension allows (by one pixel).
                        // Just OR together their last column or row. (They are mostly drawing characters.)
                        if (charW > maxGlyphW) {
                            System.out.println("Squashing right side of overly wide char " + c);
                            for (int x = maxGlyphW, xx = charW; x < xx; x++) {
                                for (int y = 0; y < charH; y++) {
                                    charPix[maxGlyphW - 1 + y * charPixStride] |= charPix[x + y * charPixStride];
                                }
                                charW--;
                            }
                        }
                        if (charH > maxGlyphH) {
                            System.out.println("Squashing bottom of overly tall char " + c);
                            for (int y = maxGlyphH, yy = charH; y < yy; y++) {
                                for (int x = 0; x < charW; x++) {
                                    charPix[x + (maxGlyphH - 1) * charPixStride] |= charPix[x + y * charPixStride];
                                }
                                charH--;
                            }
                        }

                        // Get nominal width of character (which is the x-component of DWIDTH).
                        // N.B. if this is zero or negative, because the char is used for composition,
                        // replace it with maxGlyphW.
                        int nominalW = dWidth > 0 ? dWidth : maxGlyphW;

                        // Create char
                        FontChar charData = new FontChar(c, charDX - fontBBDX,
                                // Invert dy, since dy is Cartesian, but pixels are in screen row order
                                maxGlyphH - (charH + (charDY - fontBBDY)), //
                                charW, charH, nominalW, charPix, charPixStride);
                        charToCharInfo.put(c, charData);

                        // Separately find optimal alignment of different character types to the (fontDim x fontDim) grid
                        if (c >= 32 && c <= 126) {
                            alignmentASCII.add(charData, maxGlyphW, maxGlyphH);
                        } else if (c <= 0xff) {
                            alignmentLatin1.add(charData, maxGlyphW, maxGlyphH);
                        } else if (c >= 0x4E00 && c <= 0x9FA5) {
                            alignmentCJKUnifiedIdeograph.add(charData, maxGlyphW, maxGlyphH);
                        } else if (c >= 0xAC00 && c <= 0xD7A3) {
                            alignmentHangeul.add(charData, maxGlyphW, maxGlyphH);
                        } else {
                            alignmentOther.add(charData, maxGlyphW, maxGlyphH);
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
        alignmentASCII.align(maxGlyphW, maxGlyphH);
        alignmentLatin1.align(maxGlyphW, maxGlyphH);
        alignmentCJKUnifiedIdeograph.align(maxGlyphW, maxGlyphH);
        alignmentHangeul.align(maxGlyphW, maxGlyphH);
        alignmentOther.align(maxGlyphW, maxGlyphH);

        Alignment testAlignment = new Alignment();
        for (FontChar ci : charToCharInfo.values()) {
            testAlignment.add(ci, maxGlyphW, maxGlyphH);
        }
        if (!testAlignment.isAligned()) {
            throw new RuntimeException("Failed to align font");
        }

        // Save font to disk
        File outputDir = new File("src/main/resources/fonts");
        outputDir.mkdir();
        File outputFile = new File(outputDir, outputName);
        System.out.println("Saving " + outputFile);
        new SaveableFont(charToCharInfo).save(outputFile.getPath());
    }
}
