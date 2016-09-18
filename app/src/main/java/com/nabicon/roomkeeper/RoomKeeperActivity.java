package com.nabicon.roomkeeper;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;

import com.nabicon.AuthorizedServiceTask;
import com.nabicon.Beacon;
import com.nabicon.BeaconScanner;
import com.nabicon.Constants;
import com.nabicon.R;
import com.nabicon.Utils;
import com.nabiconproximitybeacon.ProximityBeaconImpl;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class RoomKeeperActivity extends AppCompatActivity implements NewTaskDialogFragment.OnNewTaskButtonListener{
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final String TAG = RoomKeeperActivity.class.getSimpleName();
    private SharedPreferences sharedPreferences;
    private BeaconScanner beaconScanner;
    private BroadcastReceiver broadcastReceiver;
    private BroadcastReceiver progressBroadcastReceiver;
    private Beacon roomBeacon;
    private MainRoomFragment mainRoomFragment;
    private RoomTasksFragment tasksFragment;
    private RoomNotesFragment notesFragment;
    private ProgressBar progressBar;
    private Menu optionsMenu;

    TabsAdapter tabsAdapter;
    ViewPager viewPager;
    Toolbar toolbar;

    ProximityBeaconImpl client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_keeper);
        toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        sharedPreferences = this.getSharedPreferences(Constants.PREFS_NAME, 0);
        // Set the account name from the shared prefs if we ever set it before.
        String accountName = sharedPreferences.getString("accountName", "");
        client = new ProximityBeaconImpl(this, accountName);
        beaconScanner = new BeaconScanner(this, client);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                roomBeacon = intent.getExtras().getParcelable("roomBeacon");
                if (roomBeacon != null) {
                    toolbar.setTitle(roomBeacon.description);
                }
                else {
                    toolbar.setTitle("Please scan again");
                }

            }
        };
        beaconScanner.createScanner();
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setProgress(0);
        progressBar.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
        progressBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                int progress = intent.getIntExtra("progress", 0);
                if (optionsMenu != null) {
                    MenuItem scanOption = optionsMenu.findItem(R.id.action_refresh);
                    if (progress > 0 && progress < 100) {
                        scanOption.setEnabled(false);
                    } else {
                        scanOption.setEnabled(true);
                    }
                }
                progressBar.setProgress(progress);
            }
        };
        mainRoomFragment = MainRoomFragment.newInstance("", "");
        tasksFragment = RoomTasksFragment.newInstance(client);
        notesFragment = RoomNotesFragment.newInstance("", "");
        tabsAdapter = new TabsAdapter(getSupportFragmentManager(), mainRoomFragment, tasksFragment, notesFragment);
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
        registerReceiver(progressBroadcastReceiver, new IntentFilter("scanProgressUpdater"));
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
        unregisterReceiver(progressBroadcastReceiver);
    }

    private void checkManifestPermission(String permission) {
        if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    PERMISSION_REQUEST_FINE_LOCATION);
        }
        else {
            beaconScanner.startScan();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_action_bar, menu);
        optionsMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
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

    @Override
    public void onNewTaskButtonClicked(String task) {
        tasksFragment.onNewTaskButtonClicked(task);
    }
}
