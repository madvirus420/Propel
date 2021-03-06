package com.aiu.propel.util;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import com.aiu.propel.R;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import org.json.JSONObject;

import java.io.IOException;


/**
 * Created by charlie on 10/1/16.
 */
public class GcmRegisterationIntent extends IntentService {

    private static final String TAG = "RegIntentService";
    private static final String[] TOPICS = {"global"};

    public GcmRegisterationIntent() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            // [START register_for_gcm]
            // Initially this call goes out to the network to retrieve the token, subsequent calls
            // are local.
            // R.string.gcm_defaultSenderId (the Sender ID) is typically derived from google-services.json.
            // See https://developers.google.com/cloud-messaging/android/start for details on this file.
            // [START get_token]
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // [END get_token]
            Log.i(TAG, "GCM Registration Token: " + token);

            // TODO: Implement this method to send any registration to your app's servers.
            //sendRegistrationToServer(token);

            // Subscribe to topic channels
            subscribeTopics(token);

            // You should store a boolean that indicates whether the generated token has been
            // sent to your server. If the boolean is false, send the token to your server,
            // otherwise your server should have already received the token.
            SharedPreferences prefs = getSharedPreferences( "MagicBusPreferences", MODE_PRIVATE );
            prefs.edit().putBoolean(Constants.SENT_TOKEN_TO_SERVER, true).apply();
            // [END register_for_gcm]




        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            sharedPreferences.edit().putBoolean(Constants.SENT_TOKEN_TO_SERVER, false).apply();
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
        Intent registrationComplete = new Intent(Constants.REGISTRATION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    /**
     * Persist registration to third-party servers.
     *
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {

/*
        try{

            SharedPreferences prefs = getSharedPreferences( "MagicBusPreferences", MODE_PRIVATE );
            OkHttpClient client = new OkHttpClient();
            String url = Constants.SERVER_URL + Constants.API_URL + Constants.GCM_REGISTER_DEVICE;
            JsonObject json = new JsonObject();



            String email = prefs.getString("email", "");
            json.addProperty("emailID", email);
            json.addProperty("deviceID", token);
            String gson = json.toString();


            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), gson);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            Response response = client.newCall(request).execute();
            Log.i("regDev", response.body().string());
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            else{
                JsonObject resp =  new JsonObject();
                JsonParser parser = new JsonParser();

                JSONObject jsonResponse = new JSONObject( response.toString() );
                if ( jsonResponse.getInt( "status" ) == 1 ) {
                    Log.i("regDev",jsonResponse.getString("message"));
                }
            }
        }catch(Exception e){

        }*/

    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
    // [END subscribe_topics]

}