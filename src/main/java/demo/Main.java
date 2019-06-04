/**
 * This demo code is in the public domain. 
 * 
 * @author Luke Hutchison
 */
package demo;

import aobtk.hw.Bonnet;
import aobtk.ui.screen.Screen;

public class Main {
    public static void main(String[] args) throws Exception {
        // Initialize the Button class, and register the GPIO event listeners
        Bonnet.init();

        // Start with ChooseLangScreen
        Screen.init(new ChooseLangScreen());

        // Keep program running until termination
        for (;;) {
            try {
                Thread.sleep(1000_000);
            } catch (InterruptedException e) {
                Bonnet.shutdown();
                break;
            }
        }
    }
}
