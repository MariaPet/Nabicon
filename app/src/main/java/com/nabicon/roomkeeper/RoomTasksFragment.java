package com.nabicon.roomkeeper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

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
import java.util.ArrayList;

public class RoomTasksFragment extends Fragment {

    private static final String TAG = RoomTasksFragment.class.getSimpleName();

    private BroadcastReceiver scanBroadcastReceiver;
    private BroadcastReceiver deleteTaskBroadcastReceiver;
    private Beacon roomBeacon;
    private String namespace;
    private ArrayList<String> tasks;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;


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
        scanBroadcastReceiver = new BroadcastReceiver() {
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
        deleteTaskBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String deletedTask = intent.getStringExtra("deletedTask");
                int position = intent.getIntExtra("taskPosition", 0);
                deleteAttachment(deletedTask, position);
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
        recyclerView = (RecyclerView) fragmentView.findViewById(R.id.tasks_recycler_view);
        mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new TasksArrayAdapter(tasks, getActivity());
        recyclerView.setAdapter(mAdapter);
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(scanBroadcastReceiver, new IntentFilter("fragmentupdater"));
        getActivity().registerReceiver(deleteTaskBroadcastReceiver, new IntentFilter("deleteTask"));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(scanBroadcastReceiver);
        getActivity().unregisterReceiver(deleteTaskBroadcastReceiver);
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
                    mAdapter.notifyDataSetChanged();
                    try {
                        JSONObject json = new JSONObject(body);
                        if (json.length() == 0) {  // No attachment data
                            Log.i(TAG, "Beacon has no attachments");
                            return;
                        }
                        JSONArray attachments = json.getJSONArray("attachments");
                        for (int i = 0; i < attachments.length(); i++) {
                            JSONObject attachment = attachments.getJSONObject(i);
                            String[] namespacedType = attachment.getString("namespacedType").split("/");
                            String type = namespacedType[1];
                            if (type.equals(Constants.TASK_ATTACHMENT_KEY)) {
                                updateTaskList(attachment);
                            }
                        }
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

    public void deleteAttachment(String attachmentName, final int pos) {
        Callback deleteAttachmentCallback = new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.e(TAG, "Failed request: " + request, e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (response.isSuccessful()) {
                    tasks.remove(pos);
                    mAdapter.notifyDataSetChanged();
                    Toast.makeText(getActivity(), "Task deleted successfully", Toast.LENGTH_SHORT).show();
                } else {
                    String body = response.body().string();
                    Log.e(TAG, "Unsuccessful deleteAttachment request: " + body);
                }
            }
        };
        client.deleteAttachment(deleteAttachmentCallback, attachmentName);
    }

    public void onNewTaskButtonClicked(String task) {
        JSONObject body = buildCreateAttachmentJsonBody(namespace, Constants.TASK_ATTACHMENT_KEY, task);
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
                        updateTaskList(json);
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

    private void updateTaskList(JSONObject attachment) throws JSONException{
        String attachmentName = attachment.getString("attachmentName");
        String dataStr = attachment.getString("data");
        String base64Decoded = new String(Utils.base64Decode(dataStr));
        JSONObject dataJson = new JSONObject(base64Decoded);
        String taskName = dataJson.getString("taskName");
        tasks.add(attachmentName + "$" + taskName);
        mAdapter.notifyDataSetChanged();
    }
}
