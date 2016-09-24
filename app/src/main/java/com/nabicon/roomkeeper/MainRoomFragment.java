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


public class MainRoomFragment extends Fragment {

    private static final String TAG = MainRoomFragment.class.getSimpleName();;

    static ProximityBeaconImpl client;
    private BroadcastReceiver broadcastReceiver;
    private Beacon roomBeacon;
    private ArrayList<String> notifications;
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
                    listNotifications();
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

    private void listNotifications() {
        Callback listNotificationsCallback = new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.e(TAG, "Failed request: " + request, e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                String body = response.body().string();
                notifications.clear();
                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(body);
                        JSONArray attachments = json.getJSONArray("attachments");
                        for (int i = 0; i < attachments.length(); i++) {
                            JSONObject attachment = attachments.getJSONObject(i);
                            String[] namespacedType = attachment.getString("namespacedType").split("/");
                            String type = namespacedType[1];
                            if (type.equals(Constants.TASK_ATTACHMENT_KEY)) {
//                                String attachmentName = attachment.getString("attachmentName");
                                String dataStr = attachment.getString("data");
                                String base64Decoded = new String(Utils.base64Decode(dataStr));
                                JSONObject dataJson = new JSONObject(base64Decoded);
                                String deadline =  dataJson.getString("deadlineDate");
                                DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
                                Calendar deadlineDate = Calendar.getInstance();
                                deadlineDate.setTime(df.parse(deadline));
                                Calendar currentDate = Calendar.getInstance();
                                if (currentDate.get(Calendar.YEAR) == deadlineDate.get(Calendar.YEAR)
                                        && currentDate.get(Calendar.MONTH) == deadlineDate.get(Calendar.MONTH)
                                        && currentDate.get(Calendar.DAY_OF_MONTH) == deadlineDate.get(Calendar.DAY_OF_MONTH)) {
                                    String taskName = dataJson.getString("taskName");
                                    Log.i(TAG, "gia simera: "+taskName);
                                    notifications.add(taskName);
                                    adapter.notifyDataSetChanged();
                                }
                                else {
                                    Log.e(TAG, "mia treli apotyxia");
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                }
            }
        };
        client.listAttachments(listNotificationsCallback, roomBeacon.getBeaconName());
    }
}
