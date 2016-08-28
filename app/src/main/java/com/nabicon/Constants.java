package com.nabicon;

/**
 * Created by mariloo on 20/4/2016.
 */
public class Constants {

    private Constants() {}

    static final String AUTH_SCOPE = "oauth2:https://www.googleapis.com/auth/userlocation.beacon.registry";

    //Location address constants
    public static final int SUCCESSFUL_RESULT = 0;
    public static final int FAILURE_RESULT = 1;
    static final int REQUEST_CODE_PLACE_PICKER = 1003;
    static final int REQUEST_CODE_ENABLE_BLE = 2;
    //TODO why do we use this package name?
    public static final String PACKAGE_NAME =
            "com.google.android.gms.location.sample.locationaddress";
    public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";
    public static final String RESULT_DATA_KEY = PACKAGE_NAME + ".RESULT_DATA_KEY";
    public static final String LOCATION_DATA_EXTRA = PACKAGE_NAME + ".LOCATION_DATA_EXTRA";
    //Unique name of the shared preferences file
    public static final String PREFS_NAME = "com.nabicon.Prefs";
}
