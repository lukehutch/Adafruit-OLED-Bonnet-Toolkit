/**
 * This demo code is in the public domain. 
 * 
 * @author Luke Hutchison
 */
package demo;

import aobtk.font.Font;
import aobtk.hw.HWButton;
import aobtk.i18n.Str;
import aobtk.ui.element.TextElement;
import aobtk.ui.element.VLayout;
import aobtk.ui.screen.Screen;

public class RedPillScreen extends Screen {
    private static final Str RED_PILL = new Str("You took\nthe red pill", "你吃了红丸", "파란 알약을\n먹었다");

    public RedPillScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void open() {
        setUI(new VLayout(new TextElement(Font.WenQuanYi_16_bold().newStyle(), RED_PILL)));
    }

    @Override
    public void buttonDown(HWButton button) {
        if (button == HWButton.B) {
            setUI(new VLayout(new TextElement(Font.PiOLED_4x5().newStyle(), "WELCOME TO THE REAL WORLD")));
        }
    }
}