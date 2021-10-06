package com.example.sensorlab;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.example.sensorlab.uiutils.MsgUtils;
import com.example.sensorlab.utils.AngleConverter;
import com.example.sensorlab.utils.TypeConverter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * A class for measure Gyroscope and Accelerometer with a movesense component
 * Code Inspiration from Anders Lindström's repo:
 * https://gits-15.sys.kth.se/anderslm/Ble-Gatt-Movesense-2.0
 */
public class DeviceActivity extends AppCompatActivity {

    // Movesense 2.0 UUIDs (should be placed in resources file)
    public static final UUID MOVESENSE_2_0_SERVICE =
            UUID.fromString("34802252-7185-4d5d-b431-630e7050e8f0");
    public static final UUID MOVESENSE_2_0_COMMAND_CHARACTERISTIC =
            UUID.fromString("34800001-7185-4d5d-b431-630e7050e8f0");
    public static final UUID MOVESENSE_2_0_DATA_CHARACTERISTIC =
            UUID.fromString("34800002-7185-4d5d-b431-630e7050e8f0");
    // UUID for the client characteristic, which is necessary for notifications
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final String IMU_COMMAND = "Meas/IMU6/26"; // see documentation
    private final byte MOVESENSE_REQUEST = 1, MOVESENSE_RESPONSE = 2, REQUEST_ID = 99;

    private BluetoothDevice mSelectedDevice = null;
    private BluetoothGatt mBluetoothGatt = null;

    private Handler mHandler;

    private TextView mDeviceView;
    private TextView mDataView;
    private TextView mDataView2;

    ArrayList<String> accelerometerArray, gyroscopeArray;
    private DateTimeFormatter dateFormat;
    private static long lastSaved = 0; // milliseconds
    private static final long MAX_TIME = 10_000;
    private InternalFile internalFile;
    private Switch saveSwitch;
    private boolean isSaveChecked;
    private AngleConverter angleConverter;

    private static final String LOG_TAG = "DeviceActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        mDeviceView = findViewById(R.id.device_view);
        mDataView = findViewById(R.id.data_view);
        mDataView2 = findViewById(R.id.data_view2);

        angleConverter = AngleConverter.getInstance();

        accelerometerArray = new ArrayList<>();
        gyroscopeArray = new ArrayList<>();
        dateFormat = DateTimeFormatter.ofPattern("hh:mm:ss.SSSS");
        internalFile = InternalFile.getInstance();

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


        Intent intent = getIntent();
        // Get the selected device from the intent
        mSelectedDevice = intent.getParcelableExtra(ScanActivity.SELECTED_DEVICE);
        if (mSelectedDevice == null) {
            MsgUtils.createDialog("Error", "No device found", this).show();
            mDeviceView.setText(R.string.no_device);
        } else {
            mDeviceView.setText(mSelectedDevice.getName());
        }

