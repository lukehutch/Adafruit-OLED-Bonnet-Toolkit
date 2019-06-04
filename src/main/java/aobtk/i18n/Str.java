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
package aobtk.i18n;

import java.util.Arrays;

public class Str {

    private static final String TEMPLATE_CHAR = "$";

    /**
     * The current language. If you change this, all strings will be displayed in the new language at the next UI
     * repaint.
     */
    public static volatile int lang = 0;

    private String[] strings;
    private Object[] params;

    /** Create a {@link Str} object, with one string per language. */
    public Str(String... strings) {
        if (strings.length == 0) {
            throw new IllegalArgumentException("Need at least one string parameter");
        }
        this.strings = strings;
    }

    /**
     * Fill in parameters of a template {@link Str} object. Each substring "$0", "$1" etc. in the template has a
     * corresponding parameter substituted when {@link Str#toString()} is called.
     */
    public Str(Str template, Object... params) {
        this.strings = template.strings;
        if (params.length > 10) {
            // For more than 10 params, will need to implement a syntax like "${10}"
            throw new IllegalArgumentException("Only up to 10 params are supported currently");
        }
        this.params = params;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof Str)) {
            return false;
        }
        return Arrays.equals(this.strings, ((Str) obj).strings);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(strings);
    }

    public String toString(int langIdx) {
        int langIdxToUse = langIdx < 0 || langIdx >= strings.length ? 0 : langIdx;
        return strings[langIdxToUse];
    }

    @Override
    public String toString() {
        String template = toString(lang);
        if (params == null || params.length == 0) {
            // No params to substitute
            return template;
        } else {
            // Substitute params
            StringBuilder buf = new StringBuilder();
            int currPos = 0;
            for (int nextParamPos; (nextParamPos = template.indexOf(TEMPLATE_CHAR, currPos)) >= 0;) {
                if (nextParamPos > currPos) {
                    // Copy chars before param
                    buf.append(template.subSequence(currPos, nextParamPos));
                }
                char nextChar = nextParamPos == template.length() - 1 ? '\0' : template.charAt(nextParamPos + 1);
                if (nextChar >= '0' && nextChar <= '9') {
                    // Found a template parameter
                    int paramIdx = nextChar - '0';
                    if (paramIdx < params.length) {
                        buf.append(params[paramIdx++]);
                    }
                    currPos = nextParamPos + 2;
                } else {
                    // Not a template parameter
                    buf.append(TEMPLATE_CHAR);
                    currPos = nextParamPos + 1;
                }
            }
            if (currPos < template.length()) {
                buf.append(template.subSequence(currPos, template.length()));
            }
            return buf.toString();
        }
    }
}
