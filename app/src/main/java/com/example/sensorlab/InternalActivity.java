package com.example.sensorlab;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sensorlab.utils.AngleConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * A class for measure Gyroscope and Accelerometer with a mobile phone.
 */
public class InternalActivity extends AppCompatActivity implements SensorEventListener  {


    private float filteredValue, comPitch, tX,tY,tZ;
    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope;
    private TextView EWMA, gyro;
    private ArrayList<String> accelerometerArray, gyroscopeArray;

    private DateTimeFormatter dateFormat;
    private static long lastSaved = 0; // milliseconds
    private static final long MAX_TIME = 10_000;
    private InternalFile internalFile;
    private Switch saveSwitch;
    private boolean isSaveChecked;
    private AngleConverter angleConverter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_internal);

        angleConverter= AngleConverter.getInstance();
        internalFile = InternalFile.getInstance();
        dateFormat = DateTimeFormatter.ofPattern("hh:mm:ss.SSSS");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometerArray = new ArrayList<>();
        gyroscopeArray = new ArrayList<>();
        tX = 0;
        tY = 0;
        tZ = 0;

        filteredValue = 0;
        comPitch = 0;

        EWMA = findViewById(R.id.ewma);
        gyro = findViewById(R.id.gyro);

        saveSwitch = findViewById(R.id.saveSwitch);
        saveSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { //nytt
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    isSaveChecked = isChecked;
                    lastSaved = System.currentTimeMillis();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, gyroscope);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            tX = event.values[0];
            tY = event.values[1];
            tZ = event.values[2];
            angleConverter.calculateAngle(event.values[0], event.values[1], event.values[2]);
            EWMA.setText("EWMA: " + angleConverter.getFilteredValue());
            accelerometerArray.add(LocalDateTime.now().format(dateFormat) + " " + angleConverter.getFilteredValue());
            if (isSaveChecked && (System.currentTimeMillis() - lastSaved < MAX_TIME)) { // 10 sec
                internalFile.saveData(getFilesDir(), accelerometerArray, InternalFile.getInternalAccelerometerfile());
            }
        }else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            angleConverter.calculateFusedAngle(tX, tY, tZ, event.values[1]);
            gyro.setText("Gyro: " + angleConverter.getComPitch());
            gyroscopeArray.add(LocalDateTime.now().format(dateFormat) + " " + angleConverter.getComPitch());
            if (isSaveChecked && (System.currentTimeMillis() - lastSaved < MAX_TIME)) { // 10 sec
                internalFile.saveData(getFilesDir(), gyroscopeArray, InternalFile.getInternalGyroscopefile());
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
