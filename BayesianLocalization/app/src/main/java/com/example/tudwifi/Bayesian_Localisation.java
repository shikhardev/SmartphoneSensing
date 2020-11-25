package com.example.tudwifi;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Iterator;

public class Bayesian_Localisation {

    /*
     *
     * rssiHash: <Int: Float>: Rssi values and their PMF / Count
     * cellHash: <Int: RSSI>: CellID and their RSSI object
     * Bayesians: <String: Cell>: WifiID and their Cell object
     *
     * */

    // Resolution of DBs to consider
    // Res should be a factor of the range of RSSI
    private static int RSSI_RESOLUTION = 2;
    private static int START_RSSI = -40;       // Assume this represents the closest RSSI
    private static int END_RSSI = -90;         // Farthest RSSI to consider
    public HashMap<String, Cells> bayesianDB;

    public static class RSSI {
        public HashMap <Integer, Float> rssiHash;

        public RSSI() {
            rssiHash = new HashMap<Integer, Float>();
            int start = Math.abs(START_RSSI);
            int end = Math.abs (END_RSSI);


            // Now, 0 represents the closest, and end represents the farthest.
            end -= start;
            start -= start;


            for (int i = start; i < (end / RSSI_RESOLUTION); i ++) {
                rssiHash.put(i, (float) 0);
            }
        }


        public RSSI (int currentRSSI) {
            this();
            incrementRSSICount(currentRSSI);
        }


