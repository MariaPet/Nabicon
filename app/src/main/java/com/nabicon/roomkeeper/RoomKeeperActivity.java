package com.nabicon.roomkeeper;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.nabicon.AuthorizedServiceTask;
import com.nabicon.Beacon;
import com.nabicon.BeaconScanner;
import com.nabicon.Constants;
import com.nabicon.R;
import com.nabicon.Utils;
import com.nabiconproximitybeacon.ProximityBeacon;
import com.nabiconproximitybeacon.ProximityBeaconImpl;
import com.squareup.okhttp.Callback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class RoomKeeperActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final String TAG = RoomKeeperActivity.class.getSimpleName();
    private SharedPreferences sharedPreferences;
    private BeaconScanner beaconScanner;
    private BroadcastReceiver broadcastReceiver;
//    private ArrayList<Beacon> scannedBeaconsList;
//    private ScanCallback scanCallback;

    TabsAdapter tabsAdapter;
    ViewPager viewPager;
    Toolbar toolbar;
//    ProximityBeacon client;

//    public Beacon roomBeacon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_keeper);
        toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        sharedPreferences = this.getSharedPreferences(Constants.PREFS_NAME, 0);
        // Set the account name from the shared prefs if we ever set it before.
        String accountName = sharedPreferences.getString("accountName", "");
        beaconScanner = new BeaconScanner(this, accountName);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String description = intent.getStringExtra("roomBeacon");
                toolbar.setTitle(description);
            }
        };
//        client = new ProximityBeaconImpl(this, accountName);
//        scannedBeaconsList = new ArrayList<>();
//        scanCallback = new ScanCallback() {
//            @Override
//            public void onScanResult(int callbackType, ScanResult result) {
//                super.onScanResult(callbackType, result);
//                ScanRecord scanRecord = result.getScanRecord();
//                if (scanRecord == null) {
//                    Log.w(TAG, "Null ScanRecord for device " + result.getDevice().getAddress());
//                    return;
//                }
//                byte[] serviceData = scanRecord.getServiceData(Constants.EDDYSTONE_SERVICE_UUID);
//                if (serviceData == null) {
//                    Log.w(TAG, "service data null");
//                    return;
//                }
//                // We're only interested in the UID frame time since we need the beacon ID to register.
//                if (serviceData[0] != Constants.EDDYSTONE_UID_FRAME_TYPE) {
//                    Log.w(TAG, "not eddystone uuid frame type");
//                    return;
//                }
//                // Extract the beacon ID from the service data. Offset 0 is the frame type, 1 is the
//                // Tx power, and the next 16 are the ID.
//                // See https://github.com/google/eddystone/eddystone-uid for more information.
//                byte[] id = Arrays.copyOfRange(serviceData, 2, 18);
//                if (arrayListContainsId(scannedBeaconsList, id)) {
//                    return;
//                }
//                Log.i(TAG, "!!!!!!id " + Utils.toHexString(id) + ", rssi " + result.getRssi());
//
//                Beacon beacon = new Beacon("EDDYSTONE", id, Beacon.STATUS_UNSPECIFIED, result.getRssi());
//                insertIntoListAndFetchStatus(beacon);
//            }
//            @Override
//            public void onScanFailed(int errorCode) {
//                Log.e(TAG, "onScanFaile errorCode" + errorCode);
//            }
//        };
        beaconScanner.createScanner();

        tabsAdapter = new TabsAdapter(getSupportFragmentManager(), RoomKeeperActivity.this);
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(tabsAdapter);
        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter("fragmentupdater"));
        // There could be multiple instances when we need to handle a UserRecoverableAuthException
        // from GMS. Run this check every time another activity has finished running.
        String accountName = getSharedPreferences(Constants.PREFS_NAME, 0)
                .getString("accountName", "");
        if (!accountName.equals("")) {
            new AuthorizedServiceTask(this, accountName).execute();
        }
        checkManifestPermission(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    private void checkManifestPermission(String permission) {
        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    PERMISSION_REQUEST_FINE_LOCATION);
        }
        else {
//            scannedBeaconsList.clear();
            beaconScanner.startScan();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_action_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
//            scannedBeaconsList.clear();
            checkManifestPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //TODO remove startScan from here
                    beaconScanner.startScan();
//                    BeaconScanner.startScan(scanCallback);
                } else {
                    //TODO
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_CODE_ENABLE_BLE:
                if (resultCode == Activity.RESULT_OK) {
                    beaconScanner.createScanner();
                }
                if (resultCode == Activity.RESULT_CANCELED) {
                    Log.w(TAG, "Please enable Bluetooth");
                }
        }
    }

//    private boolean arrayListContainsId(ArrayList<Beacon> list, byte[] id) {
//        for (Beacon beacon: list) {
//            if (Arrays.equals(beacon.id, id)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    //TODO Why does it have to be final?
//    private void insertIntoListAndFetchStatus(final Beacon beacon) {
//        scannedBeaconsList.add(beacon);
//        Collections.sort(scannedBeaconsList, Constants.RSSI_COMPARATOR);
//        Callback getBeaconCallback = new Callback() {
//            @Override
//            public void onFailure(com.squareup.okhttp.Request request, IOException e) {
//                Log.e(TAG, String.format("Failed request: %s, IOException %s", request, e));
//            }
//            @Override
//            public void onResponse(com.squareup.okhttp.Response response) throws IOException {
//                Beacon fetchedBeacon;
//                //TODO what do these codes mean?
//                switch (response.code()) {
//                    case 200:
//                        try {
//                            String body = response.body().string();
//                            fetchedBeacon = new Beacon(new JSONObject(body));
//                        }
//                        catch (JSONException e) {
//                            Log.e(TAG, "JSONException", e);
//                            return;
//                        }
//                        break;
//                    case 403:
//                        fetchedBeacon = new Beacon(beacon.type, beacon.id, Beacon.NOT_AUTHORIZED, beacon.rssi);
//                        break;
//                    case 404:
//                        fetchedBeacon = new Beacon(beacon.type, beacon.id, Beacon.UNREGISTERED, beacon.rssi);
//                        break;
//                    default:
//                        Log.e(TAG, "Unhandled beacon service response: " + response);
//                        return;
//                }
//                int pos = scannedBeaconsList.indexOf(beacon);
//                scannedBeaconsList.set(pos, fetchedBeacon);
//                roomBeacon = scannedBeaconsList.get(0);
//                String description = roomBeacon.description;
//                Log.i(TAG, "Room: " + description);
//                toolbar.setTitle(description);
//            }
//        };
//        client.getBeacon(getBeaconCallback, beacon.getBeaconName());
//    }

}
