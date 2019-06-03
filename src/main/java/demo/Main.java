/**
 * This demo code is in the public domain. 
 * 
 * @author Luke Hutchison
 */
package demo;

import aobtk.hw.Bonnet;
import aobtk.i18n.Str;
import aobtk.ui.screen.Screen;

public class Main {
    public static void main(String[] args) throws Exception {
        // Initialize the Button class, and register the GPIO event listeners
        Bonnet.init();

        // Set language (0 => English, 1 => Chinese, 2 => Korean)
        Str.lang = 0;
        
        // Initialize UI with the RootScreen class
        Screen.init(new RootScreen());

        // Keep program running until termination
        for (;;) {
            try {
                Thread.sleep(1000_000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
