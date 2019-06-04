/**
 * This demo code is in the public domain. 
 * 
 * @author Luke Hutchison
 */
package demo;

import aobtk.font.Font;
import aobtk.font.FontChar;

/** Visualize all the characters in a given font. */
public class FontViewer {
    public static void main(String[] args) throws Exception {
        // The font to visualize
        Font font = Font.LiberationSans_16();

        // Display characters in the font
        for (FontChar fontChar : font.getFontChars()) {
            String fontCode = "000" + Integer.toString(fontChar.chr, 16);
            System.out.println("\nChar " + fontChar.chr + " (U+" + fontCode.substring(fontCode.length() - 4) + "):");
            fontChar.printGrid();
        }
    }
}
