package it.giovannidg.driversandroidthings;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.util.List;

import it.giovannidg.adc0832.Adc0832;
import it.giovannidg.pca9685.Pca9685;
import it.giovannidg.sf0180.Sf0180;

public class DriverTestActivity extends Activity {

    private static String TAG = DriverTestActivity.class.getCanonicalName();
    private static String ACTION = "it.giovannidg.driver.intent.TEST";
    private static String EXTRA_KEY = "COMMAND";
    private static final String COMMAND_READ_ANALOG = "COMMAND_READ_ANALOG";
    private static String PWM_RPI_PORT = "PWM1";
    private static int PCA9685_CHANNEL = 0;
    private static final int UP_KEY = 19;
    private static final int DOWN_KEY = 20;
    private static final int LEFT_KEY = 21;
    private static final int RIGHT_KEY = 22;
    private static final int OK_KEY = 66;

    private TextView valueTextView;
    private Adc0832 mAdc0832;
    private Sf0180 mMotor;
    private Pca9685 motorDriver = null;
    private Handler mHandler;

    private SeekBar motorPositionSeekBar;

    BroadcastReceiver commandsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive");
            String data = (intent.getStringExtra(EXTRA_KEY) != null)
                    ? intent.getStringExtra(EXTRA_KEY) : "";
            Log.i(TAG, data);
            if (data != null) {
                switch (data) {
                    case COMMAND_READ_ANALOG:
                        readAnalogData();
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();

        if (hasUI()) {
            setContentView(R.layout.activity_main);
            valueTextView = (TextView) findViewById(R.id.value_text);
            //ButtonTest
            findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    readAnalogData();
                }
            });

            motorPositionSeekBar = (SeekBar) findViewById(R.id.motor_position);
            motorPositionSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    updateMotorPosition();
                }
            });
        }

        // IntentFilter
        // adb shell am broadcast -a it.giovannidg.driver.intent.TEST --es COMMAND "COMMAND_READ_ANALOG"
        IntentFilter filterSend = new IntentFilter();
        filterSend.addAction(ACTION);
        filterSend.setPriority(999);
        registerReceiver(commandsReceiver, filterSend);

        //list available ports
        PeripheralManagerService peripheralManager = new PeripheralManagerService();
        List<String> portList = peripheralManager.getGpioList();
        List<String> pwmList = peripheralManager.getPwmList();
        List<String> i2CBusList = peripheralManager.getI2cBusList();

        if (portList.isEmpty())
            Log.e(TAG, "No GPIO port available on this device.");
        else
            Log.i(TAG, "List of available ports: " + portList);

        if (pwmList.isEmpty())
            Log.i(TAG, "No PWM port available on this device.");
        else
            Log.i(TAG, "Available PWM ports: " + pwmList);

        if (i2CBusList.isEmpty())
            Log.i(TAG, "No I2C port available on this device.");
        else
            Log.i(TAG, "Available I2C ports: " + i2CBusList);

        //Set up Adc0832
        try {
            mAdc0832 = new Adc0832(Adc0832.DEFAULT_PI_PIN_CLK, Adc0832.DEFAULT_PI_PIN_D0,
                    Adc0832.DEFAULT_PI_PIN_D1, Adc0832.DEFAULT_PI_PIN_CS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Set up Sf0180
        try {
            mMotor = new Sf0180(PWM_RPI_PORT);
//            new TestMotorTask().execute(mMotor);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //setup Pca9685
        try {
            motorDriver = new Pca9685(i2CBusList.get(0), new Pca9685.Pca9685Listener() {
                @Override
                public void onFrequencySet(boolean isSet) {
                }

                @Override
                public void onInitEnd() {
                    motorDriver.setPWMfreq(100, this);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d("key", "KeyCode: " + keyCode + " event: " + event.getKeyCode());
        switch (keyCode) {
            case UP_KEY: {
                motorDriver.setPwm(PCA9685_CHANNEL, 100, 600);
            }
            break;
            case DOWN_KEY: {
                motorDriver.setPwm(PCA9685_CHANNEL, 100, 1100);
            }
            break;
            case LEFT_KEY: {
                motorDriver.setPwm(PCA9685_CHANNEL, 100, 1500);
            }
            break;
            case RIGHT_KEY: {
                motorDriver.setPwm(PCA9685_CHANNEL, 100, 2100);
            }
            break;
            case OK_KEY: {
                motorDriver.setPwm(PCA9685_CHANNEL, 100, 2600);
            }
            break;
        }
        return true;
    }

    private void updateMotorPosition() {
        int progress = motorPositionSeekBar.getProgress();
        //double degrees= (180*progress)/100;
        double degrees = (double) progress;
        try {
            mMotor.goToDegreesPosition(degrees);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(commandsReceiver);
        mAdc0832.close();
    }

    private void readAnalogData() {
        Thread myThread = new Thread(new Runnable() {
            @Override
            public void run() {
                readValueAndLog();
            }
        });
        myThread.start();
    }

    private void readValueAndLog() {
        for (int i = 0; i < 10; i++) {
            try {
                final int a = mAdc0832.getADCChannelValue(Adc0832.CHANNEL_0);
                final int b = mAdc0832.getADCChannelValue(Adc0832.CHANNEL_1);
                Log.d(TAG, a + " - " + b);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        valueTextView.setText(a + " - " + b);
                    }
                });
                Thread.sleep(400);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean hasUI() {
        switch (Build.DEVICE) {
            case "edison":
                return false;
            case "rpi3":
                return true;
            case "imx6ul":
                return false;
        }
        return false;
    }

    public boolean runMotorTask = true;

    private class TestMotorTask extends AsyncTask<Sf0180, Void, Void> {

        @Override
        protected Void doInBackground(Sf0180... motors) {
            try {
                Sf0180 motor = motors[0];

                do {
                    motor.goToZeroPosition();
                    Thread.sleep(1000);
                    motor.goToNeutralPosition();
                    Thread.sleep(1000);
                    motor.goTo180Position();
                    Thread.sleep(1000);
                } while (runMotorTask);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}
