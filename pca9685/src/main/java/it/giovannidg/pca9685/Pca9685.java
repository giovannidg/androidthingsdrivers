package it.giovannidg.pca9685;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 * Created by Open Reply on 09/01/18.
 */

@TargetApi(Build.VERSION_CODES.KITKAT)
public class Pca9685 implements AutoCloseable {

    private static final String LOG_TAG = Pca9685.class.getSimpleName();

    private String busAddress;
    private I2cDevice mDevice;

    public Pca9685(String busAddress, Pca9685Listener listener) throws IOException {
        this.busAddress = busAddress;
//       # Setup I2C interface for the device.
        PeripheralManagerService pioService = new PeripheralManagerService();
        I2cDevice device = pioService.openI2cDevice(busAddress, Pca9685Specs.PCA9685_ADDRESS);
        mDevice = device;

        new Init(listener).execute();
    }


    private class Init extends AsyncTask<Void, Void, Void> {

        private Pca9685Listener listener;

        public Init(Pca9685Listener listener) {
            this.listener = listener;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            setAllPwm(0, 0);
            try {
                mDevice.writeRegByte(Pca9685Specs.MODE2, Pca9685Specs.OUTDRV);
                mDevice.writeRegByte(Pca9685Specs.MODE1, Pca9685Specs.ALLCALL);
                Thread.sleep(5);                                //# wait for oscillator
                byte mode1 = mDevice.readRegByte(Pca9685Specs.MODE1);
                Log.d(LOG_TAG, "mode1: " + ((int) mode1));
                mode1 = (byte) (mode1 & ~Pca9685Specs.SLEEP);         //# wake up (reset sleep)
                Log.d(LOG_TAG, "new mode1: " + ((int) mode1));
                mDevice.writeRegByte(Pca9685Specs.MODE1, mode1);
                Thread.sleep(5);                                //# wait for oscillator
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (listener != null) listener.onInitEnd();
        }
    }

    public interface Pca9685Listener {
        void onFrequencySet(boolean isSet);

        void onInitEnd();
    }

    /**
     * Set the PWM frequency to the provided value in hertz (40&lt;freqHz&gt;1000).
     *
     * @param freqHZ
     */
    public void setPWMfreq(float freqHZ, Pca9685Listener listener) throws FrequencyOutOfRange {
        if (freqHZ < Pca9685Specs.MIN_FREQUENCY_HZ || freqHZ > Pca9685Specs.MAX_FREQUENCY_HZ)
            throw new FrequencyOutOfRange();
        new SetFreq(listener).execute(freqHZ);
    }

    private class SetFreq extends AsyncTask<Float, Void, Boolean> {

        Pca9685Listener listener;

        public SetFreq(Pca9685Listener listener) {
            this.listener = listener;
        }

        @Override
        protected Boolean doInBackground(Float... freqHZ) {
            try {
                float freq = freqHZ[0];
                Log.d(LOG_TAG, "" + freq);
                float prescaleval = Pca9685Specs.INTERNAL_CLOCK_FREQ;    //# 25MHz
                prescaleval /= Pca9685Specs.PWM_STEPS; //# 12-bit
                prescaleval /= freq;
                prescaleval -= 1.0;

                Log.d(LOG_TAG, String.format("Setting PWM frequency to %1f Hz", freq));
                Log.d(LOG_TAG, String.format("Estimated pre-scale: %1f", prescaleval));

                int prescale = (int) Math.floor(prescaleval + 0.5);
                Log.d(LOG_TAG, String.format("Estimated pre-scale: %1f", (float) prescale));

                byte oldMode = mDevice.readRegByte(Pca9685Specs.MODE1);
                Log.d(LOG_TAG, "old mode" + (int) oldMode);
                byte newMode = (byte) ((oldMode & 0x7F) | 0x10);
                Log.d(LOG_TAG, "old mode" + (int) newMode);

                mDevice.writeRegByte(Pca9685Specs.MODE1, newMode);
                mDevice.writeRegByte(Pca9685Specs.PRESCALE, (byte) prescale);
                mDevice.writeRegByte(Pca9685Specs.MODE1, oldMode);

                Thread.sleep(5);

                mDevice.writeRegByte(Pca9685Specs.MODE1, (byte) (oldMode | 0x80));
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (listener != null) listener.onFrequencySet(aBoolean);
        }
    }


