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
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aobtk.font.FontChar;

class ImportFonts {
    private static void importGNUUniFont() throws IOException {
        BDFFontConverter.importBDFFont(new File("font-src/gnu-unifont/unifont-12.1.02.bdf"),
                /* outputFileName = */ "gnu-unifont-16-font", /* maxGlyphW = */ 16, /* maxGlyphH = */ 16);
    }

    // TODO: the 'i' character is one pixel lower than the rest of the font
    private static void importLiberationSansFont() throws IOException {
        File dir = new File("font-src/wqy-bitmapfont-1.0.0RC1");
        if (!dir.exists()) {
            throw new FileNotFoundException(dir.toString());
        }
        for (File file : dir.listFiles()) {
            // Read font files
            if (file.getName().startsWith("LiberationSans-")) {
                // Get font size and bold status
                String fontDesignation = file.getName().substring(15, file.getName().length() - 4);
                int fontDim = fontDesignation.startsWith("9") ? 12
                        : fontDesignation.startsWith("10") ? 13
                                : fontDesignation.startsWith("11") ? 15
                                        : fontDesignation.startsWith("12") ? 16 : -1;
                if (fontDim == -1) {
                    throw new RuntimeException("Bad font name: " + file.getName());
                }

                System.out.println("Liberation Sans bitmap font : " + fontDim + "px");

                BDFFontConverter.importBDFFont(file, /* outputFileName = */ "liberation-sans-" + fontDim + "-font",
                        /* maxGlyphW = */ fontDim, /* maxGlyphH = */ fontDim);
            }
        }
    }

    private static void importPiOLEDFont(File file, int w, int h, int charStart) throws IOException {
        System.out.println("Reading " + file);

        Map<Character, FontChar> charToCharInfo = new HashMap<>();

        CharsetDecoder decoder = Charset.forName("IBM-437").newDecoder();
        byte[] array = new byte[1];

        List<String> lines = Files.readAllLines(file.toPath());
        int[] charPix = new int[8 * 8];
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            // Characters are in cp437 (or something similar -- some of the high-bit-set glyphs
            // map to the wrong character)
            array[0] = (byte) (charStart + i);
            char chr = decoder.decode(ByteBuffer.wrap(array)).charAt(0);

            Arrays.fill(charPix, 0);
            for (int col = 0; col < w; col++) {
                int charColBits = Integer.parseInt(line.substring(col * 2, col * 2 + 2), 16);
                for (int row = 0; row < h; row++) {
                    boolean bit = (charColBits & (1 << row)) != 0;
                    if (bit) {
                        charPix[col + row * 8] = 1;
                    }
                }
            }
            charToCharInfo.put(chr, new FontChar(chr, 0, 0, w, h, w, charPix, 8));
        }

        // Save font to disk
        new File("src/main/resources/fonts").mkdir();
        String outputPath = "src/main/resources/fonts/pi-oled-" + file.getName() + "-font";
        System.out.println("Saving " + outputPath);
        new SaveableFont(charToCharInfo).save(outputPath);
    }

    private static void importPiOLEDFonts() throws IOException {
        importPiOLEDFont(new File("font-src/pi-oled-font/4x5"), 4, 5, 32);
        importPiOLEDFont(new File("font-src/pi-oled-font/5x8"), 5, 8, 0);
    }

    private static void importWQYFonts() throws IOException {
        // Use the 0.9.9 version rather than the 1.0.0RC1 version, since 1.0.0RC1 does not have the bold versions available,
        // and there's a strange "13px" version (with filesize smaller than even the "9pt" version) that is out of place.
        File dir = new File("font-src/wqy-bitmapfont-gb18030-0.9.9");
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

                System.out.println("Found WQY Song bitmap font : " + fontDim + "px" + (isBold ? " (bold)" : ""));

                BDFFontConverter.importBDFFont(file,
                        /* outputFileName = */ "wqy-song-" + fontDim + (isBold ? "-bold" : "") + "-font",
                        /* maxGlyphW = */ fontDim, /* maxGlyphH = */ fontDim);
            }
        }

        BDFFontConverter.importBDFFont(new File("font-src/wqy-unibit-1.1.0/wqy-unibit.bdf"),
                /* outputFileName = */ "wqy-unibit-16-font", /* maxGlyphW = */ 16, /* maxGlyphH = */ 16);
    }

    public static void main(String[] args) throws IOException {
        importGNUUniFont();
        importLiberationSansFont();
        NeoDGMFont.importNeoDGMFont();
        importPiOLEDFonts();
        importWQYFonts();
        System.out.println("Finished");
    }
}
