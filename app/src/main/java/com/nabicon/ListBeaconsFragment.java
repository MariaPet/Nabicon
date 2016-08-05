package com.nabicon;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import com.nabiconproximitybeacon.ProximityBeacon;
import com.nabiconproximitybeacon.ProximityBeaconImpl;
import com.squareup.okhttp.Callback;

import org.json.JSONException;
import org.json.JSONObject;

import static com.google.android.gms.internal.zzir.runOnUiThread;


public class ListBeaconsFragment extends Fragment {

    private static final String TAG = ListBeaconsFragment.class.getSimpleName();

    public final static int REQUEST_PICK_ACCOUNT = 0;
    public static final int REQUEST_ERROR_RECOVER = 1;
    public static final int REQUEST_CODE_ENABLE_BLE = 2;

    //TODO Read all about it
    public final static String EXTRA_DESTINATION = "com.nabicon.DESTINATION";
    // The Eddystone-UID frame type byte.
    // See https://github.com/google/eddystone for more information.
    private static final byte EDDYSTONE_UID_FRAME_TYPE = 0x00;
    // The Eddystone Service UUID, 0xFEAA.
    private static final ParcelUuid EDDYSTONE_SERVICE_UUID =
            ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");
    //TODO How does the comparator works?
    private static final Comparator<Beacon> RSSI_COMPARATOR = new Comparator<Beacon>() {
        @Override
        public int compare(Beacon lhs, Beacon rhs) {
            return ((Integer) rhs.rssi).compareTo(lhs.rssi);
        }
    };

    private SharedPreferences sharedPreferences;
    private ArrayList<Beacon> scannedBeaconsList;
    private BeaconArrayAdapter arrayAdapter;
    //TODO Read all about it
    private ScanCallback scanCallback;
    private BluetoothLeScanner scanner;
    private Button scanBeaconsButton;
    private TextView accountNameView;
    // Receives the runnable that stops scanning after SCAN_TIME_MILLIS.
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private ManageBeaconListener manageBeaconListener;

    ProximityBeacon client;

    public interface ManageBeaconListener {
        void onBeaconItemClicked(Bundle bundle);
    }

