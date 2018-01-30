package it.giovannidg.pca9685;

/**
 * Created by Open Reply on 09/01/18.
 */

public final class Pca9685Specs {

    protected static final int INTERNAL_CLOCK_FREQ = 25 * 1000 * 1000; // 25 MHz
    protected static final int PWM_STEPS = 4096; // 12 Bit
    protected static final int MIN_FREQUENCY_HZ = 40; // 40 Hz
    protected static final int MAX_FREQUENCY_HZ = 1000; // 1 kHz

    // REGISTERS
    protected static final int PCA9685_ADDRESS = 0x40;
    protected static final int PCA9685_ADDRESS_FOR_RESET = 0x00; // SoftWareReset # SWRST
    protected static final int MODE1 = 0x00;
    protected static final int MODE2 = 0x01;
    protected static final int SUBADR1 = 0x02;
    protected static final int SUBADR2 = 0x03;
    protected static final int SUBADR3 = 0x04;
    protected static final byte LED0_ON_L = 0x06;
    protected static final byte LED0_ON_H = 0x07;
    protected static final byte LED0_OFF_L = 0x08;
    protected static final byte LED0_OFF_H = 0x09;
    protected static final int ALL_LED_ON_L = 0xFA;
    protected static final int ALL_LED_ON_H = 0xFB;
    protected static final int ALL_LED_OFF_L = 0xFC;
    protected static final int ALL_LED_OFF_H = 0xFD;
    protected static final int PRESCALE = 0xFE;

    // BIT MASKS
    protected static final int RESTART = 0x80;
    protected static final byte SLEEP = 0x10;
    protected static final byte ALLCALL = 0x01;
    protected static final int INVRT = 0x10;
    protected static final byte OUTDRV = 0x04;

}
