package com.example.tudwifi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

class scanComparator implements Comparator<ScanResult> {

    @Override
    public int compare(ScanResult lhs, ScanResult rhs) {
        return (lhs.level >rhs.level ? -1 : (lhs.level==rhs.level ? 0 : 1));
    }
}


public class Wifi_Functionalities extends MainActivity{
    private Context context;
    private Activity activity;
    private WifiManager wifiManager;
    private ListView listView;
    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayAdapter adapter;
    private final int WIFI_CONSIDERTION_COUNT = 20;
    public int scanMode = 0;

    public Bayesian_Localisation trainBayesianInstance;
    public Bayesian_Localisation testBayesianInstance;

    public Wifi_Functionalities (Context c, ListView l, Activity a) {
        this.context = c;
        this.activity = a;
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(context, "Please enable wifi", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }
        this.listView = l;
        adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);
    }

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = intent.getBooleanExtra(
                    WifiManager.EXTRA_RESULTS_UPDATED, false);
            if (success)
                successfulRead ();
            else
                failedRead ();

            context.unregisterReceiver(this);

        }
    };

    private void successfulRead() {
        List<ScanResult> scanResults = wifiManager.getScanResults();
        HashMap<String, Integer> wifiData = extractWifiData(scanResults);

        // Just Viewing
        if (scanMode == R.id.wifi_train_showWifiButton) {
            // Show the wifi results
            for (ScanResult i : scanResults) {
                arrayList.add(i.SSID + "\t\t:\t" + Integer.toString(i.level));
                adapter.notifyDataSetChanged();
            }
            TextView t = this.activity.findViewById(R.id.wifi_train_statusTV);
            t.setText("Ready");

            // Enable all buttons
            toggleAllWifiTrainButtons(true);

        }

        // Testing
        else if (scanMode == R.id.wifi_test_locateMeButton) {
            int locationCellID = testBayesianInstance.computeLocationV2 (wifiData);
            clearAllTestButtons();
            try {
                showLocationResult(locationCellID);
            }
            catch (Exception e) {
                Toast.makeText(this, "Location unavailable in DB", Toast.LENGTH_SHORT);
            }

        }

        // Training
        else if (scanMode != 0){
            // Cell id is also the scanMode
            trainBayesianInstance.appendTrainingData(scanMode, wifiData);

            TextView t = this.activity.findViewById(R.id.wifi_train_statusTV);
            t.setText("Ready");

            // Enable all buttons
            toggleAllWifiTrainButtons(true);
        }
        scanMode = 0;

    }

    private void failedRead() {
        TextView t;
        if (scanMode == R.id.wifi_test_locateMeButton) {
            t = this.activity.findViewById(R.id.wifi_localisation_result_TV);
            t.setText("Scan failed. Please retry.");
            Button b = this.activity.findViewById(R.id.wifi_test_locateMeButton);
            b.setEnabled(true);
        }
        if ((scanMode == R.id.wifi_train_showWifiButton) || (scanMode != 0)){
            t = this.activity.findViewById(R.id.wifi_train_statusTV);
            t.setText("Scan failed. Please retry.");
            toggleAllWifiTrainButtons(true);
        }

        return;
    }

    private void scanNow () {
        context.registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
    }

    private HashMap<String, Integer> extractWifiData (List<ScanResult>  scanResults) {

        scanComparator sc = new scanComparator();
        Collections.sort(scanResults, sc);

        HashMap<String, Integer> res = new HashMap<>();
        int i = 0;
        for (ScanResult s : scanResults) {
            if (i >= WIFI_CONSIDERTION_COUNT)
                break;
            i++;
            res.put(s.BSSID, s.level);
        }
        return res;
    }

    public void trainCell (Button selectedCell) {
        TextView t = this.activity.findViewById(R.id.wifi_train_statusTV);
        // Disable all train buttons
        toggleAllWifiTrainButtons(false);
        scanMode = selectedCell.getId();
        scanNow();
    }

    void showWifiResults () {
        TextView t = this.activity.findViewById(R.id.wifi_train_statusTV);
        // Disable all train buttons
        toggleAllWifiTrainButtons(false);
        scanMode = R.id.wifi_train_showWifiButton;
        scanNow();
    }

    void toggleAllWifiTrainButtons (boolean newStatus) {
        this.activity.findViewById(R.id.wifi_train_selectedCell_1).setEnabled(newStatus);
        this.activity.findViewById(R.id.wifi_train_selectedCell_2).setEnabled(newStatus);
        this.activity.findViewById(R.id.wifi_train_selectedCell_3).setEnabled(newStatus);
        this.activity.findViewById(R.id.wifi_train_selectedCell_4).setEnabled(newStatus);
        this.activity.findViewById(R.id.wifi_train_selectedCell_5).setEnabled(newStatus);
        this.activity.findViewById(R.id.wifi_train_selectedCell_6).setEnabled(newStatus);
        this.activity.findViewById(R.id.wifi_train_selectedCell_7).setEnabled(newStatus);
        this.activity.findViewById(R.id.wifi_train_selectedCell_8).setEnabled(newStatus);
        this.activity.findViewById(R.id.wifi_train_selectedCell_9).setEnabled(newStatus);
        this.activity.findViewById(R.id.wifi_train_selectedCell_10).setEnabled(newStatus);
        this.activity.findViewById(R.id.wifi_train_selectedCell_11).setEnabled(newStatus);
        this.activity.findViewById(R.id.wifi_train_selectedCell_12).setEnabled(newStatus);
        this.activity.findViewById(R.id.wifi_train_selectedCell_13).setEnabled(newStatus);
        this.activity.findViewById(R.id.wifi_train_selectedCell_14).setEnabled(newStatus);
        this.activity.findViewById(R.id.wifi_train_selectedCell_15).setEnabled(newStatus);
        this.activity.findViewById(R.id.wifi_train_selectedCell_16).setEnabled(newStatus);
        this.activity.findViewById(R.id.wifi_train_showWifiButton).setEnabled(newStatus);
    }

    public String getJSON() {
        Gson gson = new Gson();
        String json = gson.toJson(trainBayesianInstance.bayesianDB);
        return json;
    }

    public void locateMe() {
        TextView t = this.activity.findViewById(R.id.wifi_localisation_result_TV);
        scanMode = R.id.wifi_test_locateMeButton;
        scanNow();
    }

    // isTrainInstance is true, if the data is loaded to the trainInstance.
    // Else, the data is loaded to testInstance
    public void parseConfigFileToDB (String inp, boolean isTrainInstance) {
        if (isTrainInstance)
            trainBayesianInstance = new Bayesian_Localisation();
        else
            testBayesianInstance = new Bayesian_Localisation();

        try {
            JSONObject json = new JSONObject(inp);
            for (Iterator<String> it = json.keys(); it.hasNext(); ) {
                String wifi = it.next();
                for (Iterator<String> it1 = json.getJSONObject(wifi).getJSONObject("cellHash").keys(); it1.hasNext(); ) {
                    String strCellID = it1.next();
                    int cellID = Integer.parseInt(strCellID);
                    Bayesian_Localisation.RSSI r = new Bayesian_Localisation.RSSI(json.getJSONObject(wifi).getJSONObject("cellHash").getJSONObject(strCellID).getJSONObject("rssiHash"));
                    Bayesian_Localisation.Cells cell = new Bayesian_Localisation.Cells(cellID, r);
                    if (isTrainInstance) {
                        if (!trainBayesianInstance.bayesianDB.containsKey(wifi))
                            trainBayesianInstance.bayesianDB.put(wifi, cell);
                        else
                            trainBayesianInstance.bayesianDB.get(wifi).cellHash.put(cellID, cell.cellHash.get(cellID));;

                    }

                    else
                        if (!testBayesianInstance.bayesianDB.containsKey(wifi))
                            testBayesianInstance.bayesianDB.put(wifi, cell);
                        else
                            testBayesianInstance.bayesianDB.get(wifi).cellHash.put(cellID, cell.cellHash.get(cellID));
                }
            }
            if (isTrainInstance == false)
                testBayesianInstance.normaliseDB();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void clearAllTestButtons () {
        Button b = (Button) ((MainActivity) context).findViewById(R.id.wifi_locationCell_1);
        b.getBackground().clearColorFilter();
        b = (Button) ((MainActivity) context).findViewById(R.id.wifi_locationCell_2);
        b.getBackground().clearColorFilter();
        b = (Button) ((MainActivity) context).findViewById(R.id.wifi_locationCell_3);
        b.getBackground().clearColorFilter();
        b = (Button) ((MainActivity) context).findViewById(R.id.wifi_locationCell_4);
        b.getBackground().clearColorFilter();
        b = (Button) ((MainActivity) context).findViewById(R.id.wifi_locationCell_5);
        b.getBackground().clearColorFilter();
        b = (Button) ((MainActivity) context).findViewById(R.id.wifi_locationCell_6);
        b.getBackground().clearColorFilter();
        b = (Button) ((MainActivity) context).findViewById(R.id.wifi_locationCell_7);
        b.getBackground().clearColorFilter();
        b = (Button) ((MainActivity) context).findViewById(R.id.wifi_locationCell_8);
        b.getBackground().clearColorFilter();
        b = (Button) ((MainActivity) context).findViewById(R.id.wifi_locationCell_8);
        b.getBackground().clearColorFilter();
        b = (Button) ((MainActivity) context).findViewById(R.id.wifi_locationCell_10);
        b.getBackground().clearColorFilter();
        b = (Button) ((MainActivity) context).findViewById(R.id.wifi_locationCell_11);
        b.getBackground().clearColorFilter();
        b = (Button) ((MainActivity) context).findViewById(R.id.wifi_locationCell_12);
        b.getBackground().clearColorFilter();
        b = (Button) ((MainActivity) context).findViewById(R.id.wifi_locationCell_13);
        b.getBackground().clearColorFilter();
        b = (Button) ((MainActivity) context).findViewById(R.id.wifi_locationCell_14);
        b.getBackground().clearColorFilter();
        b = (Button) ((MainActivity) context).findViewById(R.id.wifi_locationCell_15);
        b.getBackground().clearColorFilter();
        b = (Button) ((MainActivity) context).findViewById(R.id.wifi_locationCell_16);
        b.getBackground().clearColorFilter();
    }

    private void showLocationResult (int cellID) {
        TextView t = this.activity.findViewById(R.id.wifi_localisation_result_TV);
        Button res;
        int buttonID = 0;
        switch (cellID) {
            case R.id.wifi_train_selectedCell_1:
                t.setText("Cell 1");
                buttonID = R.id.wifi_locationCell_1;
                break;

            case R.id.wifi_train_selectedCell_2:
                t.setText("Cell 2");
                buttonID = R.id.wifi_locationCell_2;
                break;

            case R.id.wifi_train_selectedCell_3:
                t.setText("Cell 3");
                buttonID = R.id.wifi_locationCell_3;
                break;

            case R.id.wifi_train_selectedCell_4:
                t.setText("Cell 4");
                buttonID = R.id.wifi_locationCell_4;
                break;

            case R.id.wifi_train_selectedCell_5:
                t.setText("Cell 5");
                buttonID = R.id.wifi_locationCell_5;
                break;

            case R.id.wifi_train_selectedCell_6:
                t.setText("Cell 6");
                buttonID = R.id.wifi_locationCell_6;
                break;

            case R.id.wifi_train_selectedCell_7:
                t.setText("Cell 7");
                buttonID = R.id.wifi_locationCell_7;
                break;

            case R.id.wifi_train_selectedCell_8:
                t.setText("Cell 8");
                buttonID = R.id.wifi_locationCell_8;
                break;

            case R.id.wifi_train_selectedCell_9:
                t.setText("Cell 9");
                buttonID = R.id.wifi_locationCell_9;
                break;

            case R.id.wifi_train_selectedCell_10:
                t.setText("Cell 10");
                buttonID = R.id.wifi_locationCell_10;
                break;

            case R.id.wifi_train_selectedCell_11:
                t.setText("Cell 11");
                buttonID = R.id.wifi_locationCell_1;
                break;

            case R.id.wifi_train_selectedCell_12:
                t.setText("Cell 12");
                buttonID = R.id.wifi_locationCell_12;
                break;

            case R.id.wifi_train_selectedCell_13:
                t.setText("Cell 13");
                buttonID = R.id.wifi_locationCell_13;
                break;

            case R.id.wifi_train_selectedCell_14:
                t.setText("Cell 14");
                buttonID = R.id.wifi_locationCell_14;
                break;

            case R.id.wifi_train_selectedCell_15:
                t.setText("Cell 15");
                buttonID = R.id.wifi_locationCell_15;
                break;

            case R.id.wifi_train_selectedCell_16:
                t.setText("Cell 16");
                buttonID = R.id.wifi_locationCell_16;
                break;
                default:
                    t.setText("No match found");
                    res = (Button) ((MainActivity) context).findViewById(R.id.wifi_test_locateMeButton);
                    res.setEnabled(true);
                    return;

        }
        res = (Button) ((MainActivity) context).findViewById(buttonID);
        res.setBackgroundColor(Color.rgb(0, 121, 107));
        res = (Button) ((MainActivity) context).findViewById(R.id.wifi_test_locateMeButton);
        res.setEnabled(true);
    }

    public int getDBSize() {
        return this.trainBayesianInstance.bayesianDB.size();
    }

    public boolean isDataAvailable () {
        if (this.getDBSize() > 0)
            return true;
        else
            return false;
    }

    private String tempFunc () {
        String res = "{\"f8:23:b2:3c:83:8\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":0.6666667,\"21\":0.33333334,\"22\":0.0,\"23\":0.0,\"24\":0.0}}}},\"bc:54:f9:fc:9:e8\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":0.0,\"21\":0.0,\"22\":1.0,\"23\":0.0,\"24\":0.0}}}},\"c4:07:2f:48:78:c\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":0.0,\"21\":0.0,\"22\":0.0,\"23\":1.0,\"24\":0.0}}}},\"9c:97:26:8b:15:9\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":0.0,\"21\":1.0,\"22\":0.0,\"23\":0.0,\"24\":0.0}}}},\"18:35:d1:27:d0:1\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":0.0,\"21\":1.0,\"22\":0.0,\"23\":0.0,\"24\":0.0}}}},\"5a:d3:43:fe:b0:8\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.5,\"19\":0.5,\"20\":0.0,\"21\":0.0,\"22\":0.0,\"23\":0.0,\"24\":0.0}}}},\"00:14:5c:98:9:0e\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.33333334,\"19\":0.33333334,\"20\":0.33333334,\"21\":0.0,\"22\":0.0,\"23\":0.0,\"24\":0.0}}}},\"48:d3:43:fe:b0:9\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":1.0,\"20\":0.0,\"21\":0.0,\"22\":0.0,\"23\":0.0,\"24\":0.0}}}},\"00:1f:33:3f:ca:6\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":0.0,\"21\":0.0,\"22\":0.5,\"23\":0.5,\"24\":0.0}}}},\"c4:07:2f:48:78:b\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.33333334,\"14\":0.6666667,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":0.0,\"21\":0.0,\"22\":0.0,\"23\":0.0,\"24\":0.0}}}},\"d4:6e:0e:b0:2952\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":0.0,\"21\":0.5,\"22\":0.5,\"23\":0.0,\"24\":0.0}}}},\"30:d3:2d:5a:cd57\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":1.0,\"21\":0.0,\"22\":0.0,\"23\":0.0,\"24\":0.0}}}},\"b0:4e:26:24:da:4\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":0.0,\"21\":0.6666667,\"22\":0.33333334,\"23\":0.0,\"24\":0.0}}}},\"7c:8b:ca:20:4876\":{\"cellHash\":{\"2131165354\":{\"rssiHash\":{\"0\":0.0,\"1\":0.0,\"2\":0.0,\"3\":0.0,\"4\":0.0,\"5\":0.0,\"6\":0.0,\"7\":0.0,\"8\":0.0,\"9\":0.0,\"10\":0.0,\"11\":0.0,\"12\":0.0,\"13\":0.0,\"14\":0.0,\"15\":0.0,\"16\":0.0,\"17\":0.0,\"18\":0.0,\"19\":0.0,\"20\":0.33333334,\"21\":0.6666667,\"22\":0.0,\"23\":0.0,\"24\":0.0}}}}}";
        return res;
    }

    public void clearAllDB () {
        trainBayesianInstance = new Bayesian_Localisation();
        testBayesianInstance = new Bayesian_Localisation();
    }
}
