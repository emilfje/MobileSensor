package com.example.sensorlab;


import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;


/**
 * Class for saving data to internal files
 */
public class InternalFile {
    private static InternalFile internalFile;

    private InternalFile(){

    }

    public static InternalFile getInstance(){
        if(internalFile == null){
            internalFile = new InternalFile();
        }
        return internalFile;
    }

    private static final String INTERNAL_ACCELEROMETERFILE = "internalAcceleratorFile";
    private static final String INTERNAL_GYROSCOPEFILE = "internalGyroscopeFile";
    private static final String MOVESENSE_ACCELEROMETERFILE = "movesenseAcceleratorFile";
    private static final String MOVESENSE_GYROSCOPEFILE = "movesenseGyroscopeFile";

    public static String getInternalAccelerometerfile() {
        return INTERNAL_ACCELEROMETERFILE;
    }

    public static String getInternalGyroscopefile() {
        return INTERNAL_GYROSCOPEFILE;
    }

    public static String getMovesenseAccelerometerfile() {
        return MOVESENSE_ACCELEROMETERFILE;
    }

    public static String getMovesenseGyroscopefile() {
        return MOVESENSE_GYROSCOPEFILE;
    }

    /**
     * Save necessary data to local file
     * @param dir local file directory
     */
    public void saveData(File dir, ArrayList<String> Array, String fileName){
        try{
            FileOutputStream fileOutputStream = new FileOutputStream(new File(dir, fileName));
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream, 1024*8);

            for (String s: Array) {
                bufferedOutputStream.write(s.getBytes());
                bufferedOutputStream.write("\n".getBytes());
            }

            fileOutputStream.flush();
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
            fileOutputStream.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Clears the local files to begin a new measurement
     */
    public void clearData(){
        try {
            new FileOutputStream(new File("/data/user/0/com.example.sensorlab/files", INTERNAL_ACCELEROMETERFILE)).close();
            new FileOutputStream(new File("/data/user/0/com.example.sensorlab/files", INTERNAL_GYROSCOPEFILE)).close();
            new FileOutputStream(new File("/data/user/0/com.example.sensorlab/files", MOVESENSE_ACCELEROMETERFILE)).close();
            new FileOutputStream(new File("/data/user/0/com.example.sensorlab/files", MOVESENSE_GYROSCOPEFILE)).close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

}
