package com.nabicon;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mariloo on 10/7/2016.
 */
public class Beacon implements Parcelable{

    private static final String TAG = Beacon.class.getSimpleName();

    // These constants are in the Proximity Service Status enum:
    public static final String STATUS_UNSPECIFIED = "STATUS_UNSPECIFIED";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_DECOMMISSIONED = "DECOMMISSIONED";
    public static final String STABILITY_UNSPECIFIED = "STABILITY_UNSPECIFIED";

    // These constants are convenience for this app:
    public static final String UNREGISTERED = "UNREGISTERED";
    public static final String NOT_AUTHORIZED = "NOT_AUTHORIZED";

    public String type;
    public byte[] id;
    public String status;
    public String placeId;
    public Double latitude;
    public Double longitude;
    public String expectedStability;
    public String description;
    // This isn't really a beacon property, but it's useful to have it here so we can sort
    // the list of beacons during scanning so the closest and/or strongest is listed first.
    // It doesn't need to be persisted via the parcelable.
    public int rssi;


    //TODO check why this should be here
    protected Beacon(Parcel source) {
        type = source.readString();
        int len = source.readInt();
        id = new byte[len];
        //TODO what does this statement do?
        source.readByteArray(id);
        status = source.readString();
        if (source.readInt() == 1) {
            placeId = source.readString();
        }
        if (source.readInt() == 1) {
            latitude = source.readDouble();
        }
        if (source.readInt() == 1) {
            longitude = source.readDouble();
        }
        if (source.readInt() == 1) {
            expectedStability = source.readString();
        }
        if (source.readInt() == 1) {
            description = source.readString();
        }
    }

    public Beacon(String type, byte[] id, String status, int rssi) {
        this.type = type;
        this.id = id;
        this.status = status;
        this.placeId = null;
        this.latitude = null;
        this.longitude = null;
        this.expectedStability = null;
        this.description = null;
        this.rssi = rssi;
    }

    public Beacon(JSONObject response) {
        try {
            JSONObject json = response.getJSONObject("advertisedId");
            type = json.getString("type");
            id = Utils.base64Decode(json.getString("id"));
        }
        catch (Exception e) {

        }
        try {
            status = response.getString("status");
        }
        catch (Exception e) {
            status = STATUS_UNSPECIFIED;
        }
        try {
            placeId = response.getString("placeId");
        }
        catch (Exception e) {

        }
        try {
            JSONObject geoCoordinates = response.getJSONObject("latLng");
            latitude = geoCoordinates.getDouble("latitude");
            longitude = geoCoordinates.getDouble("longitude");
        }
        catch (Exception e) {
            latitude = null;
            longitude = null;
        }
        try {
            expectedStability = response.getString("expectedStability");
        }
        catch (Exception e) {

        }
        try {
            description = response.getString("description");
        }
        catch (Exception e) {

        }
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        JSONObject advertisedId = new JSONObject()
                .put("type", type)
                .put("id", Utils.base64Encode(id));
        json.put("advertisedId", advertisedId);
        if (!status.equals(STATUS_UNSPECIFIED)) {
            json.put("status", status);
        }
        if (placeId != null) {
            json.put("placeId", placeId);
        }
        if (latitude != null && longitude != null) {
            JSONObject geoCoordinates = new JSONObject()
                    .put("latitude", latitude)
                    .put("longitude", longitude);
            json.put("latLng", geoCoordinates);
        }
        if (expectedStability != null && !expectedStability.equals(STABILITY_UNSPECIFIED)) {
            json.put("expectedStability", expectedStability);
        }
        if (description != null) {
            json.put("description", description);
        }
        //TODO beacon properties
        return json;
    }

    public String getHexId() {
        return Utils.toHexString(id);
    }

    /**
     * The beaconName is formatted as "beacons/%d!%s" where %d is an integer representing the
     * beacon ID type. For Eddystone this is 3. The %s is the base16 (hex) ASCII for the ID bytes.
     */
    public String getBeaconName() {return String.format("beacons/3!%s", Utils.toHexString(id));}

    public LatLng getLatLng() {
        if (latitude == null || longitude == null) {
            return null;
        }
        else  {
            return new LatLng(latitude, longitude);
        }
    }

    public static final Parcelable.Creator<Beacon> CREATOR = new Creator<Beacon>() {
        @Override
        public Beacon createFromParcel(Parcel source) { return new Beacon(source);}

        @Override
        public Beacon[] newArray(int size) { return new Beacon[size];}
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type);
        dest.writeByteArray(id);
        dest.writeString(status);
        if (placeId != null) {
            dest.writeInt(1);
            dest.writeString(placeId);
        }
        else {
            dest.writeInt(0);
        }
        if (latitude != null) {
            dest.writeInt(1);
            dest.writeDouble(latitude);
        }
        else {
            dest.writeInt(0);
        }
        if (longitude != null) {
            dest.writeInt(1);
            dest.writeDouble(longitude);
        }
        else {
            dest.writeInt(0);
        }
        if (expectedStability != null) {
            dest.writeInt(1);
            dest.writeString(expectedStability);
        }
        else {
            dest.writeInt(0);
        }
        if (description != null) {
            dest.writeInt(1);
            dest.writeString(description);
        }
        else {
            dest.writeInt(0);
        }
    }
}
