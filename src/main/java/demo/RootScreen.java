/**
 * This demo code is in the public domain. 
 * 
 * @author Luke Hutchison
 */
package demo;

import aobtk.font.Font;
import aobtk.hw.HWButton;
import aobtk.i18n.Str;
import aobtk.ui.element.Menu;
import aobtk.ui.element.TextElement;
import aobtk.ui.element.VLayout;
import aobtk.ui.element.VLayout.VAlign;
import aobtk.ui.screen.Screen;

public class RootScreen extends Screen {
    private Menu menu;

    private static final Str RED_PILL = new Str("Red pill", "红丸", "파란알약");
    private static final Str BLUE_PILL = new Str("Blue pill", "蓝丸", "빨간알약");

    public RootScreen() {
        super(/* parentScreen = */ null);
    }

    @Override
    public void open() {
        // Set up UI when screen opens
        VLayout layout = new VLayout();

        layout.add(new TextElement(Font.PiOLED_5x8().newStyle(), "Choose Wisely"), VAlign.TOP);

        menu = new Menu(Font.WenQuanYi_16().newStyle(), /* spacing = */ 4, /* hLayout = */ true, RED_PILL,
                BLUE_PILL);
        layout.add(menu, VAlign.CENTER);

        setUI(layout);
    }

    @Override
    public void close() {
        // Teardown stuff here when screen closes
    }

    @Override
    public boolean acceptsButtonA() {
        // Button A will move up to parent screen by default (i.e. works as a "cancel" button),
        // unless this function returns true
        return false;
    }

    @Override
    public void buttonDown(HWButton button) {
        System.out.println("Got button " + button);
        switch (button) {
        case L:
            menu.decSelectedIdx();
            break;
        case R:
            menu.incSelectedIdx();
            break;
        case C:
            // Switch languages (0 => English, 1 => Chinese, 2 => Korean)
            Str.lang = (Str.lang + 1) % 3;
            // Schedule a repaint since UI changed
            repaint();
            break;
        case B:
            // Center button or button B will move to child screen
            if (menu.getSelectedItem() == BLUE_PILL) {
                // The story ends, you wake up in your bed and believe whatever you want to believe.
                // (Set parentScreen to null, then there's no going back to the parent.)
                setCurrScreen(new BluePillScreen(/* parentScreen = */ null));
            } else if (menu.getSelectedItem() == RED_PILL) {
                // You stay in Wonderland, and I show you how deep the rabbit hole goes.
                // (Set parentScreen to this, then goToParentScreen() will come back here.)
                setCurrScreen(new RedPillScreen(this));
            }
            break;
        case A:
        case D:
        case U:
            // Ignore
            break;
        }
    }
}