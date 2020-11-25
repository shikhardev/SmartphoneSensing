package com.example.tudwifi;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;


public class MainActivity extends AppCompatActivity implements  SensorEventListener{
    private ActivityFunctionalities af = new ActivityFunctionalities(this);
    private Wifi_Functionalities wf;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    public static boolean isAccelTraining_Still = false;
    public static boolean isAccelTraining_Move = false;
    private final int ACCEL_TRACK_BUFFER_SIZE = 20;
    private float[] trackX = new float[ACCEL_TRACK_BUFFER_SIZE];
    private float[] trackY = new float[ACCEL_TRACK_BUFFER_SIZE];
    private float[] trackZ = new float[ACCEL_TRACK_BUFFER_SIZE];
    private int trackPointer = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hideAllExcept(R.id.homeLayout);

        ListView listView = (ListView) findViewById(R.id.wifi_train_wifiList);
        wf =  new Wifi_Functionalities(this, listView, this);


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer == null)
            Toast.makeText(this, "Accelerometer unavailable", Toast.LENGTH_SHORT).show();

        if (!af.isTrainingComplete()) {
            try {
                af.getAccelTestData();
            } catch (JSONException e) {
                e.printStackTrace();
//                tv.setText("Accelerometer Training Data not found");
            }
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    private void hideAllExcept (int id) {
        LinearLayout l;
        ConstraintLayout c = (ConstraintLayout) findViewById(R.id.homeLayout);
        c.setVisibility(View.GONE);

        l = (LinearLayout) findViewById(R.id.wifiTrainLayout);
        l.setVisibility(LinearLayout.GONE);

        l = (LinearLayout) findViewById(R.id.wifiTestLayout);
        l.setVisibility(LinearLayout.GONE);


        l = (LinearLayout) findViewById(R.id.accelTrainLayout);
        l.setVisibility(View.GONE);

        if (id == R.id.homeLayout) {
            c.setVisibility(View.VISIBLE);
        }
        else {
            l = findViewById(id);
            l.setVisibility(View.VISIBLE);
        }
    }

    public void Button_Click_Handler (View view) {
        switch (view.getId()) {
            case R.id.home_wifiTestButton:
                wf.parseConfigFileToDB (readFromConfigFile(), false);
                Gson gson = new Gson();
                String json = gson.toJson(wf.testBayesianInstance);
                hideAllExcept(R.id.wifiTestLayout);
                break;
            case R.id.home_wifiTrainButton:
                wf.parseConfigFileToDB(readFromConfigFile(), true);
                hideAllExcept(R.id.wifiTrainLayout);
                break;
            case R.id.home_accelTrainButton:
                hideAllExcept(R.id.accelTrainLayout);
                break;
            case R.id.accel_trainStillButton:
                af.stillMaxMinX = new ArrayList<String>();
                af.stillMaxMinY = new ArrayList<String>();
                af.stillMaxMinZ = new ArrayList<String>();
                af.trainingComplete = false;
                trainAccelStill();
                break;
            case R.id.accel_trainMoveButton:
                af.moveMaxMinX = new ArrayList<String>();
                af.moveMaxMinY = new ArrayList<String>();
                af.moveMaxMinZ = new ArrayList<String>();
                af.trainingComplete = false;
                trainAccelMove();
                break;

            case R.id.wifi_train_showWifiButton:
                wf.showWifiResults();
                break;

            case R.id.wifi_train_selectedCell_1:
            case R.id.wifi_train_selectedCell_2:
            case R.id.wifi_train_selectedCell_3:
            case R.id.wifi_train_selectedCell_4:
            case R.id.wifi_train_selectedCell_5:
            case R.id.wifi_train_selectedCell_6:
            case R.id.wifi_train_selectedCell_7:
            case R.id.wifi_train_selectedCell_8:
            case R.id.wifi_train_selectedCell_9:
            case R.id.wifi_train_selectedCell_10:
            case R.id.wifi_train_selectedCell_11:
            case R.id.wifi_train_selectedCell_12:
            case R.id.wifi_train_selectedCell_13:
            case R.id.wifi_train_selectedCell_14:
            case R.id.wifi_train_selectedCell_15:
            case R.id.wifi_train_selectedCell_16:
                Button selectedButton = (Button) findViewById(view.getId());
                wf.trainCell(selectedButton);
                break;

            case R.id.wifi_test_locateMeButton:
                clearAllTestButtons ();
                TextView trainTV = findViewById(R.id.wifi_localisation_result_TV);
                boolean isDataAvailable  = true;

                if (isDataAvailable == true) {
                    Button b = (Button) findViewById(view.getId());
                    b.setEnabled(false);
                    wf.locateMe();
                } else {
                    trainTV.setText("Training Data unavailable");
                }
                break;

            case R.id.wifi_train_Save:
                TextView testTV = (TextView) findViewById(R.id.wifi_train_statusTV);
                try {
                    writeToConfigFile(wf.getJSON());
//                    testTV.setText("Normalised and appended " + Integer.toString(wf.getDBSize()) + " WiFi data.");
                } catch (Exception e) {
                    testTV.setText("Storage unsuccessful");
                }
                break;


            case R.id.wifi_train_clearDB:

                testTV = (TextView) findViewById(R.id.wifi_train_statusTV);
                try {
                    writeToConfigFile("{}");
                    testTV.setText("Database cleared.");
                }
                catch (Exception e) {

                    testTV.setText("Storage unsuccessful");
                }
                break;


        }
    }

    private void writeToConfigFile(String data) throws IOException {
        if (data.equals("{}"))
            wf.clearAllDB();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput("config.txt", MODE_PRIVATE));
        outputStreamWriter.write(data);
        outputStreamWriter.close();
    }

    private String readFromConfigFile() {

        String ret = "";

        try {
            InputStream inputStream = openFileInput("config.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        } catch (FileNotFoundException e) {
            Log.e("read", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("read", "Can not read file: " + e.toString());
        }

        return ret;
    }


    private void clearAllTestButtons() {
        Button b = (Button) findViewById(R.id.wifi_locationCell_1);
        b.setBackgroundResource(android.R.drawable.btn_default);
        b = (Button) findViewById(R.id.wifi_locationCell_2);
        b.setBackgroundResource(android.R.drawable.btn_default);
        b = (Button) findViewById(R.id.wifi_locationCell_3);
        b.setBackgroundResource(android.R.drawable.btn_default);
        b = (Button) findViewById(R.id.wifi_locationCell_4);
        b.setBackgroundResource(android.R.drawable.btn_default);
        b = (Button) findViewById(R.id.wifi_locationCell_5);
        b.setBackgroundResource(android.R.drawable.btn_default);
        b = (Button) findViewById(R.id.wifi_locationCell_6);
        b.setBackgroundResource(android.R.drawable.btn_default);
        b = (Button) findViewById(R.id.wifi_locationCell_7);
        b.setBackgroundResource(android.R.drawable.btn_default);
        b = (Button) findViewById(R.id.wifi_locationCell_8);
        b.setBackgroundResource(android.R.drawable.btn_default);
        b = (Button) findViewById(R.id.wifi_locationCell_9);
        b.setBackgroundResource(android.R.drawable.btn_default);
        b = (Button) findViewById(R.id.wifi_locationCell_10);
        b.setBackgroundResource(android.R.drawable.btn_default);
        b = (Button) findViewById(R.id.wifi_locationCell_11);
        b.setBackgroundResource(android.R.drawable.btn_default);
        b = (Button) findViewById(R.id.wifi_locationCell_12);
        b.setBackgroundResource(android.R.drawable.btn_default);
        b = (Button) findViewById(R.id.wifi_locationCell_13);
        b.setBackgroundResource(android.R.drawable.btn_default);
        b = (Button) findViewById(R.id.wifi_locationCell_14);
        b.setBackgroundResource(android.R.drawable.btn_default);
        b = (Button) findViewById(R.id.wifi_locationCell_15);
        b.setBackgroundResource(android.R.drawable.btn_default);
        b = (Button) findViewById(R.id.wifi_locationCell_16);
        b.setBackgroundResource(android.R.drawable.btn_default);

    }

    /*
     * Takes control to the home screen (Test / Train + <Activities>
     * */
    public void takeMeHome(View view) {
        clearAllTestButtons();
        hideAllExcept(R.id.homeLayout);
        Button b = (Button) findViewById(R.id.home_wifiTestButton);
        b.setEnabled(true);
        TextView tv = (TextView) findViewById(R.id.wifi_localisation_result_TV);
        tv.setText("");
        tv = (TextView) findViewById(R.id.wifi_train_statusTV);
        tv.setText("");

    }


    public void updateAccelInTV (float x, float y, float z) {
        TextView currentX = (TextView) findViewById(R.id.currentX);
        TextView currentY = (TextView) findViewById(R.id.currentY);
        TextView currentZ = (TextView) findViewById(R.id.currentZ);
        currentX.setText("0.0");
        currentY.setText("0.0");
        currentZ.setText("0.0");
        currentX.setText(Float.toString(x));
        currentY.setText(Float.toString(y));
        currentZ.setText(Float.toString(z));
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float x = 0, y = 0, z = 0;
        x = sensorEvent.values[0];
        y = sensorEvent.values[1];
        z = sensorEvent.values[2];
        updateAccelInTV(x, y, z);

        trackX[trackPointer] = x;
        trackY[trackPointer] = y;
        trackZ[trackPointer] = z;
        trackPointer++;

        // Send to the checking window
        if (trackPointer >= ACCEL_TRACK_BUFFER_SIZE) {
            trackPointer = 0;

            predictActivity();
            if (isAccelTraining_Move) {
                af.computeFeatureAndStoreInDB(trackX, trackY, trackZ, 1);
            } else if (isAccelTraining_Still) {
                af.computeFeatureAndStoreInDB(trackX, trackY, trackZ, 0);
            }
        }
    }

    // Function predicts activity once the buffer is full
    private void predictActivity () {
        TextView tv = findViewById(R.id.activityText);

        if (!af.isTrainingComplete()) {
            tv.setText("Accelerometer Not Trained");
            return;
        }

        int activityStatus = af.analyseActivity(trackX, trackY, trackZ);

        if (activityStatus == 0) {
            tv.setText("Still");
            tv.setTextColor(Color.rgb(13, 71, 161));
        } else {
            tv.setText ("Moving");
            tv.setTextColor(Color.rgb(13, 04, 196));
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    // Timer functions in the below two functions have been copied from below link
    // https://stackoverflow.com/questions/41368127/enable-and-disable-button-using-timer-in-android
    public void trainAccelMove() {
        isAccelTraining_Move = true;
        final Button b1 = findViewById(R.id.accel_trainStillButton);
        final Button b2 = findViewById(R.id.accel_trainMoveButton);
        b1.setEnabled(false);
        b2.setEnabled(false);
        trackPointer = 0;

        new CountDownTimer(10000, 10) { //Set Timer for 10 seconds
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                isAccelTraining_Move = false;
                b1.setEnabled(true);
                b2.setEnabled(true);
            }
        }.start();
    }

    public void trainAccelStill() {
        isAccelTraining_Still = true;
        final Button b1 = findViewById(R.id.accel_trainStillButton);
        final Button b2 = findViewById(R.id.accel_trainMoveButton);
        b1.setEnabled(false);
        b2.setEnabled(false);
        trackPointer = 0;

        new CountDownTimer(10000, 10) { //Set Timer for 10 seconds
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                isAccelTraining_Still = false;
                b1.setEnabled(true);
                b2.setEnabled(true);
            }
        }.start();
    }

    private String tempFunc () {
        String res = "{\"f8:23:b2:3c:83:98\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":0.6666667,\"21\":0.33333334,\"22\":0.0,\"23\":0.0,\"24\":0.0}}}},\"bc:54:f9:fc:90:e8\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":0.0,\"21\":0.0,\"22\":1.0,\"23\":0.0,\"24\":0.0}}}},\"c4:07:2f:48:78:bc\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":0.0,\"21\":0.0,\"22\":0.0,\"23\":1.0,\"24\":0.0}}}},\"9c:97:26:8b:15:e9\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":0.0,\"21\":1.0,\"22\":0.0,\"23\":0.0,\"24\":0.0}}}},\"18:35:d1:27:d0:b1\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":0.0,\"21\":1.0,\"22\":0.0,\"23\":0.0,\"24\":0.0}}}},\"5a:d3:43:fe:b0:89\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.5,\"19\":0.5,\"20\":0.0,\"21\":0.0,\"22\":0.0,\"23\":0.0,\"24\":0.0}}}},\"00:14:5c:98:c9:0e\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.33333334,\"19\":0.33333334,\"20\":0.33333334,\"21\":0.0,\"22\":0.0,\"23\":0.0,\"24\":0.0}}}},\"48:d3:43:fe:b0:89\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":1.0,\"20\":0.0,\"21\":0.0,\"22\":0.0,\"23\":0.0,\"24\":0.0}}}},\"00:1f:33:3f:ca:86\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":0.0,\"21\":0.0,\"22\":0.5,\"23\":0.5,\"24\":0.0}}}},\"c4:07:2f:48:78:b8\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.33333334,\"14\":0.6666667,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":0.0,\"21\":0.0,\"22\":0.0,\"23\":0.0,\"24\":0.0}}}},\"d4:6e:0e:b0:29:52\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":0.0,\"21\":0.5,\"22\":0.5,\"23\":0.0,\"24\":0.0}}}},\"30:d3:2d:5a:cd:57\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":1.0,\"21\":0.0,\"22\":0.0,\"23\":0.0,\"24\":0.0}}}},\"b0:4e:26:24:da:b4\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":0.0,\"21\":0.6666667,\"22\":0.33333334,\"23\":0.0,\"24\":0.0}}}},\"7c:8b:ca:20:48:76\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":0.33333334,\"21\":0.6666667,\"22\":0.0,\"23\":0.0,\"24\":0.0}}}}}";
        return res;
    }


}


//00796b