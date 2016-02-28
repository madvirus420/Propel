package com.aiu.propel.fragments.core;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.content.res.TypedArrayUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aiu.propel.R;
import com.aiu.propel.dao.SQLiteHandler;
import com.aiu.propel.dao.SQLiteHandlerImpl;
import com.aiu.propel.util.GoogleAPIClientSingleton;
import com.aiu.propel.util.RpeVO;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
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
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.result.ListSubscriptionsResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class GoProFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    public static final String TAG = "GoPro";

    private boolean mIsInResolution;
    private GoogleApiClient mGoogleApiClient;
    protected static final int REQUEST_CODE_RESOLUTION = 1;
    private static final String KEY_IN_RESOLUTION = "is_in_resolution";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String DATE_FORMAT = "yyyy.MM.dd HH:mm:ss";
    private static final String AUTH_PENDING = "auth_state_pending";
    private boolean authInProgress = false;
    private boolean recording = false;
    private String mParam1;
    private String mParam2;
    private Long t1;
    private Long t2;
    private Long diff;
    private TextView textGo;
    String m_Text;
    SQLiteHandler helper;

    private OnFragmentInteractionListener mListener;

    public GoProFragment() {
        // Required empty public constructor
    }


    public static GoProFragment newInstance(String param1, String param2) {
        GoProFragment fragment = new GoProFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        if ( savedInstanceState != null ) {
            mIsInResolution = savedInstanceState.getBoolean(KEY_IN_RESOLUTION, false);
        }

        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }

        if ( mGoogleApiClient == null ) {
            mGoogleApiClient = new GoogleApiClient.Builder( this.getActivity() )
                    .addApi(Fitness.RECORDING_API)
                    .addApi(Fitness.HISTORY_API)
                    .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                    .addScope(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                    .addScope( new Scope( Scopes.FITNESS_BODY_READ_WRITE ) )
                    .useDefaultAccount()
                    .addConnectionCallbacks( this )
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_go, container, false);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    /**
     * Handles Google Play Services resolution callbacks.
     */
    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data ) {
        super.onActivityResult(requestCode, resultCode, data);
        switch ( requestCode ) {
            case REQUEST_CODE_RESOLUTION:
                retryConnecting();
                break;
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
        Log.i(TAG, "GoogleApiClient connected and now initializing singleton object");
        /*Intent runSensors = new Intent( getApplicationContext(), RunningActivity.class );
        startActivity( runSensors );

        GoogleAPIClientSingleton apiClientSingleton = new GoogleAPIClientSingleton();
        apiClientSingleton.setInstance(mGoogleApiClient);
        */

        GoogleAPIClientSingleton.setInstance(mGoogleApiClient);
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
        //
        // new InsertAndVerifyDataTask().execute();

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
                    result.getErrorCode(), this.getActivity(), 0, new DialogInterface.OnCancelListener() {
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
            result.startResolutionForResult( this.getActivity(), REQUEST_CODE_RESOLUTION );
        } catch ( IntentSender.SendIntentException e ) {
            Log.e( TAG, "Exception while starting resolution activity", e );
            retryConnecting();
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable( this.getActivity() );
        if ( resultCode != ConnectionResult.SUCCESS ) {
            if ( apiAvailability.isUserResolvableError( resultCode ) ) {
                apiAvailability.getErrorDialog( this.getActivity(), resultCode, PLAY_SERVICES_RESOLUTION_REQUEST )
                        .show();
            } else {
                Log.i( TAG, "This device is not supported." );
                this.getActivity().finish();
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
        cal.set(Calendar.MILLISECOND, 999);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        long startTime = cal.getTimeInMillis();

        SimpleDateFormat dateFormat = new SimpleDateFormat( DATE_FORMAT );
        Log.i( TAG, "Range Start: " + dateFormat.format( startTime ) );
        Log.i( TAG, "Range End: " + dateFormat.format( endTime ) );



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
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .aggregate( DataType.TYPE_LOCATION_SAMPLE, DataType.AGGREGATE_LOCATION_BOUNDING_BOX )


                        // Analogous to a "Group By" in SQL, defines how data should be aggregated.
                        // bucketByTime allows for a time span, whereas bucketBySession would allow
                        // bucketing by "sessions", which would need to be defined in code.
                .bucketByTime(1, TimeUnit.HOURS)
                .enableServerQueries()
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
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
                Log.i( TAG, "\tStart: " + dateFormat.format( dp.getStartTime(TimeUnit.MILLISECONDS) ) );
                Log.i( TAG, "\tEnd: " + dateFormat.format( dp.getEndTime( TimeUnit.MILLISECONDS ) ) );

                for ( Field field : dp.getDataType().getFields() ) {
                    Log.i( TAG, "\t=======Field: " + field.getName() +
                            " Value: " + dp.getValue( field ) );


                    switch ( field.getName().trim() ) {

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
                    Fitness.HistoryApi.readData( mGoogleApiClient, readRequest ).await(1, TimeUnit.MINUTES);
            Fitness.RecordingApi.subscribe(mGoogleApiClient, DataType.TYPE_ACTIVITY_SAMPLE)
                    .setResultCallback(new ResultCallback<com.google.android.gms.common.api.Status>() {
                        @Override
                        public void onResult(com.google.android.gms.common.api.Status status) {
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
            // [END read_dataset]

            printData(dataReadResult);



            return null;
        }

        private DataReadRequest queryFitnessDataForGraph() {
            // [START build_read_data_request]
            // Setting a start and end date using a range of 1 week before this moment.
            Calendar cal = Calendar.getInstance();
            Date now = new Date();
            int interval = 60  * 7;
            cal.setTime( now );
            cal.set( Calendar.HOUR_OF_DAY, 23 );
            cal.set( Calendar.MINUTE, 59 );
            cal.set( Calendar.SECOND, 59 );
            cal.set(Calendar.MILLISECOND, 999 );
            long endTime = cal.getTimeInMillis();
            cal.add( Calendar.DAY_OF_YEAR, -1 );

            long intervalDuration = (1000 * 60 * interval);
            long startTime = cal.getTimeInMillis() - intervalDuration;
            DataReadRequest readRequest = null;



            SimpleDateFormat dateFormat = new SimpleDateFormat( DATE_FORMAT );
            Log.i( TAG, "Range Start: " + dateFormat.format( startTime ) );
            Log.i( TAG, "Range End: " + dateFormat.format( endTime ) );


            DataSource ESTIMATED_STEP_DELTAS = new DataSource.Builder()
                    .setDataType( DataType.TYPE_STEP_COUNT_DELTA )
                    .setType( DataSource.TYPE_DERIVED )
                    .setStreamName( "estimated_steps" )
                    .setAppPackageName( "com.google.android.gms" )
                    .build();

            readRequest = new DataReadRequest.Builder()
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

        @Override
        protected void onPostExecute( Void aVoid ) {
            super.onPostExecute(aVoid);

            Log.d(TAG, " inside post execute");
            /*textStepCount = (TextView) getView().findViewById(R.id.textStepCount);
            //textCalorieCount = (TextView) getView().findViewById(R.id.textCalorieCount);
            textStepCount.setText( ( int ) stepsToPrint + "" );
            BigDecimal bd = new BigDecimal( distanceToPrint / 1000 );
            bd = bd.setScale( 4, RoundingMode.HALF_UP );
            //textCalorieCount.setText( bd + "" );

            Log.d(TAG, "Steps: " + stepsToPrint);*/
        }

        private void dumpSubscriptionsList() {
            // [START list_current_subscriptions]
            Fitness.RecordingApi.listSubscriptions(mGoogleApiClient, DataType.TYPE_ACTIVITY_SAMPLE)
                    // Create the callback to retrieve the list of subscriptions asynchronously.
                    .setResultCallback(new ResultCallback<ListSubscriptionsResult>() {
                        @Override
                        public void onResult(ListSubscriptionsResult listSubscriptionsResult) {
                            for (Subscription sc : listSubscriptionsResult.getSubscriptions()) {
                                DataType dt = sc.getDataType();

                                Log.i(TAG, "Active subscription for data type: " + dt.getName());
                            }
                        }
                    });
            // [END list_current_subscriptions]
        }

        /**
         * Cancel the ACTIVITY_SAMPLE subscription by calling unsubscribe on that {@link DataType}.
         */
        private void cancelSubscription() {
            final String dataTypeStr = DataType.TYPE_ACTIVITY_SAMPLE.toString();
            Log.i(TAG, "Unsubscribing from data type: " + dataTypeStr);

            // Invoke the Recording API to unsubscribe from the data type and specify a callback that
            // will check the result.
            // [START unsubscribe_from_datatype]
            Fitness.RecordingApi.unsubscribe(mGoogleApiClient, DataType.TYPE_ACTIVITY_SAMPLE)
                    .setResultCallback(new ResultCallback<com.google.android.gms.common.api.Status>() {
                        @Override
                        public void onResult(com.google.android.gms.common.api.Status status) {
                            if (status.isSuccess()) {
                                Log.i(TAG, "Successfully unsubscribed for data type: " + dataTypeStr);
                            } else {
                                // Subscription not removed
                                Log.i(TAG, "Failed to unsubscribe for data type: " + dataTypeStr);
                            }
                        }
                    });
            // [END unsubscribe_from_datatype]
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        textGo = (TextView) getView().findViewById(R.id.textGo);
        helper = new SQLiteHandlerImpl(getActivity());

        textGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!recording) {
                    recording = true;
                    t1 = System.currentTimeMillis();
                    textGo.setText("End");

                    textGo.setBackground(getResources().getDrawable(R.drawable.shape_circle_red));
                } else {
                    recording = false;
                    t2 = System.currentTimeMillis();
                    textGo.setText("GO!");
                    diff = t2 - t1;
                    Log.i(TAG, "diff: " + diff);
                    textGo.setBackground(getResources().getDrawable(R.drawable.shape_circle));
                    long diffSeconds = diff / 1000 % 60;
                    long diffMinutes = diff / (60 * 1000) % 60;
                    long diffHours = diff / (60 * 60 * 1000) % 24;
                    long diffDays = diff / (24 * 60 * 60 * 1000);

                    diff = diff / 1000 % 60;
                    Log.i(TAG, " time taken: " + diff + " seconds");

                    createInput();

                }

            }
        });

        try {
            helper.open();
            List<RpeVO> rpeVO = helper.getPastRecord();
            helper.close();
            BarChart chart = (BarChart) getView().findViewById(R.id.barChart);


            ArrayList<String> xAxis = new ArrayList<>();
            xAxis.add("Session 1");
            xAxis.add("Session 2");
            xAxis.add("Session 3");
            xAxis.add("Session 4");
            xAxis.add("Sweet Session 5");
            xAxis.add("");


            ArrayList<BarDataSet> dataSets = null;

            ArrayList<BarEntry> valueSet1 = new ArrayList<>();

            Float averageRpe = 0F;
            int i = 0;
            if(rpeVO != null ) {
                Log.i(TAG, "********SIZE: " + rpeVO.size() + " size");
                for(RpeVO obj : rpeVO){
                    BarEntry temp = new BarEntry(Float.parseFloat(obj.getRpe()), i);
                    valueSet1.add(temp);
                    //xAxis.add("Session " + (i+1));
                    averageRpe += Float.parseFloat(obj.getRpe());
                    i++;
                }
            }

            Collections.reverse(valueSet1);
            Log.i(TAG, valueSet1.toString());

            if(i>=4){
                averageRpe /= Float.parseFloat(Integer.toString(i));
                Float maxPredictionLimit = 1.5F * averageRpe;
                Float minPredictionLimit = 0.8F * averageRpe;

                BarEntry temp;
                temp = new BarEntry(maxPredictionLimit, i);

                valueSet1.add(temp);
                temp = new BarEntry(minPredictionLimit, i);
                valueSet1.add(temp);

            }



            dataSets = new ArrayList<>();
            BarDataSet barDataSet1 = new BarDataSet(valueSet1, "RPE");
            barDataSet1.setColors(new int[]{Color.rgb(0, 255, 255), Color.CYAN, Color.CYAN, Color.CYAN, Color.GREEN, Color.RED});
            dataSets.add(barDataSet1);
            BarData data = new BarData(xAxis, dataSets);
            chart.setData(data);
            chart.setDescription("Workout Sessions");
            chart.setBackgroundColor(Color.LTGRAY);

            chart.getBackground().clearColorFilter();
            chart.getPaint(BarChart.PAINT_GRID_BACKGROUND).setColor(Color.LTGRAY);
            chart.animateXY(2000, 2000);
            chart.setScaleY(1.0F);
            chart.setMaxVisibleValueCount(100);
            chart.invalidate();



        }catch(Exception e){
            e.printStackTrace();
        }

    }

    private void createInput(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Session Intensity");
        // I'm using fragment here so I'm using getView() to provide ViewGroup
        // but you can provide here any other instance of ViewGroup from your Fragment / Activity
        View viewInflated = LayoutInflater.from(getActivity()).inflate(R.layout.edit_text, (ViewGroup) getView(), false);
        // Set up the input
        final EditText input = (EditText) viewInflated.findViewById(R.id.input);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        builder.setView(viewInflated);

        // Set up the buttons
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                m_Text = input.getText().toString();

                Long rpe = Integer.parseInt(m_Text) * diff;
                Toast.makeText( getActivity(), "Rate of perceived exertion:  " + rpe, Toast.LENGTH_LONG ).show();

                //insert into sqlite
                try{

                    helper.open();
                    helper.insertSessions(rpe);
                    helper.close();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }
}
