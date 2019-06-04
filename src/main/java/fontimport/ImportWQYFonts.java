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

class ImportWQYFonts {
    public static void main(String[] args) throws IOException {
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

        System.out.println("Finished");
    }
}
