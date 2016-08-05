package com.nabiconproximitybeacon;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by mariloo on 22/4/2016.
 */
public class ProximityBeaconImpl implements ProximityBeacon{

    private static final String TAG = ProximityBeaconImpl.class.getSimpleName();
    private static final String SCOPE = "oauth2:https://www.googleapis.com/auth/userlocation.beacon.registry";
    private static final String ENDPOINT = "https://proximitybeacon.googleapis.com/v1beta1/";
    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    private static final int GET = 0;
    private static final int PUT = 1;
    private static final int POST = 2;
    private static final int DELETE = 3;

    private final String account;
    private final Context ctx;
    private final OkHttpClient httpClient;

    public ProximityBeaconImpl(Context ctx, String account) {
        this.account = account;
        this.ctx = ctx;
        this.httpClient = new OkHttpClient();
    }

    @Override
    public void getBeacon(Callback callback, String beaconName) {
        new AuthTask(beaconName, callback).execute();
    }

    @Override
    public void activateBeacon(Callback callback, String beaconName) {
        new AuthTask(beaconName + ":activate", POST, "", callback).execute();
    }

    @Override
    public void deactivateBeacon(Callback callback, String beaconName) {
        new AuthTask(beaconName + ":deactivate", POST, "", callback).execute();
    }

    @Override
    public void registerBeacon(Callback callback, JSONObject requestBody) {
        new AuthTask("beacons:register", POST, requestBody.toString(), callback).execute();
    }

    @Override
    public void updateBeacon(Callback callback, String beaconName, JSONObject requestBody) {
        new AuthTask(beaconName, PUT, requestBody.toString(), callback).execute();
    }

    @Override
    public void listNamespaces(Callback callback) {
        new AuthTask("namespaces", callback).execute();
    }

    @Override
    public void listAttachments(Callback callback, String beaconName) {
        new AuthTask(beaconName + "/attachments?namespacedType=*/*", callback).execute();
    }

    private class AuthTask extends AsyncTask<Void, Void, Void> {

        public static final String AUTHORIZATION = "Authorization";
        public static final String BEARER = "Bearer ";
        private final String urlPart;
        private final int method;
        private final String json;
        private final Callback callback;

        AuthTask(String urlPart, Callback callback) {
            this(urlPart, GET, "", callback);
        }

        AuthTask(String urlPart, int method, String json, Callback callback) {
            this.urlPart = urlPart;
            this.method = method;
            this.json = json;
            this.callback = callback;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try{
                final String token = GoogleAuthUtil.getToken(ctx, account, SCOPE);
                Request.Builder requestBuilder = new Request.Builder()
                        .header(AUTHORIZATION, BEARER + token)
                        .url(ENDPOINT + urlPart);
                switch (method) {
                    case PUT:
                        requestBuilder.put(RequestBody.create(MEDIA_TYPE_JSON, json));
                        break;
                    case POST:
                        requestBuilder.post(RequestBody.create(MEDIA_TYPE_JSON, json));
                        break;
                    case DELETE:
                        requestBuilder.delete(RequestBody.create(MEDIA_TYPE_JSON, json));
                        break;
                    default: break;
                }
                Request request = requestBuilder.build();
                httpClient.newCall(request).enqueue(new HttpCallback(callback));
            }
            catch (UserRecoverableAuthException e) {
                Log.e(TAG, "UserRecoverableAuthException: " + e.getMessage());
            }
            catch (GoogleAuthException e) {
                Log.e(TAG, "GoogleAuthException: " + e.getMessage());
            }
            catch (IOException e) {
                Log.e(TAG, "IOException: " + e.getMessage());
            }
            return null;
        }
    }
}
