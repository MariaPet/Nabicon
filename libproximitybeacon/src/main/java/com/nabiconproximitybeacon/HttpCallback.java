package com.nabiconproximitybeacon;

import android.os.Handler;
import android.os.Looper;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

/**
 * Created by mariloo on 22/4/2016.
 */
public class HttpCallback implements Callback{
    private final Callback delegate;
    private final Handler handler;

    public HttpCallback(Callback delegate) {
        this.delegate = delegate;
        this.handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onFailure(final Request request, final IOException e) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                delegate.onFailure(request, e);
            }
        });
    }

    @Override
    public void onResponse(final Response response) throws IOException {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try{
                    delegate.onResponse(response);
                }
                catch (IOException e) {
                    delegate.onFailure(null, e);
                }
            }
        });
    }
}
