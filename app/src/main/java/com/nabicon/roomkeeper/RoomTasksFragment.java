package com.nabicon.roomkeeper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.nabicon.Beacon;
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
import java.util.ArrayList;
import java.util.Arrays;

public class RoomTasksFragment extends Fragment implements AdapterView.OnItemClickListener {

    private static final String TAG = RoomTasksFragment.class.getSimpleName();
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private BroadcastReceiver broadcastReceiver;
    Beacon roomBeacon;
    private String namespace;
    TasksArrayAdapter adapter;
    private ArrayList<String> tasks;


    static ProximityBeaconImpl client;

    public RoomTasksFragment() {
        // Required empty public constructor
    }


    public static RoomTasksFragment newInstance(ProximityBeaconImpl clnt) {
        RoomTasksFragment fragment = new RoomTasksFragment();
        client = clnt;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tasks = new ArrayList<>();
        tasks.add("I shouldn't be in the list");
        adapter = new TasksArrayAdapter(getActivity(), R.layout.beacon_list_item, R.id.task_id, tasks);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                roomBeacon = intent.getExtras().getParcelable("roomBeacon");
                if (roomBeacon == null) {
                    Toast.makeText(getActivity(), "Please scan again", Toast.LENGTH_SHORT).show();
                }
                else {
                    listAttachments();
                }
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragmentView = inflater.inflate(R.layout.fragment_room_tasks, container, false);
        ImageButton newTaskButton = (ImageButton) fragmentView.findViewById(R.id.new_task_button);
        newTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                NewTaskDialogFragment newTaskDialogFragment = new NewTaskDialogFragment();
                newTaskDialogFragment.show(fm, "fragment_new_task_dialog");
            }
        });
        Callback listNamespacesCallback = new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.e(TAG, "Failed request: " + request, e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                String body = response.body().string();
                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(body);
                        JSONArray namespaces = json.getJSONArray("namespaces");
                        // At present there can be only one namespace.
                        String tmp = namespaces.getJSONObject(0).getString("namespaceName");
                        if (tmp.startsWith("namespaces/")) {
                            namespace = tmp.substring("namespaces/".length());
                        } else {
                            namespace = tmp;
                        }
//                        redraw();
                    }
                    catch (JSONException e) {
                        Log.e(TAG, "JSONException", e);
                    }
                }
                else {
                    Log.e(TAG, "Unsuccessful listNamespaces request: " + body);
                }
            }
        };
        client.listNamespaces(listNamespacesCallback);
        ListView listView = (ListView) fragmentView.findViewById(R.id.task_list_view);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        return fragmentView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void listAttachments() {
        Callback listAttachmentsCallback = new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.e(TAG, "Failed request: " + request, e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                String body = response.body().string();
                if (response.isSuccessful()) {
                    tasks.clear();
                    try {
                        JSONObject json = new JSONObject(body);
                        if (json.length() == 0) {  // No attachment data
                            Log.w(TAG, "den vrike attachments");
                        }
                        JSONArray attachments = json.getJSONArray("attachments");
                        for (int i = 0; i < attachments.length(); i++) {
                            JSONObject attachment = attachments.getJSONObject(i);
                            String[] namespacedType = attachment.getString("namespacedType").split("/");
                            String type = namespacedType[1];

                            String dataStr = attachment.getString("data");
                            String base64Decoded = new String(Utils.base64Decode(dataStr));
                            tasks.add(base64Decoded);
                        }
                        adapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        Log.e(TAG, "JSONException in fetching attachments", e);
                    }
                } else {
                    Log.e(TAG, "Unsuccessful listAttachments request: " + body);
                }
            }
        };
        client.listAttachments(listAttachmentsCallback, roomBeacon.getBeaconName());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(getActivity(), "Item: " + position, Toast.LENGTH_SHORT)
                .show();
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

    public void onNewTaskButtonClicked(String task) {
        JSONObject body = buildCreateAttachmentJsonBody(namespace, "task", task);
        Callback createAttachmentCallback = new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.e(TAG, "Failed request: " + request, e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                String body = response.body().string();
                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(body);
                        String dataStr = json.getString("data");
                        String base64Decoded = new String(Utils.base64Decode(dataStr));
                        adapter.add(base64Decoded);
                        adapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        Log.e(TAG, "JSONException in building attachment data", e);
                    }
                } else {
                    Log.e(TAG, "Unsuccessful createAttachment request: " + body);
                }
            }
        };

        client.createAttachment(createAttachmentCallback, roomBeacon.getBeaconName(), body);
    }

    private JSONObject buildCreateAttachmentJsonBody(String namespace, String type, String data) {
        try {
            return new JSONObject().put("namespacedType", namespace + "/" + type)
                    .put("data", Utils.base64Encode(data.getBytes()));
        }
        catch (JSONException e) {
            Log.e(TAG, "JSONException", e);
        }
        return null;
    }
}
