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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import aobtk.font.Font;
import aobtk.font.FontChar;

class SaveableFont extends Font {
    /**
     * Initialize the font from a map. All characters should already be present in the map, since the characters
     * will be measured immediately.
     */
    protected SaveableFont(Map<Character, FontChar> charToCharInfo) {
        super(charToCharInfo);
    }

    /** Save the font to disk. */
    public void save(String path) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(path);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                DataOutputStream out = new DataOutputStream(bos)) {

            List<FontChar> fontChars = getFontChars();

            //            // Visualize characters in font
            //            for (FontChar fontChar : fontChars) {
            //                System.out.println("\nChar " + fontChar.chr + " (" + Integer.toString(fontChar.chr, 16) + "):");
            //                fontChar.printGrid();
            //            }

            // Concatenate the pixel bytes from all characters
            int numChars = fontChars.size();
            int[] byteOffset = new int[numChars];
            ByteArrayOutputStream charPixels = new ByteArrayOutputStream();
            for (int i = 0; i < fontChars.size(); i++) {
                FontChar fontChar = fontChars.get(i);
                byteOffset[i] = charPixels.size();
                byte[] charPixBits = fontChar.getCharPixBits();
                charPixels.write(charPixBits, fontChar.getCharPixBitsStartIdx(), fontChar.getCharPixBitsLen());
            }

            // Write out character metadata
            out.writeInt(numChars);
            for (int i = 0; i < fontChars.size(); i++) {
                FontChar fontChar = fontChars.get(i);
                out.writeChar(fontChar.chr);
                out.writeByte(fontChar.glyphPosX);
                out.writeByte(fontChar.glyphPosY);
                out.writeByte(fontChar.glyphW);
                out.writeByte(fontChar.glyphH);
                out.writeByte(fontChar.nominalW);
                out.writeInt(byteOffset[i]);
            }

            // Write out pixel data
            out.write(charPixels.toByteArray());
        }
    }
}