    public ListBeaconsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstances) {
        super.onCreate(savedInstances);
        sharedPreferences = getActivity().getSharedPreferences(Constants.PREFS_NAME, 0);
        scannedBeaconsList = new ArrayList<>();
        arrayAdapter = new BeaconArrayAdapter(getActivity(), R.layout.beacon_list_item, scannedBeaconsList);
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                ScanRecord scanRecord = result.getScanRecord();
                if (scanRecord == null) {
                    Log.w(TAG, "Null ScanRecord for device " + result.getDevice().getAddress());
                    return;
                }
                byte[] serviceData = scanRecord.getServiceData(EDDYSTONE_SERVICE_UUID);
                if (serviceData == null) {
                    Log.w(TAG, "service data null");
                    return;
                }
                // We're only interested in the UID frame time since we need the beacon ID to register.
                if (serviceData[0] != EDDYSTONE_UID_FRAME_TYPE) {
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
        createScanner();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ManageBeaconListener) {
            manageBeaconListener = (ManageBeaconListener) context;
        }
        else {
            throw new RuntimeException(context.toString()
                    + " must implement ConfigureBeaconsListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        manageBeaconListener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list_beacons, container, false);
        scanBeaconsButton = (Button) rootView.findViewById(R.id.buttonScanBeacons);
        scanBeaconsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                arrayAdapter.clear();
                scanner.startScan(scanCallback);
                Log.i(TAG, "Start scan");
                client = new ProximityBeaconImpl(getActivity(), accountNameView.getText().toString());
                Runnable stopScanning = new Runnable() {
                    @Override
                    public void run() {
                        scanner.stopScan(scanCallback);
                        Log.i(TAG, "Stop scanning");
                    }
                };
                handler.postDelayed(stopScanning, 5000);
            }
        });

        accountNameView = (TextView) rootView.findViewById(R.id.accountName);
        accountNameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickUserAccount();
            }
        });

        // Set the account name from the shared prefs if we ever set it before.
        String accountName = sharedPreferences.getString("accountName", "");
        if (!accountName.isEmpty()) {
            accountNameView.setText(accountName);
        }
        else {
            pickUserAccount();
        }
        ListView listView = (ListView) rootView.findViewById(R.id.listView);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Beacon beacon = arrayAdapter.getItem(position);
                if (beacon.status.equals(Beacon.NOT_AUTHORIZED)) {
                    new AlertDialog.Builder(getActivity()).setTitle("Not Authorized")
                            .setMessage("You don't have permission to view the details of this beacon")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).show();
                    return;
                }
                else if (beacon.status.equals(Beacon.STATUS_UNSPECIFIED)) {
                    return;
                }
                else {
                    if (manageBeaconListener != null) {
                        Bundle bundle = new Bundle();
                        bundle.putString("accountName", accountNameView.getText().toString());
                        bundle.putParcelable("beacon", beacon);
                        manageBeaconListener.onBeaconItemClicked(bundle);
                    }
                }
            }
        });
        return rootView;

    }
    //TODO Read about accountPicker
    private void pickUserAccount() {
        String[] accountTypes = new String[] {"com.google"};
        Intent intent = AccountPicker.newChooseAccountIntent(null, null, accountTypes, false, null, null, null, null);
        startActivityForResult(intent, REQUEST_PICK_ACCOUNT);
    }

    @Override
    public void onResume() {
        super.onResume();
        // There could be multiple instances when we need to handle a UserRecoverableAuthException
        // from GMS. Run this check every time another activity has finished running.
        String accountName = getActivity().getSharedPreferences(Constants.PREFS_NAME, 0)
                .getString("accountName", "");
        if (!accountName.equals("")) {
            new AuthorizedServiceTask(getActivity(), accountName).execute();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case  REQUEST_PICK_ACCOUNT:
                if (resultCode == Activity.RESULT_OK) {
                    String email = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    accountNameView.setText(email);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("accountName", email);
                    editor.apply();
                }
                else if (resultCode == Activity.RESULT_CANCELED) {
                    // The account picker dialog closed without selecting an account.
                    // Notify users that they must pick an account to proceed.
                    Toast.makeText(getActivity(), "Please pick an account", Toast.LENGTH_SHORT).show();
                }
            case REQUEST_CODE_ENABLE_BLE:
                if (resultCode == Activity.RESULT_OK) {
                    createScanner();
                }
                if (resultCode == Activity.RESULT_CANCELED) {
                    Log.w(TAG, "Please enable Bluetooth");
                }
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
        arrayAdapter.add(beacon);
        arrayAdapter.sort(RSSI_COMPARATOR);
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
                int pos = arrayAdapter.getPosition(beacon);
                scannedBeaconsList.set(pos, fetchedBeacon);
                updateArrayAdapter();
            }
        };
        client.getBeacon(getBeaconCallback, beacon.getBeaconName());
    }

    private void updateArrayAdapter() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                arrayAdapter.notifyDataSetChanged();
            }
        });
    }

    private void createScanner() {
        BluetoothManager btManager = (BluetoothManager)getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManager.getAdapter();
        if(btAdapter == null || !btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_CODE_ENABLE_BLE);
        }
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Log.e(TAG, "Can't enable Bluetooth btadapter="+(btAdapter == null?"null":"oxi null")+" btadapter.enabled="+(!btAdapter.isEnabled()?"oxi enabled":"enabled"));
            return;
        }
        scanner = btAdapter.getBluetoothLeScanner();
    }

//    private String fetchToken(String email) throws IOException {
//        try {
//            //TODO Fix deprecated
//            return GoogleAuthUtil.getToken(getActivity(), email, Constants.AUTH_SCOPE);
//        } catch (UserRecoverableAuthException e) {
//            handleException(e);
//        } catch (GoogleAuthException e) {
//            Log.w(TAG, "Fatal Exception", e);
//        }
//
//        return null;
//    }

//    public void handleException(final Exception e) {
//        // Because this call comes from the AsyncTask, we must ensure that the following
//        // code instead executes on the UI thread.
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                if (e instanceof GooglePlayServicesAvailabilityException) {
//                    // The Google Play services APK is old, disabled, or not present.
//                    // Show a dialog created by Google Play services that allows
//                    // the user to update the APK
//                    int statusCode = ((GooglePlayServicesAvailabilityException) e)
//                            .getConnectionStatusCode();
//                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(statusCode,
//                            ListBeaconsFragment.this.getActivity(),
//                            REQUEST_ERROR_RECOVER);
//                    dialog.show();
//                } else if (e instanceof UserRecoverableAuthException) {
//                    // Unable to authenticate, such as when the user has not yet granted
//                    // the app access to the account, but the user can fix this.
//                    // Forward the user to an activity in Google Play services.
//                    Intent intent = ((UserRecoverableAuthException) e).getIntent();
//                    startActivityForResult(intent,
//                            REQUEST_ERROR_RECOVER);
//                }
//            }
//        });
//    }
}
