package com.nabicon;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Created by mariloo on 28/8/2016.
 */
public class BeaconScanner {

    private static final String TAG = BeaconScanner.class.getSimpleName();
    private static BluetoothLeScanner scanner;
    // Receives the runnable that stops scanning after SCAN_TIME_MILLIS.
    private static final Handler handler = new Handler(Looper.getMainLooper());


    private BeaconScanner() {

    }

    static BluetoothLeScanner createScanner(Fragment fragment) {
        BluetoothManager btManager = (BluetoothManager) fragment.getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManager.getAdapter();
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            fragment.startActivityForResult(enableBtIntent, Constants.REQUEST_CODE_ENABLE_BLE);
        }
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Log.e(TAG, "Can't enable Bluetooth btadapter=" + (btAdapter == null ? "null" : "oxi null") + " btadapter.enabled=" + (!btAdapter.isEnabled() ? "oxi enabled" : "enabled"));
            return null;
        }
        scanner = btAdapter.getBluetoothLeScanner();
        return scanner;
    }

    static void startScan(final ScanCallback scanCallback) {
        scanner.startScan(scanCallback);
        Log.i(TAG, "Start scan");
        Runnable stopScanning = new Runnable() {
            @Override
            public void run() {
                scanner.stopScan(scanCallback);
                Log.i(TAG, "Stop scanning");
            }
        };
        handler.postDelayed(stopScanning, 5000);
    }

}
