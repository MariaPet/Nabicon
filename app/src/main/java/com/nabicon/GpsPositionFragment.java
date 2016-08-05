package com.nabicon;


import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.app.Fragment;
import android.support.v4.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 */
public class GpsPositionFragment extends Fragment implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = GpsPositionFragment.class.getSimpleName();
    private GoogleApiClient mGoogleApiClient;
    private Location mlastLocation;
    private AddressResultReceiver mResultReceiver;
    private ConfigureBeaconsListener configureBeaconsListener;

    public interface ConfigureBeaconsListener {
        void onConfigureBeaconsClicked();
    }

    public GpsPositionFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gps_position, container, false);
        mResultReceiver = new AddressResultReceiver(new Handler());
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        Button configureBeacons = (Button) view.findViewById(R.id.buttonConfigureBeacons);
        configureBeacons.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (configureBeaconsListener != null) {
                    configureBeaconsListener.onConfigureBeaconsClicked();
                }
            }
        });
        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onStart(){
        mGoogleApiClient.connect();
        super.onStart(); // For some reason the call to superclass constructor is not first
    }

    @Override
    public void onStop(){
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ConfigureBeaconsListener) {
            configureBeaconsListener = (ConfigureBeaconsListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement ConfigureBeaconsListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        configureBeaconsListener = null;
    }

    protected void startIntentService() {
        Intent intent = new Intent(getActivity(), FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mlastLocation);
        getActivity().startService(intent);
    }

    @Override
    public void onConnected(Bundle bundle) {
        mlastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mlastLocation != null) {
            Log.i(TAG, "Longitude: " + String.valueOf(mlastLocation.getLongitude()));
            Log.i(TAG, "Latitude: " + String.valueOf(mlastLocation.getLatitude()));

            // Determine whether a Geocoder is available.
            if (!Geocoder.isPresent()){
                Log.w(TAG, "No Geocoder available");
                return;
            }
            //TODO Refer to a condition changed after a button is pressed in the UI.
            startIntentService();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @SuppressLint("ParcelCreator")
    class AddressResultReceiver extends ResultReceiver {

        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            // Display the address string
            // or an error message sent from the intent service.
            String mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            final TextView gpsAddressView = (TextView) getView().findViewById(R.id.gps_address);
            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESSFUL_RESULT) {
                Log.i(TAG, "Your address is: " + mAddressOutput);
                gpsAddressView.setText("Your address is: " + mAddressOutput);
            }
            else {
                Log.e(TAG, "Something went wrong, the address is not retrieved");
                gpsAddressView.setText("Something went wrong, the address is not retrieved");
            }

        }
    }

    public static class FetchAddressIntentService extends IntentService {

        protected ResultReceiver mResultReceiver;

        public FetchAddressIntentService() {
            super(FetchAddressIntentService.class.getSimpleName());

        }

        @Override
        protected void onHandleIntent(Intent intent) {
            Log.d(TAG, "background thread started");
            // Get the location passed to this service through an extra.
            mResultReceiver = intent.getParcelableExtra(Constants.RECEIVER);
            Location location = intent.getParcelableExtra(
                    Constants.LOCATION_DATA_EXTRA);
            List<Address> addresses = null;
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());

            try {
                addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            } catch (IOException e) {
                // Catch network or other I/O problems.
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                //Catch invalid latitude or longitude values.
                e.printStackTrace();
            }

            // Handle case where no address was found.
            if (addresses == null || addresses.size() == 0) {
                Log.e(TAG, "No address is returned");
                deliverResultToReceiver(Constants.FAILURE_RESULT, "No address is returned");
            } else {
                Address address = addresses.get(0);
                ArrayList<String> addressParts = new ArrayList<String>();
                for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                    addressParts.add(address.getAddressLine(i));
                }
                deliverResultToReceiver(Constants.SUCCESSFUL_RESULT,
                        TextUtils.join(System.getProperty("line.separator"), addressParts));
            }

        }

        private void deliverResultToReceiver(int resultCode, String message) {
            Bundle addressBundle = new Bundle();
            addressBundle.putString(Constants.RESULT_DATA_KEY, message);
            mResultReceiver.send(resultCode, addressBundle);
        }
    }

}