        /*
         * Below RSSI constructor is used to parse config file into RSSI object
         * */
        public RSSI (JSONObject raw) {
            rssiHash = new HashMap<Integer, Float>();
            Iterator i = raw.keys();
            while (i.hasNext()) {
                try {
                    String strID = (String) i.next();
                    Integer intID = Integer.parseInt(strID);
                    Double val = (Double) raw.get(strID);
                    float floatVal = val.floatValue();
                    rssiHash.put(intID,floatVal);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }


        /*
         * rssiValue will be alligned in the function
         * Input: Raw RSSI reading
         * */
        public void incrementRSSICount (int rssiValue) {
            int allignedRSSI = getAllignedRSSIValue(rssiValue);
            if (this.rssiHash.containsKey(allignedRSSI))
                this.rssiHash.put(allignedRSSI, (this.rssiHash.get(allignedRSSI) + 1));
            else
                return;
        }


        /*
         * Normalising RSSI : min brought to 0 and divided by RSSI_RESOLUTION
         * */

        public int getAllignedRSSIValue (int currentRSSI) {
            currentRSSI = Math.abs(currentRSSI);
            currentRSSI -= Math.abs (START_RSSI);
            currentRSSI /= RSSI_RESOLUTION;
            return currentRSSI;
        }



        /*
        * Returns the sum of counts of each rssi. Used for normalization
        * */
        public float computeSumOfRSSIReadings () {
            float sum = 0;
            for (float v : this.rssiHash.values())
                sum += v;
            return sum;
        }

        public float computeAvgOfRSSIReadings (int start, int end) {
            float sum = 0;
            for (int i = start; i < end; i ++) {
                sum += this.rssiHash.get(i);
            }

            return (sum / (end - start));
        }



        public void convolveRSSI () {
            int reach = 1;
            int count = rssiHash.size();
            float temp = 0;
            HashMap <Integer, Float> res = new HashMap<Integer, Float>();
            int firstKey = getAllignedRSSIValue(START_RSSI);

            for (int i = firstKey; i < count; i ++) {
                int l = ((i - reach) < 0)? 0 : (i - reach);
                int r = ((i + reach) >= count)? (count-1) : (i + reach);
                r += 1; // Since avg count goes to 1 less than r
                temp = computeAvgOfRSSIReadings(l, r);
                if (rssiHash.get(i) == 0)
                    res.put(i, temp);
                else
                    res.put(i, rssiHash.get(i));
            }
            this.rssiHash = res;
        }
    }

    public static class Cells {
        public HashMap<Integer, RSSI> cellHash;

        public Cells () {
            cellHash = new HashMap<Integer, RSSI>();
        }

        /*
         * This constructor is used during training (appending)
         * */
        public Cells (int cellID, int currentRSSI) {
            this();
            RSSI rssi = new RSSI(currentRSSI);
            this.cellHash.put(cellID, rssi );
        }


        /*
         * This constructor is used during retrieval of db
         * */
        public Cells (int cellID, RSSI r) {
            this();
            this.cellHash.put (cellID, r);
        }

        public void incrementRSSICount (int cellID, int rssi) {
            this.cellHash.get(cellID).incrementRSSICount(rssi);
            return;
        }
    }

    public Bayesian_Localisation () {
        bayesianDB = new HashMap<String, Cells>();
    }

    public void appendTrainingData(int cellID, HashMap<String, Integer> wifiReadings) {
        for (String wifiName : wifiReadings.keySet()) {

            if (this.bayesianDB.containsKey(wifiName) == false) {
                this.bayesianDB.put(wifiName, new Cells());
            }
            if (this.bayesianDB.get(wifiName).cellHash.containsKey(cellID) == false) {
                this.bayesianDB.get(wifiName).cellHash.put(cellID, new RSSI());
            }
            int currentRSSI = wifiReadings.get(wifiName);
            this.bayesianDB.get(wifiName).cellHash.get(cellID).incrementRSSICount(currentRSSI);
        }
    }

    public void normaliseDB () {
        for (String wifiName : this.bayesianDB.keySet()) {
            for (int cellID: this.bayesianDB.get(wifiName).cellHash.keySet()) {

                this.bayesianDB.get(wifiName).cellHash.get(cellID).convolveRSSI();

                float sum = this.bayesianDB.get(wifiName).cellHash.get(cellID).computeSumOfRSSIReadings();

                // Normalise
                for (int rssi : this.bayesianDB.get(wifiName).cellHash.get(cellID).rssiHash.keySet()) {
                    this.bayesianDB.get(wifiName).cellHash.get(cellID).rssiHash.put(rssi,
                            this.bayesianDB.get(wifiName).cellHash.get(cellID).rssiHash.get(rssi)
                                    / sum);
                }

            }
        }
    }

    /*
    * Returns id of the most probable cell
    * */
    public int computeLocation (HashMap<String, Integer> liveWifi) {
        HashMap<Integer, Integer> cellTrack = new HashMap<Integer, Integer>();


        for (String currentWifi : liveWifi.keySet()) {
            int currentRSSI = new RSSI().getAllignedRSSIValue(liveWifi.get(currentWifi));

            float maxPMF = (float) -1;
            int maxCellID = 0;


            HashMap<Integer, RSSI> currentCellHash = new HashMap<Integer, RSSI>();

            if (bayesianDB.containsKey(currentWifi))
                currentCellHash = bayesianDB.get(currentWifi).cellHash;
            else
                continue;

            // Find the cell with max probability for current wifi-rssi combo
            for (int cellID : currentCellHash.keySet()) {
                if (currentCellHash.get(cellID).rssiHash.containsKey(currentRSSI)) {
                    float currentPMF = currentCellHash.get(cellID).rssiHash.get(currentRSSI);
                    if (currentPMF > maxPMF) {
                        maxPMF = currentPMF;
                        maxCellID = cellID;
                    }
                }
            }

            // Add current winner to cellTrack to track the number of times this cell won
            if (cellTrack.containsKey(maxCellID)) {
                cellTrack.put(maxCellID, (cellTrack.get(maxCellID) + 1));
            }
            else {
                cellTrack.put(maxCellID, 1);
            }
        }

        return getCellOfMaxBelief(cellTrack);
    }



    public int computeLocationV2 (HashMap<String, Integer> liveWifi) {
        HashMap<Integer, Integer> beliefTrack = new HashMap<Integer, Integer>();

        for (String currentWifi : liveWifi.keySet()) {
            int currentRSSI = new RSSI().getAllignedRSSIValue(liveWifi.get(currentWifi));
            HashMap<Integer, Double> tempTrack = new HashMap<Integer, Double>();
            HashMap<Integer, RSSI> currentCellHash = new HashMap<Integer, RSSI>();

            if (bayesianDB.containsKey(currentWifi))
                currentCellHash = bayesianDB.get(currentWifi).cellHash;
            else
                continue;

            // Find the cell with max probability for current wifi-rssi combo
            for (int cellID : currentCellHash.keySet()) {
                if (currentCellHash.get(cellID).rssiHash.containsKey(currentRSSI)) {
                    float currentPMF = currentCellHash.get(cellID).rssiHash.get(currentRSSI);
                    tempTrack.put(cellID, ((1/16.0) * currentPMF));
                }
            }

            tempTrack = normaliseTrackingHash (tempTrack);

            double maxPMF = 0.0;
            int maxCell = 0;

            for (int key : tempTrack.keySet()) {
                if (tempTrack.get(key) > maxPMF) {
                    maxCell = key;
                    maxPMF = tempTrack.get(key);
                }
            }
            if (maxCell == 0)
                continue;

            if (beliefTrack.containsKey(maxCell))
                beliefTrack.put(maxCell, beliefTrack.get(maxCell) + 1);
            else
                beliefTrack.put(maxCell, 1);
        }

        return getCellOfMaxBelief(beliefTrack);
    }

    public HashMap<Integer, Double> normaliseTrackingHash (HashMap<Integer, Double> inp) {
        HashMap<Integer, Double> res = new HashMap<Integer, Double>();
        double sum = 0.0;
        for (int key : inp.keySet()) {
            sum += inp.get(key);
        }

        if (sum == 0) {
            return inp;
        }

        for (int key : inp.keySet()) {
            res.put (key, (inp.get(key) / sum));
        }
        return res;
    }

    private int getCellOfMaxBelief (HashMap<Integer, Integer> locationCells) {
        int max = 0;
        int resCellID = -1;
        for (Integer id : locationCells.keySet()) {
            if (locationCells.get(id) > max) {
                max = locationCells.get(id);
                resCellID = id;
            }
        }
        return resCellID;
    }


}
