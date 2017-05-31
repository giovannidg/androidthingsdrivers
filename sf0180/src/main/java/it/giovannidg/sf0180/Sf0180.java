package it.giovannidg.sf0180;

import android.util.Log;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.Pwm;

import java.io.IOException;

/**
 * Created by gdigialluca on 31/05/17.
 */

public class Sf0180 implements AutoCloseable {

    private Pwm mMotor;
    private PeripheralManagerService manager;

    private static final String TAG = "SF0180";


    private static final double MIN_DUTYCYCLE = 3.75;
    private static final double MAX_DUTYCYCLE = 11.25;
    private static final double MAX_DEGREES = 180;
    private static final double MIN_DEGREES = 0;


    public Sf0180(Pwm mMotor) {
        this.mMotor = mMotor;
    }

    public Sf0180(String pin) throws IOException {
        manager = new PeripheralManagerService();
        mMotor = manager.openPwm(pin);
        goToNeutralPosition();
    }

    public void goToNeutralPosition() throws IOException {
        mMotor.setPwmFrequencyHz(50);
        mMotor.setPwmDutyCycle(7.5);
        // Enable the PWM signal
        mMotor.setEnabled(true);
    }

    public void goToZeroPosition() throws IOException {
        mMotor.setPwmFrequencyHz(50);
        mMotor.setPwmDutyCycle(3.75);
        // Enable the PWM signal
        mMotor.setEnabled(true);
    }

    public void goTo180Position() throws IOException {
        mMotor.setPwmFrequencyHz(50);
        mMotor.setPwmDutyCycle(11.25);
        // Enable the PWM signal
        mMotor.setEnabled(true);
    }

    public void goToDegreesPosition(double degrees) throws IOException {
        mMotor.setPwmFrequencyHz(50);
        mMotor.setPwmDutyCycle(fromDegreesToDutyCycle(degrees));
        // Enable the PWM signal
        mMotor.setEnabled(true);
    }

    private double fromDegreesToDutyCycle(double degrees) {
        degrees = degrees % 180;
        return MIN_DUTYCYCLE + (((MAX_DUTYCYCLE - MIN_DUTYCYCLE) / (MAX_DEGREES - MIN_DEGREES)) * degrees);
    }

    @Override
    public void close() {
        try {
            mMotor.close();
        } catch (IOException e) {
            Log.e(TAG, "error on closing GPIO");
        } finally {
            mMotor = null;
            manager = null;
        }
    }
}
