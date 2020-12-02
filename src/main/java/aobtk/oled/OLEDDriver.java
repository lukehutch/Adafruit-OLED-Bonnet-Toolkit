/*
 * Based on code from https://github.com/entrusc/Pi-OLED
 * 
 * with modifications by Luke Hutchison
 * 
 * --
 * 
 * Copyright (c) 2016, Florian Frankenberger
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of the copyright holder nor the names of its contributors
 *   may be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package aobtk.oled;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import aobtk.hw.Bonnet;
import aobtk.hw.I2CDevice;

/**
 * A raspberry pi driver for the 128x64 pixel OLED display (i2c bus). The supported kind of display uses the SSD1306
 * driver chip and is connected to the raspberry's i2c bus (bus 1).
 * 
 * <p/>
 * Note that you need to enable i2c (using for example raspi-config). Also note that you need to load the following
 * kernel modules:
 * 
 * <pre>
 * i2c - bcm2708
 * </pre>
 * 
 * and
 * 
 * <pre>
 * i2c_dev
 * </pre>
 * 
 * <p/>
 * Also note that it is possible to speed up the refresh rate of the display up to ~60fps by adding the following to
 * the /boot/config.txt of your raspberry: dtparam=i2c_baudrate=1000000
 * 
 * <p/>
 * This class is basically a rough port of Adafruit's BSD licensed SSD1306 library
 * (https://github.com/adafruit/Adafruit_SSD1306)
 *
 * @author Florian Frankenberger, modified by Luke Hutchison
 */
@SuppressWarnings("unused")
public class OLEDDriver {
    private I2CDevice i2cDev;

    private static final int DEFAULT_I2C_BUS = 1;
    private static final int DEFAULT_DISPLAY_ADDRESS = 0x3C;

    public static final int DISPLAY_WIDTH = 128;
    public static final int DISPLAY_HEIGHT = 64;

    private static final byte[] BLANK_BUFFER = new byte[(DISPLAY_WIDTH * DISPLAY_HEIGHT) / 8];

    private static final byte SSD1306_SETCONTRAST = (byte) 0x81;
    private static final byte SSD1306_DISPLAYALLON_RESUME = (byte) 0xA4;
    private static final byte SSD1306_DISPLAYALLON = (byte) 0xA5;
    private static final byte SSD1306_NORMALDISPLAY = (byte) 0xA6;
    private static final byte SSD1306_INVERTDISPLAY = (byte) 0xA7;
    private static final byte SSD1306_DISPLAYOFF = (byte) 0xAE;
    private static final byte SSD1306_DISPLAYON = (byte) 0xAF;

    private static final byte SSD1306_SETDISPLAYOFFSET = (byte) 0xD3;
    private static final byte SSD1306_SETCOMPINS = (byte) 0xDA;

    private static final byte SSD1306_SETVCOMDETECT = (byte) 0xDB;

    private static final byte SSD1306_SETDISPLAYCLOCKDIV = (byte) 0xD5;
    private static final byte SSD1306_SETPRECHARGE = (byte) 0xD9;

    private static final byte SSD1306_SETMULTIPLEX = (byte) 0xA8;

    private static final byte SSD1306_SETLOWCOLUMN = (byte) 0x00;
    private static final byte SSD1306_SETHIGHCOLUMN = (byte) 0x10;

    private static final byte SSD1306_SETSTARTLINE = (byte) 0x40;

    private static final byte SSD1306_MEMORYMODE = (byte) 0x20;
    private static final byte SSD1306_COLUMNADDR = (byte) 0x21;
    private static final byte SSD1306_PAGEADDR = (byte) 0x22;

    private static final byte SSD1306_COMSCANINC = (byte) 0xC0;
    private static final byte SSD1306_COMSCANDEC = (byte) 0xC8;

    private static final byte SSD1306_SEGREMAP = (byte) 0xA0;

    private static final byte SSD1306_CHARGEPUMP = (byte) 0x8D;

    private static final byte SSD1306_EXTERNALVCC = (byte) 0x1;
    private static final byte SSD1306_SWITCHCAPVCC = (byte) 0x2;

    private boolean shutdown = false;

    private static final Logger LOGGER = Logger.getLogger(OLEDDriver.class.getCanonicalName());

    /**
     * creates an oled display object with default i2c bus 1 and default display address of 0x3C
     *
     * @throws IOException
     */
    public OLEDDriver() {
        this(DEFAULT_I2C_BUS, DEFAULT_DISPLAY_ADDRESS);
    }

