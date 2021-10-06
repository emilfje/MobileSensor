package com.example.sensorlab;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 *  Main class
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Button movesense, internal_sensors;
    private InternalFile internalFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        internalFile = InternalFile.getInstance();
        internalFile.clearData();

        movesense = findViewById(R.id.MoveSense);
        movesense.setOnClickListener(this);

        internal_sensors = findViewById(R.id.Phone_sensors);
        internal_sensors.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if(v.getId() == movesense.getId()){
            Intent intent = new Intent(this, ScanActivity.class);
            startActivity(intent);
        } else if(v.getId() == internal_sensors.getId()){
            Intent intent = new Intent(this, InternalActivity.class);
            startActivity(intent);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }


}