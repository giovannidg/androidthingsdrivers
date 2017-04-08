/*
 * Copyright 2017 Giovanni Di Gialluca.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package iotlab.open.it.adc0832;

import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 * Driver for Analog-Digital converter Adc0832 using GPIO
 */
public class Adc0832 implements AutoCloseable {

    private static final String TAG = "ADC0832";
    /**
     * Default value
     */
    private String pinCLK = DEFAULT_PI_PIN_CLK;
    private String pinD0 = DEFAULT_PI_PIN_D0;
    private String pinD1 = DEFAULT_PI_PIN_D1;
    private String pinCS = DEFAULT_PI_PIN_CS;

    public static final String DEFAULT_PI_PIN_CLK = "BCM16";
    public static final String DEFAULT_PI_PIN_D0 = "BCM20";
    public static final String DEFAULT_PI_PIN_D1 = "BCM21";
    public static final String DEFAULT_PI_PIN_CS = "BCM17";

    public static final int CHANNEL_0 = 0;
    public static final int CHANNEL_1 = 1;

    private PeripheralManagerService manager;

    private Gpio gpioCLK;
    private Gpio gpioD0;
    private Gpio gpioD1;
    private Gpio gpioCS;

    /**
     * Create a new Adc0832 defining all the GPIO pins
     *
     * @param pinCLK GPIO pin for clock
     * @param pinD0  GPIO pin for D0 adc pin
     * @param pinD1  GPIO pin for D1 adc pin
     * @param pinCS  GPIO pin for CS
     * @throws IOException
     */
    public Adc0832(String pinCLK, String pinD0, String pinD1, String pinCS) throws IOException {
        this.pinCLK = pinCLK;
        this.pinD0 = pinD0;
        this.pinD1 = pinD1;
        this.pinCS = pinCS;
        //TODO check all pins are different

        try {
            connect();
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    /**
     * Create a new Adc0832 using the defaults GPIO pins
     *
     * @throws IOException
     */
    public Adc0832() throws IOException {
        try {
            connect();
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    private void connect() throws IOException {
        manager = new PeripheralManagerService();

        gpioCLK = manager.openGpio(pinCLK);
        gpioD0 = manager.openGpio(pinD0);
        gpioD1 = manager.openGpio(pinD1);
        gpioCS = manager.openGpio(pinCS);

        gpioD0.setDirection(Gpio.DIRECTION_IN);
        gpioD1.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
        gpioCLK.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
        gpioCS.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);

        gpioD0.setActiveType(Gpio.ACTIVE_HIGH);
        gpioD1.setActiveType(Gpio.ACTIVE_HIGH);
        gpioCLK.setActiveType(Gpio.ACTIVE_HIGH);
        gpioCS.setActiveType(Gpio.ACTIVE_HIGH);

        gpioD0.setEdgeTriggerType(Gpio.EDGE_RISING);
       new Thread(new Runnable() {
            public void run() {
                for (int i=0;i<10;i++) {
                    try {
                        gpioD1.setValue(i%2==0);
                        Thread.sleep(250);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * Read value from a channel
     *
     * @param channel CHANNEL_0 or CHANNEL_1
     * @return digital value
     * @throws IOException
     */
    public int getADCChannelValue(int channel) throws IOException {
        if (channel != 0 & channel != 1) return Integer.MIN_VALUE;

        int ad = 0;

        // CS LOW.
        gpioCS.setValue(true);
        gpioCS.setValue(false);

        // Start clock
        gpioCLK.setValue(false);

        // Input MUX address
        for (int i = 0; i < 3; i++) {
            if (i == 0 || i == 1 || channel == 1)
                gpioD1.setValue(true);
            else gpioD1.setValue(false);

            gpioCLK.setValue(true);
            gpioCLK.setValue(false);
        }

        // Read 8 bits from ADC
        for (int i = 0; i < 8; i++) {
            gpioCLK.setValue(true);
            gpioCLK.setValue(false);
            ad = ad << 1;
            boolean value = gpioD0.getValue();
            if (value)
                ad |= 0x01;
        }

        // Reset
        gpioCS.setValue(true);

        return ad;
    }

    @Override
    public void close() {
        try {
            if (gpioCLK != null) gpioCLK.close();
            if (gpioD0 != null) gpioD0.close();
            if (gpioD1 != null) gpioD1.close();
            if (gpioCS != null) gpioCS.close();
        } catch (IOException e) {
            Log.e(TAG, "error on closing GPIO");
        } finally {
            gpioCLK = null;
            gpioD0 = null;
            gpioD1 = null;
            gpioCS = null;
        }
    }
}
