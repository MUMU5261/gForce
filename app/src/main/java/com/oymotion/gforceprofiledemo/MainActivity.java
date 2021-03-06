package com.oymotion.gforceprofiledemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.oymotion.gforceprofile.GForceProfile;
import com.oymotion.gforceprofile.ScanCallback;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.scan_toggle_btn)
    Button scanToggleButton;
    @BindView(R.id.scan_results)
    RecyclerView recyclerView;
    private ScanResultsAdapter resultsAdapter;

    private final static String TAG = MainActivity.class.getSimpleName();
    private BluetoothAdapter bluetoothAdapter;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private GForceProfile gForceProfile;
    private final int ACCESS_LOCATION = 1;
    private List<BluetoothDevice> bluetoothDevices = new ArrayList<BluetoothDevice>();
    private boolean isScanning = false;
    private GforceDatabaseHelper dbHelper;

    @SuppressLint("WrongConstant")
    private void getPermission() {
        int permissionCheck = 0;
        permissionCheck = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        permissionCheck += this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissionCheck += this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            //???????????????
            this.requestPermissions( // ????????????
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    ACCESS_LOCATION);// ???????????????,????????????
        }
    }

    private boolean hasAllPermissionGranted(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        configureResultList();
        getPermission();

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) // ??? without {},if need to add to include following two sentences, or compile following single line code.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_CONTACTS); //?????

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "ble_not_supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "error_bluetooth_not_supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        gForceProfile = new GForceProfile(this);

        dbHelper = new GforceDatabaseHelper(this, "Quaternion.db", null, 1);
        dbHelper.getWritableDatabase();

    }

    @OnClick(R.id.scan_toggle_btn)
    public void onScanToggleClick() {

        if (isScanning) {
            gForceProfile.stopScan();
            isScanning = false;
        } else {
            scanBleDevices();
        }

        updateButtonUIState();
    }

    private void scanBleDevices() {
        resultsAdapter.clearScanResults();

        gForceProfile.startScan(new ScanCallback() {
            @Override
            public void onScanResult(BluetoothDevice bluetoothDevice, int rssi) {
                runOnUiThread(() -> {
                    //Log.d(TAG, "Device discovered: " + bluetoothDevice.toString() + ", Rssi:" + rssi);

                    if (bluetoothDevice != null && bluetoothDevice.getName() != null &&
                            bluetoothDevice.getName().contains("gForce")) {
                        Log.i(TAG, "Device discovered: " + bluetoothDevice.toString() + ", Rssi:" + rssi);

                        bluetoothDevices.add(bluetoothDevice);

                        resultsAdapter.addScanResult(new ScanResult(bluetoothDevice, rssi));
                    }
                });
            }

            @Override
            public void onScanFailed(int i) {
                Log.e(TAG, "Error code: " + i);
            }
        });

        isScanning = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case ACCESS_LOCATION:
                if (hasAllPermissionGranted(grantResults)) {
                    Log.i(TAG, "onRequestPermissionsResult: ??????????????????");
                } else {
                    Log.i(TAG, "onRequestPermissionsResult: ????????????????????????");
                }
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (isScanning) {
            /*
             * Stop scanning in onPause callback.
             */
            isScanning = false;
            gForceProfile.stopScan();
        }
    }

    private void configureResultList() {
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);
        LinearLayoutManager recyclerLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(recyclerLayoutManager);
        resultsAdapter = new ScanResultsAdapter();
        recyclerView.setAdapter(resultsAdapter);
        resultsAdapter.setOnAdapterItemClickListener(view -> {
            final int childAdapterPosition = recyclerView.getChildAdapterPosition(view);
            final ScanResult itemAtPosition = resultsAdapter.getItemAtPosition(childAdapterPosition);
            onAdapterItemClick(itemAtPosition);
        });
    }

    // EMG choose and transfer view
    private void onAdapterItemClick(ScanResult scanResults) {
        final Intent intent = new Intent(this, DeviceActivity.class);
        intent.putExtra(DeviceActivity.EXTRA_DEVICE_NAME, scanResults.getBluetoothDevice().getName());
        intent.putExtra(DeviceActivity.EXTRA_MAC_ADDRESS, scanResults.getBluetoothDevice().getAddress());
        startActivity(intent);
    }

    private void updateButtonUIState() {
        scanToggleButton.setText(isScanning ? R.string.stop_scan : R.string.start_scan);
    }
}