    public boolean setPwm(int channel, int on, int off) {
        //Sets a single PWM channel.
        try {
            Log.d(LOG_TAG, "before");
            Log.d(LOG_TAG, "LED0_ON_L: " + (int) mDevice.readRegByte(Pca9685Specs.LED0_ON_L + 4 * channel));
            Log.d(LOG_TAG, "LED0_ON_H: " + (int) mDevice.readRegByte(Pca9685Specs.LED0_ON_H + 4 * channel));
            Log.d(LOG_TAG, "LED0_OFF_L: " + (int) mDevice.readRegByte(Pca9685Specs.LED0_OFF_L + 4 * channel));
            Log.d(LOG_TAG, "LED0_OFF_H: " + (int) mDevice.readRegByte(Pca9685Specs.LED0_OFF_H + 4 * channel));

            mDevice.writeRegByte(Pca9685Specs.LED0_ON_L + 4 * channel, (byte) (on & 0xFF));
            mDevice.writeRegByte(Pca9685Specs.LED0_ON_H + 4 * channel, (byte) (on >> 8));
            mDevice.writeRegByte(Pca9685Specs.LED0_OFF_L + 4 * channel, (byte) (off & 0xFF));
            mDevice.writeRegByte(Pca9685Specs.LED0_OFF_H + 4 * channel, (byte) (off >> 8));

            Log.d(LOG_TAG, "after");
            Log.d(LOG_TAG, "LED0_ON_L: " + (int) mDevice.readRegByte(Pca9685Specs.LED0_ON_L + 4 * channel));
            Log.d(LOG_TAG, "LED0_ON_H: " + (int) mDevice.readRegByte(Pca9685Specs.LED0_ON_H + 4 * channel));
            Log.d(LOG_TAG, "LED0_OFF_L: " + (int) mDevice.readRegByte(Pca9685Specs.LED0_OFF_L + 4 * channel));
            Log.d(LOG_TAG, "LED0_OFF_H: " + (int) mDevice.readRegByte(Pca9685Specs.LED0_OFF_H + 4 * channel));

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setDutyCycle(int channel, int dutyCycle) {
        int off = (4096 * dutyCycle) / 100;
        return setPwm(channel, 0, off);
    }

    public boolean setAllPwm(int on, int off) {
        //Sets all PWM channels.
        try {
            mDevice.writeRegByte(Pca9685Specs.ALL_LED_ON_L, (byte) (on & 0xFF));
            mDevice.writeRegByte(Pca9685Specs.ALL_LED_ON_H, (byte) (on >> 8));
            mDevice.writeRegByte(Pca9685Specs.ALL_LED_OFF_L, (byte) (off & 0xFF));
            mDevice.writeRegByte(Pca9685Specs.ALL_LED_OFF_H, (byte) (off >> 8));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Close the driver and the underlying device.
     */
    @Override
    public void close() throws IOException {
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    /**
     * TO BE TESTED
     *
     * @param bus
     * @throws IOException
     */
    private static void softwareReset(String bus) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        I2cDevice device = pioService.openI2cDevice(bus, Pca9685Specs.PCA9685_ADDRESS_FOR_RESET);
        if (device != null) {
            device.writeRegByte(0x00, (byte) 0x06); // TODO: 09/01/18 test me
            device.close();
        } else {
            Log.d(LOG_TAG, "Unable to open the connection");
        }
    }

    private class FrequencyOutOfRange extends Error {
        @Override
        public String getMessage() {
            return "Frequency in HZ must stay between 40HZ and 1000HZ (1KHZ)";
        }
    }

}
