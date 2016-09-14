package com.nabicon;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.nabiconproximitybeacon.ProximityBeacon;
import com.nabiconproximitybeacon.ProximityBeaconImpl;
import com.squareup.okhttp.Callback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by mariloo on 28/8/2016.
 */
public class BeaconScanner {

    private static final String TAG = BeaconScanner.class.getSimpleName();
    private BluetoothLeScanner scanner;
    // Receives the runnable that stops scanning after SCAN_TIME_MILLIS.
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ScanCallback scanCallback;
    private ArrayList<Beacon> scannedBeaconsList;
    private Beacon roomBeacon;
    //TODO find better way to initialize activity
    private Activity activity;

    ProximityBeacon client;


    public BeaconScanner(Activity activity, ProximityBeacon client) {
        this.activity = activity;
        this.client = client;
        scannedBeaconsList = new ArrayList<>();
    }

    public BluetoothLeScanner createScanner(Fragment fragment) {
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

    public BluetoothLeScanner createScanner() {
        BluetoothManager btManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManager.getAdapter();
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, Constants.REQUEST_CODE_ENABLE_BLE);
        }
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Log.e(TAG, "Can't enable Bluetooth btadapter=" + (btAdapter == null ? "null" : "oxi null") + " btadapter.enabled=" + (!btAdapter.isEnabled() ? "oxi enabled" : "enabled"));
            return null;
        }
        scanner = btAdapter.getBluetoothLeScanner();
        return scanner;
    }

    public void startScan() {
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                ScanRecord scanRecord = result.getScanRecord();
                if (scanRecord == null) {
                    Log.w(TAG, "Null ScanRecord for device " + result.getDevice().getAddress());
                    return;
                }
                byte[] serviceData = scanRecord.getServiceData(Constants.EDDYSTONE_SERVICE_UUID);
                if (serviceData == null) {
                    Log.w(TAG, "service data null");
                    return;
                }
                // We're only interested in the UID frame time since we need the beacon ID to register.
                if (serviceData[0] != Constants.EDDYSTONE_UID_FRAME_TYPE) {
                    Log.w(TAG, "not eddystone uuid frame type");
                    return;
                }
                // Extract the beacon ID from the service data. Offset 0 is the frame type, 1 is the
                // Tx power, and the next 16 are the ID.
                // See https://github.com/google/eddystone/eddystone-uid for more information.
                byte[] id = Arrays.copyOfRange(serviceData, 2, 18);
                if (arrayListContainsId(scannedBeaconsList, id)) {
                    return;
                }
                Log.i(TAG, "!!!!!!id " + Utils.toHexString(id) + ", rssi " + result.getRssi());

                Beacon beacon = new Beacon("EDDYSTONE", id, Beacon.STATUS_UNSPECIFIED, result.getRssi());
                insertIntoListAndFetchStatus(beacon);
            }
            @Override
            public void onScanFailed(int errorCode) {
                Log.e(TAG, "onScanFaile errorCode" + errorCode);
            }
        };
        try {
            scannedBeaconsList.clear();
            scanner.startScan(scanCallback);
            Log.i(TAG, "Start scan");
            Runnable stopScanning = new Runnable() {
                @Override
                public void run() {
                    scanner.stopScan(scanCallback);
                    Log.i(TAG, "Stop scanning");
                }
            };
            handler.postDelayed(stopScanning, 8000);
        }
        catch(Exception e){
            Log.e(TAG, "Scanner is null. " + e.getMessage());
        }
    }

    private boolean arrayListContainsId(ArrayList<Beacon> list, byte[] id) {
        for (Beacon beacon: list) {
            if (Arrays.equals(beacon.id, id)) {
                return true;
            }
        }
        return false;
    }

    //TODO Why does it have to be final?
    private void insertIntoListAndFetchStatus(final Beacon beacon) {
        scannedBeaconsList.add(beacon);
        Collections.sort(scannedBeaconsList, Constants.RSSI_COMPARATOR);
        Callback getBeaconCallback = new Callback() {
            @Override
            public void onFailure(com.squareup.okhttp.Request request, IOException e) {
                Log.e(TAG, String.format("Failed request: %s, IOException %s", request, e));
            }
            @Override
            public void onResponse(com.squareup.okhttp.Response response) throws IOException {
                Beacon fetchedBeacon;
                //TODO what do these codes mean?
                switch (response.code()) {
                    case 200:
                        try {
                            String body = response.body().string();
                            fetchedBeacon = new Beacon(new JSONObject(body));
                        }
                        catch (JSONException e) {
                            Log.e(TAG, "JSONException", e);
                            return;
                        }
                        break;
                    case 403:
                        fetchedBeacon = new Beacon(beacon.type, beacon.id, Beacon.NOT_AUTHORIZED, beacon.rssi);
                        break;
                    case 404:
                        fetchedBeacon = new Beacon(beacon.type, beacon.id, Beacon.UNREGISTERED, beacon.rssi);
                        break;
                    default:
                        Log.e(TAG, "Unhandled beacon service response: " + response);
                        return;
                }
                int pos = scannedBeaconsList.indexOf(beacon);
                scannedBeaconsList.set(pos, fetchedBeacon);
                roomBeacon = scannedBeaconsList.get(0);
                String description = roomBeacon.description;
                Log.i(TAG, "Room: " + description);
                Intent data = new Intent("fragmentupdater");
                data.putExtra("roomBeacon", roomBeacon);
                activity.sendBroadcast(data);
            }
        };
        client.getBeacon(getBeaconCallback, beacon.getBeaconName());
    }

}
