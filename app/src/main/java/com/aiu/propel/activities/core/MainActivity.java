package com.aiu.propel.activities.core;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.aiu.propel.activities.core.MainActivity;
import com.aiu.propel.R;
import com.aiu.propel.fragments.core.ChallengeFragment;
import com.aiu.propel.fragments.core.GoProFragment;
import com.aiu.propel.util.GcmRegisterationIntent;
import com.aiu.propel.util.GoogleAPIClientSingleton;
import com.aiu.propel.fragments.core.LoginFragment;
import com.aiu.propel.fragments.core.StepCountFragment;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;



public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private boolean mIsInResolution;
    private GoogleApiClient mGoogleApiClient;
    protected static final int REQUEST_CODE_RESOLUTION = 1;
    private static final String KEY_IN_RESOLUTION = "is_in_resolution";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    /* HISTORY API */
    private static final int REQUEST_OAUTH = 1;
    private static final String DATE_FORMAT = "yyyy.MM.dd HH:mm:ss";
    private CallbackManager callbackManager;

    private boolean authInProgress = false;
    private static final String AUTH_PENDING = "auth_state_pending";
    private static final String TAG = "MainActivity";
    private AccessToken accessToken;

    TextView nav_name;
    TextView nav_email;
    ImageView imageView;


    String textToPrint = "";
    double stepsToPrint = 0;
    double distanceToPrint = 0;
    double timeToPrint = 0;

    private ShareDialog shareDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();

        setContentView(R.layout.activity_main);

        if ( savedInstanceState != null ) {
            mIsInResolution = savedInstanceState.getBoolean( KEY_IN_RESOLUTION, false );
        }


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                fragmentTransaction.addToBackStack(null);

                fragmentTransaction.replace(R.id.fragment_container_main, new GoProFragment());

                fragmentTransaction.commit();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if ( savedInstanceState != null ) {
            authInProgress = savedInstanceState.getBoolean( AUTH_PENDING );
        }

        //Setup GCM
        Intent regIntent = new Intent(getApplicationContext(), GcmRegisterationIntent.class);
        startService(regIntent);

        SharedPreferences prefs = getSharedPreferences("PropelPref", MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        fragmentTransaction.addToBackStack(null);

        fragmentTransaction.replace(R.id.fragment_container_main, new StepCountFragment());

        fragmentTransaction.commit();

    }



    public void getFacebookData(){
        final SharedPreferences prefs = getSharedPreferences("PropelPref", MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();
        GraphRequest request = GraphRequest.newMeRequest(
                accessToken,
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(
                            JSONObject object,
                            GraphResponse response) {
                        Log.i(TAG,response.toString() + "");
                        try {
                            Log.i(TAG, object.getString("name"));
                            editor.putString("name", object.getString("name"));
                            editor.putString("fb_id", object.getString("id"));
                            editor.apply();
                            nav_name = (TextView) findViewById( R.id.nav_name );
                            nav_email = ( TextView ) findViewById( R.id.nav_email );
                            if(nav_name !=null) {
                                nav_name.setText(prefs.getString("name", "Android Propel"));
                            }
                            String imageURL = object.getJSONObject("picture").getJSONObject("data").getString("url");
                            Log.i(TAG, imageURL);
                            imageView = (ImageView) findViewById(R.id.imageView);
                            Log.i(TAG, "imageURL = " + "https://graph.facebook.com/" + prefs.getString("fb_id", "") + "/picture?type=large");
                            Picasso.with(getApplicationContext()).load("https://graph.facebook.com/" + prefs.getString("fb_id", "") + "/picture?type=large").into(imageView);
                        }catch(JSONException e){
                            Log.e(TAG, "Error parsing FB JSON");
                        }catch(Exception e){
                            Log.e(TAG, "Waat laagli");
                            e.printStackTrace();
                        }

                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,picture, email ");
        request.setParameters(parameters);
        request.executeAsync();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        accessToken = AccessToken.getCurrentAccessToken();

        SharedPreferences prefs = getSharedPreferences( "PropelPref", MODE_PRIVATE );
        nav_name = ( TextView ) findViewById( R.id.nav_name );
        nav_email = ( TextView ) findViewById(R.id.nav_email );
        String fName = prefs.getString( "name", "" );

        if ( nav_name != null ) {
            nav_name.setText( fName );
        }
        if ( nav_email != null ) {
            nav_email.setText( prefs.getString( "email", "login@propel.com" ) );

        }

        if (accessToken != null) {

            getFacebookData();

        }


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        SharedPreferences prefs = getSharedPreferences("PropelPref", MODE_PRIVATE);
        int id = item.getItemId();
        Log.i("MAIN", "selected id: " + id);
        Log.i("MAIN", "challenge id: " + R.id.nav_challenge);
       if (id == R.id.nav_share) {

            shareDialog = new ShareDialog( this );


                if ( ShareDialog.canShow( ShareLinkContent.class ) ) {
                    ShareLinkContent linkContent = new ShareLinkContent.Builder()
                            .setContentTitle( "Propel - Fitness App" )
                            .setContentDescription(
                                    "I am getting fit without inuries. How about you?" )
                            .setContentUrl( Uri.parse("https://en.wikipedia.org/wiki/Physical_fitness") )
                            .build();

                    shareDialog.show( linkContent );

                }

            } else if( id == R.id.nav_dashboard ){
            Log.i("MAIN", "chal mere bhai");
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            StepCountFragment stepCountFragment = new StepCountFragment();
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.replace(R.id.fragment_container_main, stepCountFragment);//nav_dashboard
            fragmentTransaction.commit();
        }else if( id == R.id.nav_challenge ){
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            Log.i(TAG, "challenge menu selected");
            accessToken = AccessToken.getCurrentAccessToken();


            if( accessToken == null ){
                LoginFragment loginFragment = new LoginFragment();
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.replace(R.id.fragment_container_main, loginFragment);
                fragmentTransaction.commit();

            }else{
                final SharedPreferences.Editor editor = prefs.edit();

                getFacebookData();

                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.replace(R.id.fragment_container_main, new ChallengeFragment());
                fragmentTransaction.commit();
            }
        }else if ( id == R.id.nav_go_pro){
           FragmentManager fragmentManager = getFragmentManager();
           FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

           fragmentTransaction.addToBackStack(null);

           fragmentTransaction.replace(R.id.fragment_container_main, new GoProFragment());

           fragmentTransaction.commit();
       }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Logs 'install' and 'app activate' App Events.
        AppEventsLogger.activateApp(this);

        /*new InsertAndVerifyDataTask().execute();*/
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Logs 'app deactivate' App Event.
        AppEventsLogger.deactivateApp(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if ( mGoogleApiClient == null ) {
            mGoogleApiClient = new GoogleApiClient.Builder( this )
                    .addApi( Fitness.RECORDING_API )
                    .addApi( Fitness.HISTORY_API )
                    .addScope( new Scope( Scopes.FITNESS_LOCATION_READ_WRITE ) )
                    .addScope( new Scope( Scopes.FITNESS_ACTIVITY_READ_WRITE ) )
                    .addScope( new Scope( Scopes.FITNESS_BODY_READ_WRITE ) )
                    .useDefaultAccount()
                    .addConnectionCallbacks( this )
                    .addOnConnectionFailedListener( this )
                    .build();
        }
        mGoogleApiClient.connect();

        //Initilize the Facebook SDK
        FacebookSdk.sdkInitialize(getApplicationContext());
    }




    /**
     * Saves the resolution state.
     */
    @Override
    protected void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IN_RESOLUTION, mIsInResolution);
    }
    /**
     * Handles Google Play Services resolution callbacks.
     */
    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        super.onActivityResult(requestCode, resultCode, data);
        switch ( requestCode ) {
            case REQUEST_CODE_RESOLUTION:
                retryConnecting();
                break;
            default:
                callbackManager.onActivityResult( requestCode, resultCode, data );
                Log.i(TAG, "Result code: " + resultCode);
        }
    }

    private void retryConnecting() {
        mIsInResolution = false;
        if ( !mGoogleApiClient.isConnecting() ) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnected( Bundle connectionHint ) {
        Log.i( TAG, "GoogleApiClient connected and now initializing singleton object" );
        /*Intent runSensors = new Intent( getApplicationContext(), RunningActivity.class );
        startActivity( runSensors );

        GoogleAPIClientSingleton apiClientSingleton = new GoogleAPIClientSingleton();
        apiClientSingleton.setInstance(mGoogleApiClient);
        */

        GoogleAPIClientSingleton.setInstance( mGoogleApiClient );
        Fitness.RecordingApi.subscribe( mGoogleApiClient, DataType.TYPE_STEP_COUNT_DELTA )
                .setResultCallback( new ResultCallback<Status>() {
                    @Override
                    public void onResult( Status status ) {
                        if ( status.isSuccess() ) {
                            if ( status.getStatusCode()
                                    == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED ) {
                                Log.i( TAG, "Existing subscription for activity detected." );
                            } else {
                                Log.i( TAG, "Successfully subscribed!" );
                            }
                        } else {
                            Log.i( TAG, "There was a problem subscribing." );
                        }
                    }
                } );
        GoogleAPIClientSingleton.setInstance( mGoogleApiClient );
        Fitness.RecordingApi.subscribe( mGoogleApiClient, DataType.TYPE_DISTANCE_DELTA )
                .setResultCallback( new ResultCallback<Status>() {
                    @Override
                    public void onResult( Status status ) {
                        if ( status.isSuccess() ) {
                            if ( status.getStatusCode()
                                    == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED ) {
                                Log.i( TAG, "Existing subscription for activity detected." );
                            } else {
                                Log.i( TAG, "Successfully subscribed!" );
                            }
                        } else {
                            Log.i( TAG, "There was a problem subscribing." );
                        }
                    }
                } );
        GoogleAPIClientSingleton.setInstance( mGoogleApiClient );
        Fitness.RecordingApi.subscribe( mGoogleApiClient, DataType.TYPE_CALORIES_EXPENDED )
                .setResultCallback( new ResultCallback<Status>() {
                    @Override
                    public void onResult( Status status ) {
                        if ( status.isSuccess() ) {
                            if ( status.getStatusCode()
                                    == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED ) {
                                Log.i( TAG, "Existing subscription for activity detected." );
                            } else {
                                Log.i( TAG, "Successfully subscribed!" );
                            }
                        } else {
                            Log.i( TAG, "There was a problem subscribing." );
                        }
                    }
                } );
        GoogleAPIClientSingleton.setInstance(mGoogleApiClient);
        Fitness.RecordingApi.subscribe( mGoogleApiClient, DataType.TYPE_LOCATION_SAMPLE )
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            if (status.getStatusCode()
                                    == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                Log.i(TAG, "Existing subscription for activity detected.");
                            } else {
                                Log.i(TAG, "Successfully subscribed!");
                            }
                        } else {
                            Log.i(TAG, "There was a problem subscribing.");
                        }
                    }
                });

        // HISTORY API Called
        new InsertAndVerifyDataTask().execute();

    }

    /**
     * Called when {@code mGoogleApiClient} connection is suspended.
     */
    @Override
    public void onConnectionSuspended( int cause ) {
        Log.i(TAG, "GoogleApiClient connection suspended");
        retryConnecting();
    }

    /**
     * Called when {@code mGoogleApiClient} is trying to connect but failed.
     * Handle {@code result.getResolution()} if there is a resolution
     * available.
     */
    @Override
    public void onConnectionFailed( ConnectionResult result ) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if ( !result.hasResolution() ) {
            // Show a localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(
                    result.getErrorCode(), this, 0, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            retryConnecting();
                        }
                    }).show();
            return;
        }
        // If there is an existing resolution error being displayed or a resolution
        // activity has started before, do nothing and wait for resolution
        // progress to be completed.
        if ( mIsInResolution ) {
            return;
        }
        mIsInResolution = true;
        try {
            result.startResolutionForResult( this, REQUEST_CODE_RESOLUTION );
        } catch ( IntentSender.SendIntentException e ) {
            Log.e( TAG, "Exception while starting resolution activity", e );
            retryConnecting();
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable( this );
        if ( resultCode != ConnectionResult.SUCCESS ) {
            if ( apiAvailability.isUserResolvableError( resultCode ) ) {
                apiAvailability.getErrorDialog( this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST )
                        .show();
            } else {
                Log.i( TAG, "This device is not supported." );
                finish();
            }
            return false;
        }
        return true;
    }


    /**
     * Return a {@link DataReadRequest} for all step count changes in the past week.
     */
    private DataReadRequest queryFitnessData() {
        // [START build_read_data_request]
        // Setting a start and end date using a range of 1 week before this moment.
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime( now );
        cal.set( Calendar.HOUR_OF_DAY, 23 );
        cal.set( Calendar.MINUTE, 59 );
        cal.set( Calendar.SECOND, 59 );
        cal.set( Calendar.MILLISECOND, 999 );
        long endTime = cal.getTimeInMillis();
        cal.add( Calendar.DAY_OF_YEAR, -1 );
        long startTime = cal.getTimeInMillis();

        SimpleDateFormat dateFormat = new SimpleDateFormat( DATE_FORMAT );
        Log.i( TAG, "Range Start: " + dateFormat.format( startTime ) );
        Log.i( TAG, "Range End: " + dateFormat.format( endTime ) );

        textToPrint = "";
        stepsToPrint = 0;
        timeToPrint = 0;
        distanceToPrint = 0;

        DataSource ESTIMATED_STEP_DELTAS = new DataSource.Builder()
                .setDataType( DataType.TYPE_STEP_COUNT_DELTA )
                .setType( DataSource.TYPE_DERIVED )
                .setStreamName( "estimated_steps" )
                .setAppPackageName( "com.google.android.gms" )
                .build();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                // The data request can specify multiple data types to return, effectively
                // combining multiple data queries into one call.
                // In this example, it's very unlikely that the request is for several hundred
                // datapoints each consisting of a few steps and a timestamp.  The more likely
                // scenario is wanting to see how many steps were walked per day, for 7 days.
                .aggregate( ESTIMATED_STEP_DELTAS, DataType.AGGREGATE_STEP_COUNT_DELTA )
                .aggregate( DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA )
                .aggregate( DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED )
                .aggregate( DataType.TYPE_LOCATION_SAMPLE, DataType.AGGREGATE_LOCATION_BOUNDING_BOX )
                        // Analogous to a "Group By" in SQL, defines how data should be aggregated.
                        // bucketByTime allows for a time span, whereas bucketBySession would allow
                        // bucketing by "sessions", which would need to be defined in code.
                .bucketByTime( 1, TimeUnit.HOURS )
                .enableServerQueries()
                .setTimeRange( startTime, endTime, TimeUnit.MILLISECONDS )
                .build();
        // [END build_read_data_request]

        return readRequest;
    }

    private void printData( DataReadResult dataReadResult ) {
        // [START parse_read_data_result]
        // If the DataReadRequest object specified aggregated data, dataReadResult will be returned
        // as buckets containing DataSets, instead of just DataSets.
        if ( dataReadResult.getBuckets().size() > 0 ) {
            Log.i( TAG, "Number of returned buckets of DataSets is: "
                    + dataReadResult.getBuckets().size() );
            for ( Bucket bucket : dataReadResult.getBuckets() ) {
                List<DataSet> dataSets = bucket.getDataSets();
                for ( DataSet dataSet : dataSets ) {
                    dumpDataSet( dataSet );
                }
            }
        } else if ( dataReadResult.getDataSets().size() > 0 ) {
            Log.i( TAG, "Number of returned DataSets is: "
                    + dataReadResult.getDataSets().size() );
            for ( DataSet dataSet : dataReadResult.getDataSets() ) {
                dumpDataSet( dataSet );
            }
        }
        // [END parse_read_data_result]
    }

    // [START parse_dataset]
    private void dumpDataSet( DataSet dataSet ) {
        if ( !dataSet.isEmpty() ) {
            Log.i( TAG, "Data returned for Data type: " + dataSet.getDataType().getName() );
            SimpleDateFormat dateFormat = new SimpleDateFormat( DATE_FORMAT );
            for ( DataPoint dp : dataSet.getDataPoints() ) {
                Log.i( TAG, "Data point:" );
                Log.i( TAG, "\tType: " + dp.getDataType().getName() );
                Log.i( TAG, "\tStart: " + dateFormat.format( dp.getStartTime( TimeUnit.MILLISECONDS ) ) );
                Log.i( TAG, "\tEnd: " + dateFormat.format( dp.getEndTime( TimeUnit.MILLISECONDS ) ) );
                textToPrint += "\n" + dp.getDataType().getName() +
                        "\n Start: " + dateFormat.format( dp.getStartTime( TimeUnit.MILLISECONDS ) ) +
                        "\n End: " + dateFormat.format( dp.getEndTime( TimeUnit.MILLISECONDS ) );
                for ( Field field : dp.getDataType().getFields() ) {
                    Log.i( TAG, "\tField: " + field.getName() +
                            " Value: " + dp.getValue( field ) );
                    textToPrint += "\n" + "Field: " + field.getName() +
                            " Value: " + dp.getValue( field );

                    switch ( field.getName().trim() ) {
                        case "steps":
                            stepsToPrint += Double.parseDouble( dp.getValue( field ).toString().trim() );
                            break;
                        case "distance":
                            distanceToPrint += Double.parseDouble( dp.getValue( field ).toString().trim() );
                            break;
                        case "calories":
                            timeToPrint += Double.parseDouble( dp.getValue( field ).toString().trim() );
                            break;
                    }

                }
            }
        }
    }

    private class InsertAndVerifyDataTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground( Void... params ) {

            // Begin by creating the query.
            DataReadRequest readRequest = queryFitnessData();

            // [START read_dataset]
            // Invoke the History API to fetch the data with the query and await the result of
            // the read request.
            DataReadResult dataReadResult =
                    Fitness.HistoryApi.readData( mGoogleApiClient, readRequest ).await( 1, TimeUnit.MINUTES );
            // [END read_dataset]

            printData( dataReadResult );

            return null;
        }

        @Override
        protected void onPostExecute( Void aVoid ) {
            super.onPostExecute( aVoid );
            Log.i(TAG, "welcome to the shit");
            /*fitness_steps.setText( ( int ) stepsToPrint + "" );
            BigDecimal bd = new BigDecimal( distanceToPrint / 1000 );
            bd = bd.setScale( 2, RoundingMode.HALF_UP );
            fitness_distance.setText( ( ( bd ) ) + "" );
            fitness_time.setText( ( int ) timeToPrint + "" );*/
            //fitness_time.setText( ( int ) 15 + "" );

        }
    }
}