        mHandler = new Handler();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mSelectedDevice != null) {
            // Connect and register call backs for bluetooth gatt
            mBluetoothGatt =
                    mSelectedDevice.connectGatt(this, false, mBtGattCallback);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            try {
                mBluetoothGatt.close();
            } catch (Exception e) {
                // ugly, but this is to handle a bug in some versions in the Android BLE API
            }
        }
    }

    /**
     * Callbacks for bluetooth gatt changes/updates
     * The documentation is not always clear, but most callback methods seems to
     * be executed on a worker thread - hence use a Handler when updating the ui.
     */
    private final BluetoothGattCallback mBtGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                mBluetoothGatt = gatt;
                mHandler.post(new Runnable() {
                    public void run() {
                        mDataView.setText(R.string.connected);
                    }
                });
                // Discover services
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Close connection and display info in ui
                mBluetoothGatt = null;
                mHandler.post(new Runnable() {
                    public void run() {
                        mDataView.setText(R.string.disconnected);
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Debug: list discovered services
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {
                    Log.i(LOG_TAG, service.getUuid().toString());
                }

                // Get the Movesense 2.0 IMU service
                BluetoothGattService movesenseService = gatt.getService(MOVESENSE_2_0_SERVICE);
                if (movesenseService != null) {
                    // debug: service present, list characteristics
                    List<BluetoothGattCharacteristic> characteristics =
                            movesenseService.getCharacteristics();
                    for (BluetoothGattCharacteristic chara : characteristics) {
                        Log.i(LOG_TAG, chara.getUuid().toString());
                    }

                    // Write a command, as a byte array, to the command characteristic
                    // Callback: onCharacteristicWrite
                    BluetoothGattCharacteristic commandChar =
                            movesenseService.getCharacteristic(
                                    MOVESENSE_2_0_COMMAND_CHARACTERISTIC);
                    // command example: 1, 99, "/Meas/Acc/13"
                    byte[] command =
                            TypeConverter.stringToAsciiArray(REQUEST_ID, IMU_COMMAND);
                    commandChar.setValue(command);
                    boolean wasSuccess = mBluetoothGatt.writeCharacteristic(commandChar);
                    Log.i("writeCharacteristic", "was success=" + wasSuccess);
                } else {
                    mHandler.post(new Runnable() {
                        public void run() {
                            MsgUtils.createDialog("Alert!",
                                    getString(R.string.service_not_found),
                                    DeviceActivity.this)
                                    .show();
                        }
                    });
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic, int status) {
            Log.i(LOG_TAG, "onCharacteristicWrite " + characteristic.getUuid().toString());

            // Enable notifications on data from the sensor. First: Enable receiving
            // notifications on the client side, i.e. on this Android device.
            BluetoothGattService movesenseService = gatt.getService(MOVESENSE_2_0_SERVICE);
            BluetoothGattCharacteristic dataCharacteristic =
                    movesenseService.getCharacteristic(MOVESENSE_2_0_DATA_CHARACTERISTIC);
            // second arg: true, notification; false, indication
            boolean success = gatt.setCharacteristicNotification(dataCharacteristic, true);
            if (success) {
                Log.i(LOG_TAG, "setCharactNotification success");
                // Second: set enable notification server side (sensor). Why isn't
                // this done by setCharacteristicNotification - a flaw in the API?
                BluetoothGattDescriptor descriptor =
                        dataCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor); // callback: onDescriptorWrite
            } else {
                Log.i(LOG_TAG, "setCharacteristicNotification failed");
            }
        }

        @Override
        public void onDescriptorWrite(final BluetoothGatt gatt, BluetoothGattDescriptor
                descriptor, int status) {
            Log.i(LOG_TAG, "onDescriptorWrite, status " + status);

            if (CLIENT_CHARACTERISTIC_CONFIG.equals(descriptor.getUuid()))
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // if success, we should receive data in onCharacteristicChanged
                    mHandler.post(new Runnable() {
                        public void run() {
                            mDeviceView.setText(R.string.notifications_enabled);
                        }
                    });
                }
        }

        /**
         * Callback called on characteristic changes, e.g. when a sensor data value is changed.
         * This is where we receive notifications on new sensor data.
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic) {
            // debug
            // Log.i(LOG_TAG, "onCharacteristicChanged " + characteristic.getUuid());

            // if response and id matches
            if (MOVESENSE_2_0_DATA_CHARACTERISTIC.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                if (data[0] == MOVESENSE_RESPONSE && data[1] == REQUEST_ID) {
                    // NB! use length of the array to determine the number of values in this
                    // "packet", the number of values in the packet depends on the frequency set(!)
                    int len = data.length;
                    if((len-2)/4 > 0) {
                        // ...
                        //Log.d("TAG", "onCharacteristicChanged: number of values: " + ((data.length - 2) / 4) + " len: " + len);
                        // parse and interpret the data, ...
                        int time = TypeConverter.fourBytesToInt(data, 2);
                        float accX = TypeConverter.fourBytesToFloat(data, 6);
                        float accY = TypeConverter.fourBytesToFloat(data, 10);
                        float accZ = TypeConverter.fourBytesToFloat(data, 14);
                        float gyroX = TypeConverter.fourBytesToFloat(data, 18);
                        float gyroY = TypeConverter.fourBytesToFloat(data, 22);
                        float gyroZ = TypeConverter.fourBytesToFloat(data, 26);

                        // ... and then, filter data, calculate something interesting,
                        // ... display a graph or show values, ...
                        angleConverter.calculateAngle(accX, accY, accZ);
                        if (isSaveChecked && (System.currentTimeMillis() - lastSaved < MAX_TIME)) { // 10 sec
                            accelerometerArray.add(LocalDateTime.now().format(dateFormat) + " " + angleConverter.getFilteredValue());
                            saveAcceleratorData();
                        }
                        angleConverter.calculateFusedAngle(accX, accY, accZ, gyroY);
                        if (isSaveChecked && (System.currentTimeMillis() - lastSaved < MAX_TIME)) { // 10 sec
                            gyroscopeArray.add(LocalDateTime.now().format(dateFormat) + " " + angleConverter.getComPitch());
                            saveCombinedData();
                        }

                        String accStr = "" + accX + " " + accY + " " + accZ;
                        Log.i("acc data", "" + time + " " + accStr);

                        final String viewDataStr = String.format("%.2f, %.2f, %.2f", accX, accY, accZ);
                        mHandler.post(new Runnable() {
                            public void run() {
                                mDataView.setText("EWMA: " + angleConverter.getFilteredValue());
                                mDataView2.setText("Gyro: " + angleConverter.getComPitch());
                            }
                        });
                    }else{ mDataView.setText(R.string.no_data); mDataView2.setText(R.string.no_data);}
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic, int status) {
            Log.i(LOG_TAG, "onCharacteristicRead " + characteristic.getUuid().toString());
        }
    };

    /**
     * Saves the angle based on the accelerator values
     */
    private void saveAcceleratorData(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                internalFile.saveData(getFilesDir(), accelerometerArray, InternalFile.getMovesenseAccelerometerfile());
            }
        }).start();
    }

    /**
     * Saves the angle on accelerometer with gyroscope data
     */
    private void saveCombinedData(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                internalFile.saveData(getFilesDir(), gyroscopeArray, InternalFile.getMovesenseGyroscopefile());
            }
        }).start();
    }


}