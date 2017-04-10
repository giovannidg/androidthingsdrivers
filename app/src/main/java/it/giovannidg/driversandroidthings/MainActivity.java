package it.giovannidg.driversandroidthings;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.util.List;

import it.giovannidg.adc0832.Adc0832;

public class MainActivity extends Activity {

    private static String TAG = MainActivity.class.getCanonicalName();
    private static String ACTION = "it.giovannidg.driver.intent.TEST";
    private static String EXTRA_KEY = "COMMAND";
    private static final String COMMAND_READ_ANALOG = "COMMAND_READ_ANALOG";

    private TextView valueTextView;
    private Adc0832 mAdc0832;
    private Handler mHandler;

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
        }

        // IntentFilter
        // adb shell am broadcast -a it.giovannidg.driver.intent.TEST --es COMMAND "COMMAND_READ_ANALOG"
        IntentFilter filterSend = new IntentFilter();
        filterSend.addAction(ACTION);
        filterSend.setPriority(999);
        registerReceiver(commandsReceiver, filterSend);

        //list all GPIOs
        PeripheralManagerService manager = new PeripheralManagerService();
        List<String> portList = manager.getGpioList();
        if (portList.isEmpty()) {
            Log.i(TAG, "No GPIO port available on this device.");
        } else {
            Log.i(TAG, "List of available ports: " + portList);
        }

        //set up mAdc0832
        try {
            mAdc0832 = new Adc0832(Adc0832.DEFAULT_PI_PIN_CLK, Adc0832.DEFAULT_PI_PIN_D0,
                    Adc0832.DEFAULT_PI_PIN_D1, Adc0832.DEFAULT_PI_PIN_CS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mHandler = new Handler();
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
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
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

}
