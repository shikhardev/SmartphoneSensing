package com.example.tudwifi;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {
    private LocalizationFunctionalities lf = new LocalizationFunctionalities();
    private  ActivityFunctionalities af = new ActivityFunctionalities();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LinearLayout l = (LinearLayout) findViewById(R.id.testLayout);
        l.setVisibility(LinearLayout.GONE);
        l = (LinearLayout) findViewById(R.id.trainLayout);
        l.setVisibility(LinearLayout.GONE);


        if (!lf.isTestReady()) {
            Button b = (Button) findViewById(R.id.backButton);
            b = (Button) findViewById(R.id.testButton);
            b.setEnabled(false);
        }
    }



    /*
    * Interfaces the train phase for localization
    * */
    public void trainButtonClick(View view) {
        LinearLayout l = (LinearLayout) findViewById(R.id.homeLayout);
        l.setVisibility(LinearLayout.GONE);

        l = (LinearLayout) findViewById(R.id.trainLayout);
        l.setVisibility(LinearLayout.VISIBLE);

        l = (LinearLayout) findViewById(R.id.testLayout);
        l.setVisibility(LinearLayout.GONE);
    }


    /*
    * Interfaces the testing phase for test cycle for localization
    * */
    public void testButtonClick(View view) {
        LinearLayout l = (LinearLayout) findViewById(R.id.homeLayout);
        l.setVisibility(LinearLayout.GONE);

        l = (LinearLayout) findViewById(R.id.trainLayout);
        l.setVisibility(LinearLayout.GONE);

        l = (LinearLayout) findViewById(R.id.testLayout);
        l.setVisibility(LinearLayout.VISIBLE);
    }


    /*
    * Takes control to the home screen (Test / Train + <Activities>
    * */
    public void takeMeHome(View view) {
        LinearLayout l = (LinearLayout) findViewById(R.id.homeLayout);
        l.setVisibility(LinearLayout.VISIBLE);

        l = (LinearLayout) findViewById(R.id.trainLayout);
        l.setVisibility(LinearLayout.GONE);

        l = (LinearLayout) findViewById(R.id.testLayout);
        l.setVisibility(LinearLayout.GONE);

        if (!lf.isTestReady()) {
            Button b = (Button) findViewById(R.id.backButton);
            b = (Button) findViewById(R.id.testButton);
            b.setEnabled(false);
        }

    }
}
