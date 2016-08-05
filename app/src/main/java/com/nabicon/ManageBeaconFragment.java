package com.nabicon;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.nabiconproximitybeacon.ProximityBeacon;
import com.nabiconproximitybeacon.ProximityBeaconImpl;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


/**
 * A simple {@link Fragment} subclass.
 */
public class ManageBeaconFragment extends Fragment {

    private static final String TAG = ManageBeaconFragment.class.getSimpleName();

    private static final TableLayout.LayoutParams FIXED_WIDTH_COLS_LAYOUT =
            new TableLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.8f);

    private static final TableLayout.LayoutParams BUTTON_COL_LAYOUT =
            new TableLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);

    private Beacon beacon;
    private String namespace;

    private TextView advertisedId_Type;
    private TextView advertisedId_Id;
    private TextView status;
    private TextView placeId;
    private TextView latLng;
    private ImageView mapView;
    private TextView expectedStability;
    private TextView description;
    private Button actionButton;
    private Button decommissionButton;
    private View attachmentsDivider;
    private TextView attachmentsLabel;
    private TableLayout attachmentsTable;

    ProximityBeacon client;

    public ManageBeaconFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstancesState) {
        super.onCreate(savedInstancesState);
        Bundle bundle = getArguments();
        beacon = bundle.getParcelable("beacon");
        String accountName = bundle.getString("accountName");
        client = new ProximityBeaconImpl(getActivity(), accountName);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_manage_beacon, container, false);

        advertisedId_Type = (TextView)rootView.findViewById(R.id.advertisedId_Type);
        advertisedId_Id = (TextView)rootView.findViewById(R.id.advertisedId_Id);
        status = (TextView)rootView.findViewById(R.id.status);
        placeId = (TextView)rootView.findViewById(R.id.placeId);
        placeId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editLatLngAction();
            }
        });
        latLng = (TextView)rootView.findViewById(R.id.latLng);
        latLng.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editLatLngAction();
            }
        });
        mapView = (ImageView)rootView.findViewById(R.id.mapView);
        expectedStability = (TextView)rootView.findViewById(R.id.expectedStability);
        description = (TextView)rootView.findViewById(R.id.description);
        actionButton = (Button)rootView.findViewById(R.id.actionButton);
        decommissionButton = (Button)rootView.findViewById(R.id.decommissionButton);
        attachmentsDivider = rootView.findViewById(R.id.attachmentsDivider);
        attachmentsLabel = (TextView)rootView.findViewById(R.id.attachmentsLabel);
        attachmentsTable = (TableLayout)rootView.findViewById(R.id.attachmentsTableLayout);

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
                        redraw();
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
        return rootView;
    }

    private void editLatLngAction() {
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        if (beacon.getLatLng() != null) {
            builder.setLatLngBounds(new LatLngBounds(beacon.getLatLng(), beacon.getLatLng()));
        }
        try {
            startActivityForResult(builder.build(getActivity()), Constants.REQUEST_CODE_PLACE_PICKER);
        }
        catch (GooglePlayServicesRepairableException e) {
            Log.e(TAG, "GooglePlayServicesRepairableException", e);
        }
        catch (GooglePlayServicesNotAvailableException e) {
            Log.e(TAG, "GooglePlayServicesNotAvailableException", e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CODE_PLACE_PICKER) {
            if (resultCode == Activity.RESULT_OK) {
                Place place = PlacePicker.getPlace(data, getActivity());
                if (place == null) {
                    return;
                }
                // The place picker presents two selection options: "select this location" and
                // "nearby places". Only the nearby places selection returns a placeId we can
                // submit to the service; the location selection will return a hex-like 0xbeef
                // identifier for that position instead, which isn't what we want. Here we check
                // if the entire string is hex and clear the placeId if it is.
                String id = place.getId();
                if (id.startsWith("0x") && id.matches("0x[0-9a-f]+")) {
                    placeId.setText("");
                    beacon.placeId = "";
                } else {
                    placeId.setText(id);
                    beacon.placeId = id;
                }
                LatLng placeLatLng = place.getLatLng();
                latLng.setText(placeLatLng.toString());
                beacon.latitude = placeLatLng.latitude;
                beacon.longitude = placeLatLng.longitude;
                updateBeacon();
            } else {
                Log.e(TAG, "Error loading place picker. Is the Places API enabled? "
                        + "See https://developers.google.com/places/android-api/signup for more detail");
            }
        }
    }

    private void redraw() {
        advertisedId_Type.setText(beacon.type);
        advertisedId_Id.setText(beacon.getHexId());
        status.setText(beacon.status);
        switch (beacon.status) {
            case Beacon.UNREGISTERED:
                enableRegister();
                break;
            case Beacon.STATUS_ACTIVE:
                enableDeactivate();
                break;
            case Beacon.STATUS_INACTIVE:
                enableActivate();
                break;
            case Beacon.STATUS_DECOMMISSIONED:
                break;
        }
        if (beacon.placeId != null) {
            placeId.setText(beacon.placeId);
        }
        else {
            placeId.setText(R.string.click_to_set);
        }
        if (beacon.getLatLng() != null) {
            latLng.setText(
                    String.format("%.6f, %.6f", beacon.getLatLng().latitude, beacon.getLatLng().longitude));
            String url = String.format(
                    "https://maps.googleapis.com/maps/api/staticmap?size=500x200&scale=2&markers=%.6f,%.6f",
                    beacon.getLatLng().latitude, beacon.getLatLng().longitude);
            new FetchStaticMapTask(mapView).execute(url);
        }
        if (beacon.expectedStability != null) {
            expectedStability.setText(beacon.expectedStability);
        }
        else {
            expectedStability.setText(R.string.click_to_set);
        }
        if (beacon.description != null) {
            description.setText(beacon.description);
        }
        else {
            description.setText(R.string.click_to_set);
        }
        if (!beacon.status.equals(Beacon.UNREGISTERED)) {
            listAttachments();
        }
    }

    private void enableRegister() {
        actionButton.setText("Register");
        actionButton.setOnClickListener(createActionButtonOnClickListener(Beacon.UNREGISTERED));
        enableAttachmentsView(false);
    }

    private void enableDeactivate() {
        actionButton.setText("Deactivate");
        actionButton.setOnClickListener(createActionButtonOnClickListener(Beacon.STATUS_INACTIVE));
        enableAttachmentsView(true);
    }

    private void enableActivate() {
        actionButton.setText("Activate");
        actionButton.setOnClickListener(createActionButtonOnClickListener(Beacon.STATUS_ACTIVE));
        enableAttachmentsView(true);
    }

    private View.OnClickListener createActionButtonOnClickListener(final String status) {
        if (status == null) {
            return null;
        }
        //When status equals Decommissioned, unspecified or not authorized return null
        if (!status.equals(Beacon.STATUS_ACTIVE) &&
                !status.equals(Beacon.STATUS_INACTIVE) &&
                !status.equals(Beacon.UNREGISTERED)) {
            return null;
        }
        return new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                actionButton.setEnabled(false);
                Callback onClickCallBack = new Callback() {
                    @Override
                    public void onFailure(Request request, IOException e) {
                        Log.e(TAG, "Failed Request");
                    }

                    @Override
                    public void onResponse(Response response) throws IOException {
                        String body = response.body().string();
                        if (response.isSuccessful()) {
                            try {
                                JSONObject json = new JSONObject(body);
                                if (json.length() > 0) {
                                    // Activate, deactivate and decommission return empty responses. Register returns
                                    // a beacon object.
                                    beacon = new Beacon(json);
                                }
                                updateBeacon();
                            }
                            catch (JSONException e) {
                                Log.e(TAG, "Failed JSON creation from response: " + body + " " + e);
                            }
                        }
                        else {
                            Log.e(TAG, "Unsuccessful Request " + body + " " + response.code());
                        }
                        actionButton.setEnabled(true);
                    }
                };
                switch (status) {
                    case Beacon.STATUS_ACTIVE:
                        client.activateBeacon(onClickCallBack, beacon.getBeaconName());
                        break;
                    case Beacon.STATUS_INACTIVE:
                        client.deactivateBeacon(onClickCallBack, beacon.getBeaconName());
                        break;
                    case Beacon.UNREGISTERED:
                        try {
                            JSONObject activeBeacon = beacon.toJson().put("status", Beacon.STATUS_ACTIVE);
                            client.registerBeacon(onClickCallBack, activeBeacon);
                        }
                        catch (JSONException e) {
                            Log.e(TAG, "Failed to convert beacon to JSON", e);
                            return;
                        }
                        break;
                }
            }
        };
    }

    private void updateBeacon() {
        // If the beacon hasn't been registered or was decommissioned, redraw the view and let the
        // commit happen in the parent action.
        Log.d(TAG, "Inside update beacon");
        if (beacon.status.equals(Beacon.UNREGISTERED)
                || beacon.status.equals(Beacon.STATUS_DECOMMISSIONED)) {
            redraw();
            return;
        }
        Callback updateBeaconCallback = new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.e(TAG, "Failed Request");
            }

            @Override
            public void onResponse(Response response) throws IOException {
                String body = response.body().string();
                if (response.isSuccessful()) {
                    try {
                        beacon = new Beacon(new JSONObject(body));
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed JSON creation from response: " + body);
                    }
                    redraw();

                }
                else {
                    Log.e(TAG, "Unsuccessful updateBeacon request: " + body);
                }
            }
        };
        JSONObject json;
        try {
            json = beacon.toJson();
        } catch (JSONException e) {
            Log.e(TAG, "JSONException in creating update request");
            return;
        }

        client.updateBeacon(updateBeaconCallback, beacon.getBeaconName(), json);
    }

    private void enableAttachmentsView(boolean b) {
        if (b) {
            attachmentsDivider.setVisibility(View.VISIBLE);
            attachmentsLabel.setVisibility(View.VISIBLE);
            attachmentsTable.setVisibility(View.VISIBLE);
        }
        else {
            attachmentsDivider.setVisibility(View.GONE);
            attachmentsLabel.setVisibility(View.GONE);
            attachmentsTable.setVisibility(View.GONE);
        }
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
                    try {
                        JSONObject json = new JSONObject(body);
                        attachmentsTable.removeAllViews();
                        attachmentsTable.addView(makeAttachmentTableHeader());
                        attachmentsTable.addView(makeAttachmentInsertRow());
                        if (json.length() == 0) {  // No attachment data
                            return;
                        }
                    }
                    catch (JSONException e) {
                        Log.e(TAG, "JSONException in fetching attachments", e);
                    }
                }
            }
        };
        client.listAttachments(listAttachmentsCallback, beacon.getBeaconName());
    }

    private LinearLayout makeAttachmentTableHeader() {
        LinearLayout headerRow = new LinearLayout(getActivity());
        headerRow.addView(makeTextView("Namespace"));
        headerRow.addView(makeTextView("Type"));
        headerRow.addView(makeTextView("Data"));

        // Attachment rows will have four elements, so insert a fake one here with the same
        // layout weight as the delete button.
        TextView dummyView = new TextView(getActivity());
        dummyView.setLayoutParams(BUTTON_COL_LAYOUT);
        headerRow.addView(dummyView);

        return headerRow;
    }

    private TextView makeTextView(String text) {
        TextView textView = new TextView(getActivity());
        textView.setText(text);
        textView.setLayoutParams(FIXED_WIDTH_COLS_LAYOUT);
        return textView;
    }

    private LinearLayout makeAttachmentInsertRow() {
        LinearLayout insertRow = new LinearLayout(getActivity());
        final TextView namespaceTextView = makeTextView(namespace);
        final EditText typeEditText = makeEditText();
        final EditText dataEditText = makeEditText();

        insertRow.addView(namespaceTextView);
        insertRow.addView(typeEditText);
        insertRow.addView(dataEditText);

        Button insertButton = new Button(getActivity());
        insertButton.setText("+");
        insertButton.setLayoutParams(BUTTON_COL_LAYOUT);
//        insertButton.setOnClickListener(
//                makeInsertAttachmentOnClickListener(insertButton, namespaceTextView, typeEditText,
//                        dataEditText));

        insertRow.addView(insertButton);
        return insertRow;
    }

    private EditText makeEditText() {
        EditText editText = new EditText(getActivity());
        editText.setLayoutParams(FIXED_WIDTH_COLS_LAYOUT);
        return editText;
    }
}
