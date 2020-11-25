package com.example.tudwifi;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;


public class ActivityFunctionalities extends SQLiteOpenHelper {
    private int FEATURE_COUNT = 2;
    private final Context context;
    ArrayList<String> stillMaxMinX = new ArrayList<String>();
    ArrayList<String> moveMaxMinX = new ArrayList<String>();
    ArrayList<String> stillMaxMinY = new ArrayList<String>();
    ArrayList<String> moveMaxMinY = new ArrayList<String>();
    ArrayList<String> stillMaxMinZ = new ArrayList<String>();
    ArrayList<String> moveMaxMinZ = new ArrayList<String>();
    boolean trainingComplete = false;


    public ActivityFunctionalities (Context context) {
        super (context, "accelDB", null, 1);
        this.context = context;
    }

    public float[] computeFeatures (float[] input) {
        float max = -90;
        float min = 90;
        float sum = 0, avg = 0;

        float[] res = new float[FEATURE_COUNT];
        for (float i: input) {
            sum += i;
            if (i > max)
                max = i;
            if (i < min)
                min = i;
        }

        avg = sum / input.length;
        res[0] = avg;
        float maxMin = max - min;
        res[1] = maxMin;
        return res;
    }

    public int analyseActivity (float[] x, float[] y, float[] z) {
        float[] processedX = computeFeatures(x);
        float[] processedY = computeFeatures(y);
        float[] processedZ = computeFeatures(z);
        double resMaxmin_Still = 0;
        double resMaxmin_Move = 0;
        double curRes = Math.sqrt((double) (Math.pow(processedX[1], 2) + Math.pow(processedY[1], 2) + Math.pow(processedZ[1], 2)));

        float sumStill = 0;
        float sumMove = 0;
        int count = stillMaxMinX.size();

        // Sum of ABS(Diff(curRes, resMaxminStill)) and ABS(Diff(curRes, resMaxminMove))
        for (int i = 0; i < count; i ++) {
            resMaxmin_Still = Math.pow(Float.parseFloat(stillMaxMinX.get(i)), 2);
            resMaxmin_Still += Math.pow(Float.parseFloat(stillMaxMinY.get(i)), 2);
            resMaxmin_Still += Math.pow(Float.parseFloat(stillMaxMinZ.get(i)), 2);
            resMaxmin_Still = Math.sqrt ((double)resMaxmin_Still );

            resMaxmin_Move = Math.pow(Float.parseFloat(moveMaxMinX.get(i)), 2);
            resMaxmin_Move += Math.pow(Float.parseFloat(moveMaxMinY.get(i)), 2);
            resMaxmin_Move += Math.pow(Float.parseFloat(moveMaxMinZ.get(i)), 2);
            resMaxmin_Move = Math.sqrt ((double)resMaxmin_Move );

            sumStill += Math.abs(curRes - resMaxmin_Still);
            sumMove += Math.abs(curRes - resMaxmin_Move);

        }



        float avgStill = sumStill / count;
        float avgMove = sumMove / count;


        if (avgStill > avgMove)
            return 1;
        else
            return 0;
    }

    // Store each feature in DB
    // Mode = 0 for still, 1 for move
    // For a window of 20 readings, what is the max - min in each direction (x, y or z)
    public void computeFeatureAndStoreInDB (float[] x, float[] y, float[] z, int mode)  {
        float[] processedX = computeFeatures(x);
        float[] processedY = computeFeatures(y);
        float[] processedZ = computeFeatures(z);
        if (mode == 0) {
            stillMaxMinX.add (Float.toString((float)processedX[1]));
            stillMaxMinY.add (Float.toString((float)processedY[1]));
            stillMaxMinZ.add (Float.toString((float)processedZ[1]));
        }
        else if (mode == 1) {
            moveMaxMinX.add (Float.toString((float)processedX[1]));
            moveMaxMinY.add (Float.toString((float)processedY[1]));
            moveMaxMinZ.add (Float.toString((float)processedZ[1]));
        }
        if ((moveMaxMinX.size() > 0) && (stillMaxMinX.size() > 0) && (stillMaxMinX.size() == moveMaxMinX.size()))
            this.trainingComplete = true;
    }

    public boolean isTrainingComplete () {
        return this.trainingComplete;
    }

    public void getAccelTestData () throws JSONException {
        JSONArray j = new JSONArray();
        InputStream is = context.getResources().openRawResource(R.raw.info);
        Scanner s = new Scanner(is).useDelimiter("\\A");
        String storedString = s.hasNext() ? s.next() : "";
        JSONObject storedJS = null;
        JSONArray currentArr = null;
        String currentKey = null;
        storedJS = new JSONObject(storedString);

        currentKey = "stillMaxMinX";
        currentArr = storedJS.getJSONArray(currentKey);
        int sizeOfTrainingSet = currentArr.length();
        if (sizeOfTrainingSet == 0)
            return;
        for (int i = 0; i < sizeOfTrainingSet; i ++) {
            stillMaxMinX.add((String) currentArr.get(i));
        }
        currentKey = "stillMaxMinY";
        currentArr = storedJS.getJSONArray(currentKey);
        for (int i = 0; i < sizeOfTrainingSet; i ++) {
            stillMaxMinY.add((String) currentArr.get(i));
        }
        currentKey = "stillMaxMinZ";
        currentArr = storedJS.getJSONArray(currentKey);
        for (int i = 0; i < sizeOfTrainingSet; i ++) {
            stillMaxMinZ.add((String) currentArr.get(i));
        }
        currentKey = "moveMaxMinX";
        currentArr = storedJS.getJSONArray(currentKey);
        for (int i = 0; i < sizeOfTrainingSet; i ++) {
            moveMaxMinX.add((String) currentArr.get(i));
        }
        currentKey = "moveMaxMinY";
        currentArr = storedJS.getJSONArray(currentKey);
        for (int i = 0; i < sizeOfTrainingSet; i ++) {
            moveMaxMinY.add((String) currentArr.get(i));
        }
        currentKey = "moveMaxMinZ";
        currentArr = storedJS.getJSONArray(currentKey);
        for (int i = 0; i < sizeOfTrainingSet; i ++) {
            moveMaxMinZ.add((String) currentArr.get(i));
        }
        trainingComplete = true;
        return;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db = this.getWritableDatabase();
//        db.execSQL("CREATE TABLE "+tablesTable+" ("+colNum+ " INTEGER PRIMARY KEY , "+
//                colSeats+ " INTEGER)");

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
