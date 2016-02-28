package com.aiu.propel.fragments.core;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aiu.propel.R;
import com.aiu.propel.activities.core.MainActivity;
import com.aiu.propel.util.GoogleAPIClientSingleton;
import com.facebook.CallbackManager;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link StepCountFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link StepCountFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StepCountFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener  {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    public static final String TAG = "StepCount";

    private boolean mIsInResolution;
    private GoogleApiClient mGoogleApiClient;
    protected static final int REQUEST_CODE_RESOLUTION = 1;
    private static final String KEY_IN_RESOLUTION = "is_in_resolution";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    /* HISTORY API */
    private static final int REQUEST_OAUTH = 1;
    private static final String DATE_FORMAT = "yyyy.MM.dd HH:mm:ss";
    private CallbackManager callbackManager;
    private ShareDialog shareDialog;
    private boolean authInProgress = false;
    private static final String AUTH_PENDING = "auth_state_pending";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private TextView textStepCount;
    private TextView textCalorieCount;



    String textToPrint = "";
    double stepsToPrint = 0;
    double distanceToPrint = 0;
    double timeToPrint = 0;


    private GoogleApiClient mClient = null;


    private OnFragmentInteractionListener mListener;

    public StepCountFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment StepCountFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static StepCountFragment newInstance(String param1, String param2) {
        StepCountFragment fragment = new StepCountFragment();
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
                    .addScope(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                    .addScope( new Scope( Scopes.FITNESS_ACTIVITY_READ_WRITE ) )
                    .addScope(new Scope(Scopes.FITNESS_BODY_READ_WRITE))
                    .addScope( new Scope( Scopes.FITNESS_NUTRITION_READ_WRITE ) )
                    .useDefaultAccount()
                    .addConnectionCallbacks( this )
                    .addOnConnectionFailedListener( this )
                    .build();
        }
        mGoogleApiClient.connect();


    }

    private ArrayList<BarDataSet> getDataSet() {
        ArrayList<BarDataSet> dataSets = null;

        ArrayList<BarEntry> valueSet1 = new ArrayList<>();
        BarEntry v1e1 = new BarEntry(11.000f, 0); // Jan
        valueSet1.add(v1e1);
        BarEntry v1e2 = new BarEntry(4.000f, 1); // Feb
        valueSet1.add(v1e2);
        BarEntry v1e3 = new BarEntry(6.000f, 2); // Mar
        valueSet1.add(v1e3);
        BarEntry v1e4 = new BarEntry(3.000f, 3); // Apr
        valueSet1.add(v1e4);
        BarEntry v1e5 = new BarEntry(9.000f, 4); // May
        valueSet1.add(v1e5);
        BarEntry v1e6 = new BarEntry(10.000f, 5); // Jun
        valueSet1.add(v1e6);

        ArrayList<BarEntry> valueSet2 = new ArrayList<>();
        BarEntry v2e1 = new BarEntry(15.000f, 0); // Jan
        valueSet2.add(v2e1);
        BarEntry v2e2 = new BarEntry(9.000f, 1); // Feb
        valueSet2.add(v2e2);
        BarEntry v2e3 = new BarEntry(12.000f, 2); // Mar
        valueSet2.add(v2e3);
        BarEntry v2e4 = new BarEntry(6.000f, 3); // Apr
        valueSet2.add(v2e4);
        BarEntry v2e5 = new BarEntry(2.000f, 4); // May
        valueSet2.add(v2e5);
        BarEntry v2e6 = new BarEntry(8.000f, 5); // Jun
        valueSet2.add(v2e6);

        BarDataSet barDataSet1 = new BarDataSet(valueSet1, "Brand 1");
        barDataSet1.setColor(Color.rgb(0, 155, 0));
        BarDataSet barDataSet2 = new BarDataSet(valueSet2, "Brand 2");
        barDataSet2.setColors(ColorTemplate.COLORFUL_COLORS);

        dataSets = new ArrayList<>();
        dataSets.add(barDataSet1);
        dataSets.add(barDataSet2);
        return dataSets;
    }

    private ArrayList<String> getXAxisValues() {
        ArrayList<String> xAxis = new ArrayList<>();
        xAxis.add("SUN");
        xAxis.add("MON");
        xAxis.add("TUE");
        xAxis.add("WED");
        xAxis.add("THU");
        xAxis.add("FRI");
        return xAxis;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        BarChart chart = (BarChart) getView().findViewById(R.id.chart);

        BarData data = new BarData(getXAxisValues(), getDataSet());
        chart.setData(data);
        chart.setDescription("Steps");
        chart.animateXY(2000, 2000);
        chart.setBackgroundColor(Color.LTGRAY);

        chart.getBackground().clearColorFilter();
        chart.getPaint(BarChart.PAINT_GRID_BACKGROUND).setColor(Color.LTGRAY);
        chart.setScaleY(1.0F);
        chart.setMaxVisibleValueCount(100);
        chart.invalidate();


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_step_count, container, false);
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    /**
     * Saves the resolution state.
     */
    @Override
    public void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IN_RESOLUTION, mIsInResolution);
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
            default:
                callbackManager.onActivityResult( requestCode, resultCode, data );
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


            ArrayList<BarDataSet> dataSets = null;

            ArrayList<BarEntry> valueSet1 = new ArrayList<>();
            BarEntry v1e1 = new BarEntry(11.000f, 0); // Jan
            valueSet1.add(v1e1);
            BarEntry v1e2 = new BarEntry(4.000f, 1); // Feb
            valueSet1.add(v1e2);
            BarEntry v1e3 = new BarEntry(6.000f, 2); // Mar
            valueSet1.add(v1e3);
            BarEntry v1e4 = new BarEntry(3.000f, 3); // Apr
            valueSet1.add(v1e4);
            BarEntry v1e5 = new BarEntry(9.000f, 4); // May
            valueSet1.add(v1e5);
            BarEntry v1e6 = new BarEntry(10.000f, 5); // Jun
            valueSet1.add(v1e6);



            BarDataSet barDataSet1 = new BarDataSet(valueSet1, "Steps");
            barDataSet1.setColor(Color.rgb(0, 155, 0));



            dataSets = new ArrayList<>();
            dataSets.add(barDataSet1);



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
                    Log.i( TAG, "\t=======Field: " + field.getName() +
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

                            Log.i(TAG, "AALA RE");
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
                    Fitness.HistoryApi.readData( mGoogleApiClient, readRequest ).await(1, TimeUnit.MINUTES);
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
            textStepCount = (TextView) getView().findViewById(R.id.textStepCount);
            //textCalorieCount = (TextView) getView().findViewById(R.id.textCalorieCount);
            textStepCount.setText( ( int ) stepsToPrint + "" );
            BigDecimal bd = new BigDecimal( distanceToPrint / 1000 );
            bd = bd.setScale( 4, RoundingMode.HALF_UP );
            //textCalorieCount.setText( bd + "" );

            Log.d(TAG, "Steps: " + stepsToPrint);
        }
    }


    }

