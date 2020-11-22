package aobtk.hw;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pi4j.exception.LifecycleException;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;
import com.pi4j.io.i2c.I2CProvider;

public class I2CDevice {
    private I2C i2c;

    private static final Logger LOGGER = Logger.getLogger(Bonnet.class.getCanonicalName());

    I2CDevice(int busNum, int deviceAddr) {
        LOGGER.log(Level.FINE, String.format("Opening I2C bus %d", busNum));
        try {
            I2CProvider i2cProvider = Bonnet.pi4j.i2c();
            I2CConfig config = I2C.newConfigBuilder(Bonnet.pi4j).id("i2c-bus").name("I2C Bus").bus(busNum)
                    .device(deviceAddr).build();
            i2c = i2cProvider.create(config);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, "Exception opening PI4J instance or I2C bus: " + e, e);
        }
        if (i2c == null) {
            LOGGER.log(Level.SEVERE, "Could not open I2C bus " + busNum + " -- is I2C enabled?");
            shutdown();
            System.exit(1);
        }
        LOGGER.log(Level.FINE, String.format("Opened I2C bus %d, device 0x%02x", busNum, deviceAddr));
        Bonnet.i2cDevices.add(this);
    }

    public void writeRegister(int register, byte command) throws IOException {
        if (i2c != null) {
            i2c.writeRegister(register, command);
        } else {
            LOGGER.log(Level.SEVERE, "Tried to write to null I2C device");
        }
    }

    public void writeRegister(int register, byte[] bytes, int start, int length) throws IOException {
        if (i2c != null) {
            i2c.writeRegister(register, bytes, start, length);
        } else {
            LOGGER.log(Level.SEVERE, "Tried to write to null I2C device");
        }
    }

    public void shutdown() {
        Bonnet.i2cDevices.remove(this);
        if (i2c != null) {
            try {
                i2c.shutdown(Bonnet.pi4j);
            } catch (LifecycleException e) {
                // Ignore
            }
            i2c = null;
        }
    }
}