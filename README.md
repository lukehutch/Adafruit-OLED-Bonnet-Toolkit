# Adafruit-OLED-Bonnet-Toolkit
Java toolkit for the Adafruit 128x64 OLED bonnet, with support for the screen, D-pad/buttons, UI layout, and task scheduling.

To initialize the OLED screen and button hardware, call:

```java
Bonnet.init();
```

To set up the UI and start drawing, do the following:

```
Screen.init(new RootScreen());
```

where `RootScreen` extends `Screen`:

```
import aobtk.font.Font;
import aobtk.hw.HWButton;
import aobtk.ui.element.Menu;
import aobtk.ui.element.TextElement;
import aobtk.ui.element.VLayout;
import aobtk.ui.element.VLayout.VAlign;
import aobtk.ui.screen.Screen;

public class RootScreen extends Screen {

    private Menu menu;

    public TestScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void open() {
        VLayout layout = new VLayout();

        layout.add(new TextElement(Font.FONT_5X8, "Hello World"), VAlign.TOP);

        menu = new Menu(Font.FONT_5X8, /* spacing = */ 0, /* hLayout = */ true,
                "Hello", "Goodbye");
        layout.add(menu, VAlign.BOTTOM);

        setUI(layout);
    }

    @Override
    public void close() {
        // Teardown stuff here when screen closes
    }

    @Override
    public boolean acceptsButtonA() {
        // Button A will move up to parent screen by default
        return false;
    }

    @Override
    public void buttonDown(HWButton button) {
        switch (button) {
        case L:
            menu.decSelectedIdx();
            break;
        case R:
            menu.incSelectedIdx();
            break;
        case C:
        case B:
            // Center button or button B will move to child screen
            if (menu.getSelectedItem().toString().equals("Hello")) {
                setCurrScreen(new HelloScreen(this));
            } else if (menu.getSelectedItem().toString().equals("Goodbye")) {
                setCurrScreen(new GoodbyeScreen(this));
            }
            break;
        }
    }
}
```



### Code used in this project:

* OLED driver code and 4x5/5x8 fonts from [Pi-OLED](https://github.com/entrusc/Pi-OLED) by Florian Frankenberger, which is a port of the [Adafruit_SSD1306](https://github.com/adafruit/Adafruit_SSD1306) Python driver for the SSD1306 OLED driver.
* [Latin1/Korean 16x16](https://github.com/Dalgona/neodgm/blob/master/font.py) font by Dalgona. 
