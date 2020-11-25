package com.example.particlefilter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import com.google.gson.Gson;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private final boolean IS_DEBUG = false;
    private boolean isPFMonitoring = false;

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor accel;

    private int particleDirection = 0;
    private float calibratedNorth = 0f;
    private boolean isCalibrating = false;
    private boolean isCalibrationComplete = false;
    private List<Float> calibrationDirectionList = new ArrayList<Float>();

    private int distancePerStep = 1;    // In cm
    private int calibrationAccelStepCount = 0;
    private int stepCount = 0;

    private HashMap<String, Cell> cellHash = new HashMap<>();
    private HashMap<String, Integer> resultCellTrack = new HashMap<>();
    private List<Particle> particles;


    private StepDetector sd = new StepDetector();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        // get the screen dimensions
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        ImageView canvasView = (ImageView) findViewById(R.id.canvas);
        Bitmap blankBitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(blankBitmap);
        canvasView.setImageBitmap(blankBitmap);
        try {
            constructCells (canvas);
            generateParticles (canvas);
        }
        catch (Exception e) {
            Toast.makeText(this, "Cell data unavailable", Toast.LENGTH_LONG);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
//        if (sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
//            return;
//        else if (sensor.getType() == Sensor.TYPE_STEP_COUNTER)
//            return;
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (IS_DEBUG)
            return;

        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotationMatrix = new float[16];
            float[] orientationVals = new float[3];
            float directionConsiderationWindow = 30;

            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientationVals);
            float direction = (float) Math.toDegrees(orientationVals[0]);
            direction = getCompensatedDirection (direction);

            if (isCalibrating)
                calibrationDirectionList.add(direction);

            // If North (+- directionConsiderationWindow around 0), return 0
            if ((direction > (360 - directionConsiderationWindow)) || (direction < directionConsiderationWindow))
                direction = 0;

                // If East (+- directionConsiderationWindow around 90), return 90
            else if ((direction > (90 - directionConsiderationWindow)) && (direction < (90 + directionConsiderationWindow)))
                direction = 90;

                // If South (+- directionConsiderationWindow around 180), return 180
            else if ((direction > (180 - directionConsiderationWindow)) && (direction < (180 + directionConsiderationWindow)))
                direction = 180;

                // If West (+- directionConsiderationWindow around 270), return 270
            else if ((direction > (270 - directionConsiderationWindow)) && (direction < (270 + directionConsiderationWindow)))
                direction = 270;
            else
                direction = 9999;

            if ((isCalibrationComplete) && (direction != particleDirection) && (direction != 9999)) {
                particleDirection = (int) direction;
                TextView tv = (TextView) findViewById(R.id.directionTV);
                String res = "Direction : " + Float.toString(direction);
                tv.setText(res);
            }


        }

        else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            boolean stepDetected = sd.isStepDetected(event.values.clone());

            if ((stepDetected) && (isCalibrating)) {
                TextView t = findViewById(R.id.homeStepTV);
                calibrationAccelStepCount ++;
                t.setText("Calibration Step Count: " + Integer.toString(calibrationAccelStepCount));
            }

            else if ((stepDetected) && (isCalibrationComplete) && (isPFMonitoring)) {
                int pixelsToMove = 1;
                stepCount ++;
                TextView t = (TextView) findViewById(R.id.stepCountTV);
                t.setText("Step Count: " + Integer.toString(stepCount));
                switch (particleDirection) {
                    case 0:
                    case 180:
                        pixelsToMove = (int) ((2200f / (10*4*100)) * distancePerStep);
                        break;
                    case 90:
                    case 270:
                        pixelsToMove = (int) ((1080f / (14.3*100)) * distancePerStep);
                        break;
                }
                try {
                    moveAllParticles(pixelsToMove, getCanvas());
                }
                catch (Exception e) {
                    return;
                }

            }
        }
    }

    private float[] filterAccel (float[] oldData, float[] newData) {
        float[] res = new float[3];
        float alpha = 0.25f;
        for (int i = 0; i < 3; i ++) {
            res[i] = oldData[i] + (alpha * (newData[i] - oldData[i]));
        }
        return res;
    }

    private float getCompensatedDirection (float current) {
        current = current < 0 ? 360 + current : current;
        float res = current - calibratedNorth;
        res = res < 0 ? 360 + res : res;
        return res;
    }

    private void calibrationCompleteAction () {
        int minStepsForCalibration = 5;
        if (calibrationAccelStepCount < minStepsForCalibration) {
            Toast.makeText(this, "Calibration Not Successful. Please Retry.", Toast.LENGTH_LONG);
            isCalibrationComplete = false;
            isCalibrating = false;
            return;
        }


        int ignoreCount = 20;
        float sum = 0;

        // First fixing direction
        // Assumes that the movement is only in one direction
        // Assumes calibrationDirectionList has been recorded, and ignores the first ignoreCount readings
        int directionReadCount = calibrationDirectionList.size();
        for (int i = ignoreCount; i < directionReadCount; i++)
            sum += calibrationDirectionList.get(i);
        calibratedNorth = sum / (directionReadCount - ignoreCount);
        calibrationDirectionList = new ArrayList<>();


        // Calibrating Accel parameters
        // 400 cm in a north pointing cell
        distancePerStep = (int) 800 / calibrationAccelStepCount;
        stepCount = 1;



        isCalibrationComplete = true;
    }

    public void buttonHandler(View view) {
        ConstraintLayout bottomLayer = (ConstraintLayout) findViewById(R.id.bottomLayer);
        ConstraintLayout canvasLayer = (ConstraintLayout) findViewById(R.id.canvasLayer);
        LinearLayout homeLayout = (LinearLayout) findViewById(R.id.homeLayout);
        TextView t;
        Button b;

        switch (view.getId()) {
            case R.id.backButton:
                isPFMonitoring = false;
                bottomLayer.setVisibility(View.GONE);
                canvasLayer.setVisibility(View.GONE);
                homeLayout.setVisibility(View.VISIBLE);
                t = findViewById(R.id.homeStepTV);
                t.setText("");
//                TODO: Destroy all particles
                break;
            case R.id.toPFScreenButton:
                isPFMonitoring = true;
                homeLayout.setVisibility(View.GONE);
                canvasLayer.setVisibility(View.VISIBLE);
                bottomLayer.setVisibility(View.VISIBLE);
                if (IS_DEBUG) {
                    LinearLayout l = findViewById(R.id.defaultBottom);
                    l.setVisibility(View.GONE);
                    l = findViewById(R.id.debugBottom);
                    l.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.startCalibrateButton:
                calibrationAccelStepCount = 0;
                calibrationDirectionList = new ArrayList<>();
                Toast.makeText(this, "Starting Calibration", Toast.LENGTH_SHORT).show();
                isCalibrating = true;
                b = findViewById(R.id.startCalibrateButton);
                b.setEnabled(false);
                t = findViewById(R.id.homeStepTV);
                t.setText("Calibration Step Count: 0");
                break;
            case R.id.endCalibrateButton:
                calibrationCompleteAction();
                Toast.makeText(this, "Ending Calibration", Toast.LENGTH_SHORT).show();
                isCalibrating = false;
                b = findViewById(R.id.startCalibrateButton);
                b.setEnabled(true);
                break;
        }
    }

    private void constructCells (Canvas canvas) throws JSONException {
        InputStream is = this.getResources().openRawResource(R.raw.cell_data);
        Scanner s = new Scanner(is).useDelimiter("\\A");
        String storedString = s.hasNext() ? s.next() : "";
        JSONObject js = new JSONObject(storedString);
        for (Iterator<String> it = js.keys(); it.hasNext(); ) {
            String cellID = it.next();
            Cell t = new Cell (cellID, js.getJSONObject(cellID));
            t.drawCell(canvas);
            cellHash.put(cellID, t);
            resultCellTrack.put(cellID, 0);
        }

    }

    private void generateParticles (Canvas canvas) {
        particles = new ArrayList<>();
        int particlesPerCell = 0;
        List<String> largeCells = Arrays.asList("1", "3", "11", "13", "14", "16");
        for (String cell : cellHash.keySet()) {
            particlesPerCell = largeCells.contains(cell)? 200: 50;
            generateParticlesInCell(cellHash.get(cell),particlesPerCell, canvas);
        }
    }

    private void generateParticlesInCell (Cell c, int particleCount, Canvas canvas) {
        int x1 = c.x;
        int x2 = x1 + c.length;
        int y1 = c.y;
        int y2 = y1 - c.height;
        int tempX = 0;
        int tempY = 0;

        for (int i = 0; i < particleCount; i ++) {
            tempX = getRandomIntInRange(x1,  x2);
            tempY = getRandomIntInRange(y2, y1);
            Particle p = new Particle(tempX, tempY, c);
            p.drawParticle(canvas);
            particles.add(p);
        }
    }

    private int getRandomIntInRange(int min, int max) {
        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    private Particle cloneParticle(Particle inp) {
        int x = inp.x;
        int y = inp.y;
        Cell c = inp.cell;
        float w = inp.weight;
        return new Particle(x, y, c, w);
    }

    private void cloneNewListToParticles (List<Particle> newList) {
        particles = new ArrayList<>();
        for (Particle p : newList) {
            particles.add(cloneParticle(p));
        }
    }

    // Moves all particles in particleDirection
    private void moveAllParticles (int movePixelCount, Canvas canvas) {
        int deletedParticleCount = 0;
        int temp = 0;

        List <Particle> nextGenParticles = new ArrayList<>();

        // Try moving all particles
        // Update weight if the particle exists; else delete the particle from nextGenParticles
        for (Particle p : particles) {
            temp = (int) getRandomFromGaussian((float)movePixelCount, 5.0f);
            p.move (temp, particleDirection);
            p.validateCell(cellHash);
            if (p.isAlive) {
                p.weight += 1.0;
                nextGenParticles.add(cloneParticle(p));
            }
            else {
                deletedParticleCount += 1;
            }
        }

        List <Particle> resampledParticles = resampleParticles(nextGenParticles, deletedParticleCount);
        particles = new ArrayList<>();
        for (Particle r : resampledParticles)
            particles.add(cloneParticle(r));


        for (Particle  p : particles)
            p.drawParticle(canvas);

        // Drawing walls
        for (Cell c : cellHash.values())
            c.drawCell(canvas);
    }



    public List<Particle> resampleParticles (List<Particle> nextGenParticles, int deleteCount) {
        List<Particle> res = new ArrayList<>();
        int generatedCount = 0;


        Gson gson = new Gson();
        String json;
        List<Float> skd = new ArrayList<>();


        if (deleteCount == 0)
            return nextGenParticles;
        else if (nextGenParticles.size() == 0) {
            for (Particle p : particles)
                res.add(new Particle(p.oldX, p.oldY, p.cell));
            return res;
        }

        float totalWeight = 0;
        float maxWeight = 0;
        Particle maxWeightParticle = null;

        for (Particle p : nextGenParticles)
            totalWeight += p.weight;

        // Normalising weights
        for (Particle p : nextGenParticles) {
            p.weight /= totalWeight;
            res.add (cloneParticle(p));
            skd.add(p.weight);
            if (p.weight > maxWeight) {
                maxWeightParticle = p;
                maxWeight = p.weight;
            }
        }
        json = gson.toJson(skd);

        // ReSampling
        for (Particle p : nextGenParticles) {
            // Add the alive particles
            int currentGenerateCount = (int) Math.floor((double) p.weight * deleteCount);
            if ((generatedCount + currentGenerateCount) > deleteCount)
                break;
            generatedCount += currentGenerateCount;
            List<Particle> temp = getNNewParticlesAroundP(p, currentGenerateCount);
            for (Particle t : temp)
                res.add(cloneParticle(t));
        }

        if (generatedCount < deleteCount) {
            List<Particle> temp = getNNewParticlesAroundP(maxWeightParticle, (deleteCount - generatedCount));
            for (Particle t : temp)
                res.add(cloneParticle(t));
        }
         return res;
    }


    public void moveParticles_demo(View view) {
        switch (view.getId()) {
            case R.id.upButton:
                particleDirection = 0;
                break;
            case R.id.downButton:
                particleDirection = 180;
                break;
            case R.id.leftButton:
                particleDirection = 270;
                break;
            case R.id.rightButton:
                particleDirection = 90;
                break;
        }
        moveAllParticles(100, getCanvas());
//        updateCellPrediction();
    }

    private Canvas getCanvas() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        ImageView canvasView = (ImageView) findViewById(R.id.canvas);
        Bitmap blankBitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        Canvas temp = new Canvas(blankBitmap);
        canvasView.setImageBitmap(blankBitmap);
        return temp;
    }

    public void updateCellPrediction () {
        String maxCellID = "";
        int maxParticlesInCell = -99;

        for (Particle p : particles) {
            String currentID = p.cell.cellID;
            resultCellTrack.put(currentID, (resultCellTrack.get(currentID) + 1));
        }

        for (String cellID : resultCellTrack.keySet()) {
            int currentCount = resultCellTrack.get(cellID);
            if (currentCount > maxParticlesInCell) {
                maxParticlesInCell = currentCount;
                maxCellID = cellID;
            }
        }

        TextView t = findViewById(R.id.cellTV);
        t.setText(maxCellID);
    }

    // Source: https://stackoverflow.com/questions/6011943/java-normal-distribution
    public float getRandomFromGaussian (float mean, float sd) {
        Random r = new Random();
        return (float) ((r.nextGaussian()*sd) + mean);
    }

    public void generateNParticlesAroundParticle (Particle p, int count, Canvas canvas) {
        int x = 1;
        int y = 1;
        for (int i = 0; i < count; i++) {
            while (!isCoordinateInCell(x, 0, p.cell))
                x = (int) getRandomFromGaussian((float)p.x, 2);
            while (!isCoordinateInCell(0, y, p.cell))
                y = (int) getRandomFromGaussian((float)p.y, 1);
            Particle newParticle = new Particle(x, y, p.cell);
            newParticle.drawParticle(canvas);
            particles.add(newParticle);
        }
    }


    public List <Particle> getNNewParticlesAroundP (Particle p, int count) {
        int x = 1;
        int y = 1;
        List <Particle> res = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            while (!isCoordinateInCell(x, 0, p.cell))
                x = (int) getRandomFromGaussian((float)p.x, 2);
            while (!isCoordinateInCell(0, y, p.cell))
                y = (int) getRandomFromGaussian((float)p.y, 1);
            Particle newParticle = new Particle(x, y, p.cell);
            res.add(newParticle);
        }
        return res;
    }

    private boolean isCoordinateInCell (int x, int y, Cell c) {
        int x1 = c.x;
        int x2 = x1 + c.length;
        int y1 = c.y;
        int y2 = y1 - c.height;

        if (x == 0) {
            // Check only y
            if ((y > y2) && (y < y1))
                return true;
            else
                return false;
        }
        else if (y == 0) {
            // Check x
            if ((x > x1) && (x < x2))
                return true;
            else
                return false;
        }
        else {
            // Check both
            if (isCoordinateInCell (x, 0, c) && isCoordinateInCell(0, y, c))
                return true;
            else
                return false;
        }
    }




}
