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

public class BluePillScreen extends Screen {
    private static final Str BLUE_PILL = new Str("You took\nthe blue pill", "你吃了蓝丸", "빨간알약을\n먹었다");

    public BluePillScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void open() {
        setUI(new VLayout(new TextElement(Font.WQY_Song_16_bold().newStyle(), BLUE_PILL)));

        // There's no going back to parent (parentScreen is null, and additionally button A is overridden)
        waitThenGoToParentScreen(/* milliseconds = */ 3000);
    }

    @Override
    public boolean acceptsButtonA() {
        return true;
    }

    @Override
    public void buttonDown(HWButton button) {
        if (button == HWButton.A) {
            setUI(new VLayout(new TextElement(Font.PiOLED_4x5().newStyle(), "THERE IS NO GOING BACK")));
        } else {
            setUI(new VLayout(new TextElement(Font.PiOLED_4x5().newStyle(), "THE WORLD IS BACK TO NORMAL")));
        }
    }
}