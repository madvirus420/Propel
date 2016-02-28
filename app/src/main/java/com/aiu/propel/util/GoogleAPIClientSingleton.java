package com.aiu.propel.util;

import android.app.Application;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;

/**
 * Created by Rohan on 11/10/2015.
 */
public class GoogleAPIClientSingleton extends Application {
    private static final String TAG = "Singleton class";
    static GoogleApiClient googleApiClient;
    private static GoogleAPIClientSingleton instance = new GoogleAPIClientSingleton();


    public GoogleAPIClientSingleton() {

    }

    public synchronized static GoogleApiClient getInstance() {
        if ( instance == null ) {
            instance = new GoogleAPIClientSingleton();
        }
        if ( googleApiClient == null ) {
            googleApiClient.reconnect();
        }
        return googleApiClient;
    }

    public static void setInstance( GoogleApiClient apiClient ) {
        googleApiClient = apiClient;
        Log.i( TAG, "Google api client initiated" );
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.e( TAG, "Singleton Terminated" );
    }
}
