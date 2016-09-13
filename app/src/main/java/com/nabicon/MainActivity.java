package com.nabicon;

import android.app.FragmentManager;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;

import com.nabicon.roomkeeper.RoomKeeperActivity;


public class MainActivity extends AppCompatActivity implements
        GpsPositionFragment.ConfigureBeaconsButtonListener,
        GpsPositionFragment.StartButtonListener {
//        ListBeaconsFragment.ManageBeaconListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private FragmentManager fragmentManager = getFragmentManager();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fragmentManager.beginTransaction().
                add(R.id.container, new GpsPositionFragment()).
                commit();

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int height = displaymetrics.heightPixels;
        int width = displaymetrics.widthPixels;
        Log.i(TAG, "Screen dimension: Width - " + width + " Height - " + height);
    }

    @Override
    public void onConfigureBeaconsClicked() {
//        fragmentManager.beginTransaction().
//                replace(R.id.container, new ListBeaconsFragment()).
//                addToBackStack(GpsPositionFragment.class.getSimpleName()).
//                commit();

    }

    @Override
    public void onStartClicked() {
        Intent i = new Intent(this, RoomKeeperActivity.class);
        startActivity(i);
    }

//    @Override
//    public void onBeaconItemClicked(Bundle bundle) {
//        ManageBeaconFragment fragment = new ManageBeaconFragment();
//        fragment.setArguments(bundle);
//        fragmentManager.beginTransaction().
//                replace(R.id.container, fragment).
//                addToBackStack(ListBeaconsFragment.class.getSimpleName()).
//                commit();
//    }


}
