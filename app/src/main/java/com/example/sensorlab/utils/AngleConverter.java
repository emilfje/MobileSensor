package com.example.sensorlab.utils;

/**
 * Class for converting radians to degrees
 */
public class AngleConverter {
    private static AngleConverter angleConverter;
    private float filteredValue, comPitch;
    private double F=0.1;

    private AngleConverter(){

    }

    public static AngleConverter getInstance(){
        if(angleConverter == null){
            angleConverter = new AngleConverter();
        }
        return angleConverter;
    }


    /**
     * Calculates the angle based of the accelerator values
     * @param tx is the x value from the accelerator
     * @param ty is the y value from the accelerator
     * @param tz is the z value from the accelerator
     */
    public void calculateAngle(float tx, float ty, float tz){
        float accPitch = (float) (Math.atan(tz/(Math.sqrt(Math.pow(tx,2) + Math.pow(ty,2)))) * (180/Math.PI));
        filteredValue = (float) (F * filteredValue + (1-F)*accPitch);
    }

    /**
     * Calculates the angle of accelerometer with gyroscope data
     * @param tx is the x value from the accelerator
     * @param ty is the y value from the accelerator
     * @param tz is the z value from the accelerator
     * @param ry is the y value from the gyro
     */
    public void calculateFusedAngle(float tx, float ty, float tz, float ry){
        float accPitch = (float) (Math.atan(tz/(Math.sqrt(Math.pow(tx,2) + Math.pow(ty,2)))) * (180/Math.PI));
        float dT = (float) (1/52); //52Hz
        comPitch = (float) (((F * (comPitch + dT * ry)) + (1-F)*accPitch));
    }

    public float getComPitch() {
        return comPitch;
    }

    public float getFilteredValue() {
        return filteredValue;
    }
}
