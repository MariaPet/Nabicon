package com.nabicon.roomkeeper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.nabicon.Beacon;
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;


public class MainRoomFragment extends Fragment {

    private static final String TAG = MainRoomFragment.class.getSimpleName();;

    static ProximityBeaconImpl client;
    private BroadcastReceiver broadcastReceiver;
    private Beacon roomBeacon;
    private ArrayList<Task> notifications;
    private NotificationsAdapter adapter;

    public MainRoomFragment() {
        // Required empty public constructor
    }

    public static MainRoomFragment newInstance(ProximityBeaconImpl clnt) {
        MainRoomFragment fragment = new MainRoomFragment();
        client = clnt;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notifications = new ArrayList<>();
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                roomBeacon = intent.getExtras().getParcelable("roomBeacon");
                if (roomBeacon != null) {
                    Log.i(TAG, "Room Changed to: " + roomBeacon.description);
                }
            }
        };

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main_room, container, false);
        adapter = new NotificationsAdapter(getActivity(), R.layout.notification_list_item, notifications);
        adapter.setNotifyOnChange(true);
        ListView notificationsListView = (ListView) rootView.findViewById(R.id.notificationsListView);
        notificationsListView.setAdapter(adapter);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter("fragmentupdater"));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(broadcastReceiver);
    }

    public void addNotification(Task task) {
        if (task.deadline != null) {
            Calendar currentDate = Calendar.getInstance();
            if (currentDate.get(Calendar.YEAR) == task.deadline.get(Calendar.YEAR)
                    && currentDate.get(Calendar.MONTH) == task.deadline.get(Calendar.MONTH)
                    && currentDate.get(Calendar.DAY_OF_MONTH) == task.deadline.get(Calendar.DAY_OF_MONTH)) {
                notifications.add(task);
                adapter.notifyDataSetChanged();
            }
        }
    }

    public void deleteNotification(Task taskToDelete) {
        for (Iterator<Task> iter = notifications.iterator(); iter.hasNext();) {
            Task task = iter.next();
            if (task.attachmentName.equals(taskToDelete.attachmentName)) {
                iter.remove();
            }
        }
        adapter.notifyDataSetChanged();
    }

    public void clearNotifications() {
        notifications.clear();
        adapter.notifyDataSetChanged();
    }
}
