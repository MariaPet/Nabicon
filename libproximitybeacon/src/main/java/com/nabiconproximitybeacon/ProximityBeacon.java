package com.nabiconproximitybeacon;

import com.squareup.okhttp.Callback;

import org.json.JSONObject;

/**
 * Created by mariloo on 22/4/2016.
 */
public interface ProximityBeacon {

    /**
     * https://developers.google.com/beacons/proximity/reference/rest/v1beta1/beacons/get
     */
    void getBeacon(Callback callback, String beaconName);

    /**
     * https://developers.google.com/beacons/proximity/reference/rest/v1beta1/beacons/activate
     */
    void activateBeacon(Callback callback, String beaconName);

    /**
     * https://developers.google.com/beacons/proximity/reference/rest/v1beta1/beacons/deactivate
     */
    void deactivateBeacon(Callback callback, String beaconName);

    /**
     * https://developers.google.com/beacons/proximity/reference/rest/v1beta1/beacons/register
     */
    void registerBeacon(Callback callback, JSONObject requestBody);

    /**
     * https://developers.google.com/beacons/proximity/reference/rest/v1beta1/beacons/update
     */
    void updateBeacon(Callback callback, String beaconName, JSONObject requestBody);

    /**
     * https://developers.google.com/beacons/proximity/reference/rest/v1beta1/namespaces/list
     */
    void listNamespaces(Callback callback);

    /**
     * https://developers.google.com/beacons/proximity/reference/rest/v1beta1/beacons.attachments/create
     */
    void createAttachment(Callback callback, String beaconName, JSONObject requestBody);

    /**
     * https://developers.google.com/beacons/proximity/reference/rest/v1beta1/beacons.attachments/delete
     */
    void deleteAttachment(Callback callback, String attachmentName);

    /**
     * https://developers.google.com/beacons/proximity/reference/rest/v1beta1/beacons.attachments/list
     */
    void listAttachments(Callback callback, String beaconName);
}
