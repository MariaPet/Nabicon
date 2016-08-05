package com.nabicon;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import java.io.IOException;

/**
 * Created by mariloo on 14/7/2016.
 * NOP async task that allows us to check if a new user has authorized the app
 * to access their account.
 */
public class AuthorizedServiceTask extends AsyncTask<Void, Void, Void>{

    private static final String TAG = AuthorizedServiceTask.class.getSimpleName();
    private static final int REQ_SIGN_IN_REQUIRED = 55664;

    private final Activity activity;
    private final String accountName;

    public AuthorizedServiceTask(Activity activity, String accountName) {
        this.activity = activity;
        this.accountName = accountName;
    }

    @Override
    protected Void doInBackground(Void... params) {
        Log.i(TAG, "Checking authorization for " + accountName);
        try {
            GoogleAuthUtil.getToken(activity, accountName, Constants.AUTH_SCOPE);
        }
        catch (UserRecoverableAuthException e) {
            Log.e(TAG, e.getMessage());
            //The line below present the user with an extra screen to allow the app handle missing permissions
            //activity.startActivityForResult(e.getIntent(), REQ_SIGN_IN_REQUIRED);
        }
        catch (GoogleAuthException e) {
            Log.e(TAG, e.getMessage());
        }
        catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }
}