    /**
     * creates an oled display object with default i2c bus 1 and the given display address
     *
     * @param displayAddress the i2c bus address of the display
     * @throws IOException
     */
    public OLEDDriver(int displayAddress) {
        this(DEFAULT_I2C_BUS, displayAddress);
    }

    /**
     * constructor with all parameters
     *
     * @param busNumber      the i2c bus number (use constants from I2CBus)
     * @param displayAddress the i2c bus address of the display
     * @throws IOException
     */
    public OLEDDriver(int busNumber, int displayAddress) {
        i2cDev = Bonnet.openI2CDevice(busNumber, displayAddress);
        try {
            switchOn();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IOException trying to switch on display", e);
            Bonnet.shutdown();
            System.exit(1);
        }
    }

    public synchronized void clear() throws IOException {
        update(BLANK_BUFFER);
    }

    private synchronized void writeCommand(byte cmd) throws IOException {
        if (!shutdown) {
            i2cDev.writeRegister(0x00, cmd);
        }
    }

    private synchronized void writeRegister(int register, byte[] bytes, int start, int length) throws IOException {
        if (!shutdown) {
            i2cDev.writeRegister(register, bytes, start, length);
        }
    }

    private synchronized void switchOn() throws IOException {
        writeCommand(SSD1306_DISPLAYOFF); // 0xAE
        writeCommand(SSD1306_SETDISPLAYCLOCKDIV); // 0xD5
        writeCommand((byte) 0x80); // the suggested ratio 0x80
        writeCommand(SSD1306_SETMULTIPLEX); // 0xA8
        writeCommand((byte) 0x3F);
        writeCommand(SSD1306_SETDISPLAYOFFSET); // 0xD3
        writeCommand((byte) 0x0); // no offset
        writeCommand((byte) (SSD1306_SETSTARTLINE | 0x0)); // line #0
        writeCommand(SSD1306_CHARGEPUMP); // 0x8D
        writeCommand((byte) 0x14);
        writeCommand(SSD1306_MEMORYMODE); // 0x20
        writeCommand((byte) 0x00); // 0x0 acts like ks0108
        writeCommand((byte) (SSD1306_SEGREMAP | 0x1));
        writeCommand(SSD1306_COMSCANDEC);
        writeCommand(SSD1306_SETCOMPINS); // 0xDA
        writeCommand((byte) 0x12);
        writeCommand(SSD1306_SETCONTRAST); // 0x81
        writeCommand((byte) 0xCF);
        writeCommand(SSD1306_SETPRECHARGE); // 0xd9
        writeCommand((byte) 0xF1);
        writeCommand(SSD1306_SETVCOMDETECT); // 0xDB
        writeCommand((byte) 0x40);
        writeCommand(SSD1306_DISPLAYALLON_RESUME); // 0xA4
        writeCommand(SSD1306_NORMALDISPLAY);

        clear(); // Clear buffer before switching on OLED panel

        writeCommand(SSD1306_DISPLAYON); // -- turn on OLED panel
    }

    private synchronized void switchOff() throws IOException {
        clear();
        writeCommand(SSD1306_DISPLAYOFF); // -- turn off OLED panel
    }

    /**
     * sends the current buffer to the display
     * 
     * @throws IOException
     */
    public synchronized void update(byte[] imageBuffer) throws IOException {
        if (imageBuffer.length * 8 != DISPLAY_WIDTH * DISPLAY_HEIGHT) {
            throw new IllegalArgumentException("Invalid imageBuffer length");
        }

        writeCommand(SSD1306_COLUMNADDR);
        writeCommand((byte) 0); // Column start address (0 = reset)
        writeCommand((byte) (DISPLAY_WIDTH - 1)); // Column end address (127 = reset)

        writeCommand(SSD1306_PAGEADDR);
        writeCommand((byte) 0); // Page start address (0 = reset)
        writeCommand((byte) 7); // Page end address

        // Send display contents in 16 byte chunks
        for (int i = 0; i < ((DISPLAY_WIDTH * DISPLAY_HEIGHT / 8) / 16); i++) {
            writeRegister(0x40, imageBuffer, i * 16, 16);
        }
    }

    public synchronized void shutdown() {
        if (!shutdown) {
            LOGGER.log(Level.INFO, "Shutting down OLED driver");
            try {
                // before we shut down we clear the display and switch it off
                switchOff();
                LOGGER.log(Level.INFO, "Switched off OLED panel");
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Exception closing i2c bus", e);
            }
            shutdown = true;
        }
    }
}
