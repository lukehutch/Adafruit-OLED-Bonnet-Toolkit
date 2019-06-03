# Adafruit OLED Bonnet Toolkit
Java driver toolkit for the [Adafruit 128x64 OLED bonnet for Raspberry Pi, with buttons and D-pad](https://www.adafruit.com/product/3531).
<br>
*(This code is not associated with or endorsed by Adafruit. Adafruit is a trademark of Limor "Ladyada" Fried.)*

<p align="center"><a href="https://raw.githubusercontent.com/lukehutch/Adafruit-OLED-Bonnet-Toolkit/master/rpi-assembly.jpg"><img alt="Raspberry Pi hardware assembly" height="297" width="752" src="https://raw.githubusercontent.com/lukehutch/Adafruit-OLED-Bonnet-Toolkit/master/rpi-assembly.jpg"></a><br><i>Hardware assembly: The OLED bonnet mounted on a Raspberry Pi Zero,<br>which is further mounted on an (optional) Zero4U USB hub.</i></p>

<p align="center"><a href="https://raw.githubusercontent.com/lukehutch/usb-copier/master/screen-en.jpg"><img alt="USB copier screenshot, English" width="375" height="280" src="https://raw.githubusercontent.com/lukehutch/usb-copier/master/screen-en.jpg"></a>&nbsp;<a href="https://raw.githubusercontent.com/lukehutch/usb-copier/master/screen-en.jpg"></a><a href"https://raw.githubusercontent.com/lukehutch/usb-copier/master/screen-ko.jpg">&nbsp;&nbsp;<img alt="USB copier screenshot, Korean" width="375" height="280" src="https://raw.githubusercontent.com/lukehutch/usb-copier/master/screen-ko.jpg"></a>
<br><i>The <a href="https://github.com/lukehutch/usb-copier">USB copier demo application</a>, showing highlighted menus arranged using the built-in<br>
UI layout system, and limited support for internationalization (Latin1 and Korean fonts included).</i></p>


This toolkit has support for drawing text or pixels on the OLED screen of the bonnet, through the OLED screen's SSD1306 chipset,
and for receiving events from the D-pad and buttons through the GPIO interface. It also includes a UI layout library, and some
task scheduling classes to make it easy to build an asynchronous application that does not block the screen update thread.

The code is currently designed only for a text UI, although you can write individual pixels to the display, so you could use a Java rendering library
to create a `BufferedImage`, and copy the image over to the display a pixel at a time. 

## Raspberry Pi config

Make sure your `/boot/config.txt` file contains the following two lines (to enable I2C, and increase the baudrate to 1Mbaud, to dramatically increase the display update rate).

```
dtparam=i2c_arm=on
dtparam=i2c_baudrate=1000000
```

## Simple example

[See a simple demo application here.](https://github.com/lukehutch/Adafruit-OLED-Bonnet-Toolkit/tree/master/src/main/java/demo)

## More complex example

For a complete working example of an asynchronous UI application powered by this toolkit, see the [usb-copier](https://github.com/lukehutch/usb-copier) project. 

### Code used in this project:

* OLED driver code and 4x5/5x8 fonts from [Pi-OLED](https://github.com/entrusc/Pi-OLED) by Florian Frankenberger, which is a port of
the [Adafruit_SSD1306](https://github.com/adafruit/Adafruit_SSD1306) Python driver for the SSD1306 OLED driver.
* [Latin1/Korean 16x16](https://github.com/Dalgona/neodgm/blob/master/font.py) font by Dalgona. 
* [WenQuanYi Song](http://wenq.org/wqy2/index.cgi?BitmapSong_en) Unicode bitmap font for Latin1, unified CJK ideographs, and Hangeul.
