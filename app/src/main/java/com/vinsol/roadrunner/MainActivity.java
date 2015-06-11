package com.vinsol.roadrunner;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements  GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    ListView parent;
    TextView lastDelivery;
    TextView tokenMain;
    ProgressBar refreshProgress;
    Button refresh;
    ProgressDialog gettingToken;
    Toolbar toolbar;
    RelativeLayout lastClicked;
    Button lastClick;

    public final static String BASE_URL = "http://139.162.17.105:3000/";//"http://1146a6d6.ngrok.com/";
    public final static String TOKEN_TAG = "driver_token";
    public final static String RECIEVE_TOKEN_TAG = "token";
    public final static String PACKAGE_TAG = "package_number";
    public final static String ADDRESS_TAG = "address";
    public final static String LATITUDE_TAG = "latitude";
    public final static String LONGITUDE_TAG = "longitude";
    public final static String ID_TAG = "id";
    public final static String RESPONSE_TAG = "message";
    public final static String TOKEN_PAGE = "drivers/register_token";
    public final static String MARK_DONE_PAGE = "drivers/mark_task_as_done";
    public final static String GET_DELIVERIES_PAGE = "drivers/tasks/";
    public final static String LOCATION_PAGE = "drivers/update_coordinates";
    public final static int SUCCESS_RESPONSE = 200;
    public static String latitude, longitude;



    LocationRequest mLocationRequest;
    Location mCurrentLocation;
    int responseCode=200,responseViaAsync;
    DB_Helper db_helper;
    MyAdapter adapter;
    SendLocationAsyncTask sendLocationAsyncTask;
    ArrayList<String> listAddress, listPackageNo,listTaskId;
    String TOKEN;
    boolean viaGetToken,viaGetDeliveries,viaMarkDone;
    GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viaGetToken=false;
        viaGetDeliveries=false;
        viaMarkDone=false;

        initUI();
        listAddress = new ArrayList<String>();
        listPackageNo = new ArrayList<String>();
        adapter = new MyAdapter(this);
        parent.setAdapter(adapter);
        if (db_helper.getCount() == 0) {
            Toast.makeText(getApplicationContext(), "No token Found", Toast.LENGTH_SHORT).show();
            getToken();
        } else {
            TOKEN = db_helper.getToken();
            tokenMain.setText(TOKEN);
            Toast.makeText(getApplicationContext(), TOKEN, Toast.LENGTH_SHORT).show();
            buildGoogleApiClient();
            getDeliveriesToBeDone();
        }


    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();

    }

    public void initUI() {
        db_helper = new DB_Helper(this);
        toolbar=(Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        parent = (ListView) findViewById(R.id.list);
        lastDelivery = (TextView) findViewById(R.id.footer);
        tokenMain=(TextView)findViewById(R.id.texttoken);
        refreshProgress = (ProgressBar) findViewById(R.id.progressBar);
        refresh = (Button) findViewById(R.id.refresh);
        gettingToken = new ProgressDialog(this);
        gettingToken.setTitle("Getting Token...");
        gettingToken.setMessage("Please Wait.");
        gettingToken.setIndeterminate(true);
        refreshProgress.setVisibility(View.GONE);
    }

    public void onRefresh(View v) {

        listPackageNo=new ArrayList<>();
        listTaskId=new ArrayList<>();
        listAddress=new ArrayList<>();
        adapter.notifyDataSetChanged();
        getDeliveriesToBeDone();

    }

    public void onDone(View v) {
        View parentRow = (View) v.getParent();
        markDeliveryDone(parentRow);
    }

    public class MyAdapter extends BaseAdapter {

        Context con;

        public MyAdapter(Context context) {
            this.con = context;
        }

        @Override
        public int getCount() {
            return listAddress.size();
        }

        @Override
        public Object getItem(int i) {
            return listAddress.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View v;

            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) con.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.custom_list_item, viewGroup, false);
            } else {
                v = view;
            }
            TextView address = (TextView) v.findViewById(R.id.location);
            TextView packageNo = (TextView) v.findViewById(R.id.packageNo);
            address.setText("" + listAddress.get(i));
            packageNo.setText("" + listPackageNo.get(i));
            return v;
        }
    }

    public void getToken() {

        //check if url working
        //responseCode=checkStatus(BASE_URL + TOKEN_PAGE);
        if (responseCode == SUCCESS_RESPONSE)
            new GetTokenAsyncTask().execute();
        else
            Toast.makeText(getApplicationContext(), "Connection Failed with Error Code: " + responseCode, Toast.LENGTH_LONG).show();


    }

    public void getDeliveriesToBeDone() {

        //check if url working
        //responseCode=checkStatus(BASE_URL);
        if(viaMarkDone) {
            lastClicked.setBackgroundColor(Color.WHITE);
            lastClick.setVisibility(View.VISIBLE);
        }
        if (responseCode == SUCCESS_RESPONSE)
            new GetDeliveryListAsyncTask().execute(TOKEN);
        else
            Toast.makeText(getApplicationContext(), "Connection Failed with Error Code: " + responseCode, Toast.LENGTH_LONG).show();
    }

    public void markDeliveryDone(View toBeMarked) {

        //check if url working
        //responseCode=checkStatus(BASE_URL);
        toBeMarked.setBackgroundColor(Color.RED);
        if (responseCode == SUCCESS_RESPONSE)
            new MarkDeliveryAsyncTask().execute(toBeMarked);
        else
            Toast.makeText(getApplicationContext(), "Connection Failed with Error Code: " + responseCode, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnected(Bundle bundle) {

        requestLocationUpdates();
        startLocationUpdates();

    }

    protected void startLocationUpdates() {

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    private void requestLocationUpdates() {


        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(3000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {


    }

    @Override
    public void onLocationChanged(Location location) {

        mCurrentLocation = location;
        latitude = String.valueOf(mCurrentLocation.getLatitude());
        longitude = String.valueOf(mCurrentLocation.getLongitude());
        //Toast.makeText(MainActivity.this, latitude+longitude, Toast.LENGTH_SHORT).show();
        if (latitude != null) {
            sendLocationAsyncTask = new SendLocationAsyncTask();
            sendLocationAsyncTask.execute(latitude, longitude);
        }
    }

    public class MarkDeliveryAsyncTask extends AsyncTask<View, Void, String> {
        View row;
        String markedAddress,task_id;

        @Override
        protected void onPostExecute(String response) {
            viaMarkDone=true;

            if (response.equals("true")) {
                //if success
                //remove the list item
                ListView listView = (ListView) row.getParent();
                int position = listView.getPositionForView(row);
                row.setBackgroundColor(Color.GREEN);

                RelativeLayout temp=(RelativeLayout)row;
                lastClicked=temp;
                Button clicked=(Button)temp.getChildAt(2);
                lastClick=clicked;
                clicked.setVisibility(View.GONE);
                lastDelivery.setText(markedAddress);

            } else {
                //if failure
                //update the appearance of list item
                lastDelivery.setBackgroundColor(Color.RED);
                lastDelivery.setText("Try Again!!");
            }
        }

        @Override
        protected void onProgressUpdate(Void... values) {

            //update the list item appearance to be sent to server
            row.setBackgroundColor(Color.RED);
        }


        @Override
        protected String doInBackground(View... delivery) {
            row = delivery[0];
            String response = null;
            URL url = null;
            JSONObject jsonObjectOut = null;
            HttpURLConnection httpURLConnection = null;
            DataInputStream dataInputStream = null;
            OutputStream dataOutputStream = null;
            //sending mark delivery request
            ListView listView = (ListView) row.getParent();
            int position = listView.getPositionForView(row);
            RelativeLayout parentRow = (RelativeLayout) delivery[0];
            TextView address = (TextView) parentRow.getChildAt(1);
            markedAddress = address.getText().toString();
            task_id=listTaskId.get(position);


            try {
                url = new URL(BASE_URL + MARK_DONE_PAGE);
            } catch (MalformedURLException exception) {
                exception.printStackTrace();
            }
            try {
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestProperty("Content-Type", "application/json");
                httpURLConnection.setRequestProperty("Accept", "application/json");
                httpURLConnection.setRequestMethod("PUT");
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);

                dataOutputStream = new BufferedOutputStream(httpURLConnection.getOutputStream());

                JSONObject jsonObject = new JSONObject();
                jsonObject.put(TOKEN_TAG, TOKEN);
                jsonObject.put("task_id", task_id);

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(dataOutputStream, "UTF-8"));
                writer.write(jsonObject.toString());
                writer.close();

                dataInputStream = new DataInputStream(httpURLConnection.getInputStream());
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(dataInputStream, "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();

                String inputStr;
                while ((inputStr = streamReader.readLine()) != null)
                    responseStrBuilder.append(inputStr);

                try {
                    jsonObjectOut = new JSONObject(responseStrBuilder.toString());
                    response = jsonObjectOut.getString(RESPONSE_TAG);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } catch (Exception exception) {
                exception.printStackTrace();
            } finally {
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                        dataOutputStream.flush();
                        dataOutputStream.close();
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                }
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            }
            return response;
        }
    }

    public class GetTokenAsyncTask extends AsyncTask<Void, Void, Integer[]> {
        @Override
        protected void onPostExecute(Integer... params) {
            try {
                db_helper.insertToken(params[0].toString());
                TOKEN = params[0].toString();
                tokenMain.setText(TOKEN);
                sendLocation();
                getDeliveriesToBeDone();
            }
            catch (Exception e){
                Toast.makeText(getApplicationContext(),"Error in Getting Token!!",Toast.LENGTH_LONG).show();
            }
            //remove the progress dialog
            gettingToken.dismiss();
        }
        public void sendLocation() {
            buildGoogleApiClient();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            //while fetching the token
        }

        @Override
        protected Integer[] doInBackground(Void... voids) {
            Integer response[] = new Integer[2];

            //get token request

            URL url = null;

            HttpURLConnection httpURLConnection = null;
            DataInputStream dataInputStream = null;
            try {
                url = new URL(BASE_URL + TOKEN_PAGE);
            } catch (MalformedURLException exception) {
                exception.printStackTrace();
            }
            try {
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestProperty("Content-Type", "application/json");
                httpURLConnection.setRequestProperty("Accept", "application/json");
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setDoInput(true);
                dataInputStream = new DataInputStream(httpURLConnection.getInputStream());
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(dataInputStream, "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();

                String inputStr;
                while ((inputStr = streamReader.readLine()) != null)
                    responseStrBuilder.append(inputStr);

                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(responseStrBuilder.toString());
                    response[0] = jsonObject.getInt(RECIEVE_TOKEN_TAG);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } catch (IOException exception) {
                exception.printStackTrace();
            } finally {
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                }
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            }
            return response;
        }


        @Override
        protected void onPreExecute() {
            //show the progress dialog
            gettingToken.show();
        }
    }

    public class SendLocationAsyncTask extends AsyncTask<String, Void, Void> {


        @Override
        protected Void doInBackground(String... strings) {

            URL url = null;
            OutputStream dataOutputStream = null;
            try {
                url=new URL(BASE_URL+LOCATION_PAGE);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            HttpURLConnection httpURLConnection = null;
            DataInputStream dataInputStream = null;
            try {
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestProperty("Content-Type", "application/json");
                httpURLConnection.setRequestProperty("Accept", "application/json");
                httpURLConnection.setRequestMethod("PUT");
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);

                dataOutputStream=new BufferedOutputStream(httpURLConnection.getOutputStream());

                JSONObject jsonObject = new JSONObject();
                jsonObject.put(TOKEN_TAG, TOKEN);
                jsonObject.put(LATITUDE_TAG, strings[0]);
                jsonObject.put(LONGITUDE_TAG, strings[1]);

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(dataOutputStream, "UTF-8"));
                writer.write(jsonObject.toString());

                writer.close();
                dataInputStream = new DataInputStream(httpURLConnection.getInputStream());
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(dataInputStream,"UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();

                String inputStr;
                while ((inputStr = streamReader.readLine()) != null)
                    responseStrBuilder.append(inputStr);

            } catch (Exception exception) {
                exception.printStackTrace();
            } finally {
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                        dataOutputStream.flush();
                        dataOutputStream.close();
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                }
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            }
            return null;
        }
    }

    public class GetDeliveryListAsyncTask extends AsyncTask<String, Void, JSONArray> {

        @Override
        protected JSONArray doInBackground(String... strings)
        {
            URL url = null;
            JSONObject jsonArray = null;
            JSONArray jsonArray1=null;
            try {
                url=new URL(BASE_URL+GET_DELIVERIES_PAGE+"?"+TOKEN_TAG+"="+TOKEN);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            HttpURLConnection httpURLConnection = null;
            DataInputStream dataInputStream = null;
            try {
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestProperty("Content-Type", "application/json");
                httpURLConnection.setRequestProperty("Accept", "application/json");
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setDoInput(true);

                dataInputStream = new DataInputStream(httpURLConnection.getInputStream());
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(dataInputStream,"UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();

                String inputStr;
                while ((inputStr = streamReader.readLine()) != null)
                    responseStrBuilder.append(inputStr);

                try {
                    jsonArray = new JSONObject(responseStrBuilder.toString());
                    jsonArray1=jsonArray.getJSONArray("tasks");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (Exception exception) {
                exception.printStackTrace();
            } finally {
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                }
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            }
            return jsonArray1;
        }

        @Override
        protected void onPostExecute(JSONArray jsonArray) {
            //update the list view

            refreshProgress.setVisibility(View.GONE);
            refresh.setVisibility(View.VISIBLE);

            JSONObject deliveryObject;
            listPackageNo = null;
            listPackageNo = new ArrayList<>();
            listAddress = new ArrayList<>();
            listTaskId=new ArrayList<>();
            try {

                int deliveryCount = jsonArray.length();
                for (int i = 0; i < deliveryCount; i++) {
                    deliveryObject = jsonArray.getJSONObject(i);
                    listAddress.add(deliveryObject.getString(ADDRESS_TAG));
                    listPackageNo.add(deliveryObject.getString(PACKAGE_TAG));
                    listTaskId.add(deliveryObject.getString(ID_TAG));
                    adapter.notifyDataSetChanged();
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(),"No Tasks Found!!",Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }


        @Override
        protected void onPreExecute() {
            //show small indication of request being processed
            refresh.setVisibility(View.GONE);
            refreshProgress.setVisibility(View.VISIBLE);

        }
    }

    public class GetStatusAsyncTask extends AsyncTask<String,Void,Integer>{

        int statusCode;
        @Override
        protected void onPostExecute(Integer integer) {
            responseViaAsync=integer;
            if (integer != SUCCESS_RESPONSE)
                Toast.makeText(getApplicationContext(), "Connection Failed with Error Code: " + responseCode, Toast.LENGTH_LONG).show();
        }

        @Override
        protected Integer doInBackground(String... strings) {
            URL url = null;
            HttpURLConnection httpURLConnection = null;
            try {
                url = new URL(strings[0]);
            } catch (MalformedURLException exception) {
                exception.printStackTrace();
            }
            try {
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setConnectTimeout(2000);
                httpURLConnection.setReadTimeout(2000);
                httpURLConnection.setRequestMethod("HEAD");
                httpURLConnection.connect();
                statusCode = httpURLConnection.getResponseCode();
            } catch (Exception exception) {
                exception.printStackTrace();
            } finally {

                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }

            }

            return statusCode;
        }
    }

    public int checkStatus(String Url) {
        new GetStatusAsyncTask().execute(Url);
        return responseViaAsync;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.requestToken) {

            listAddress=new ArrayList<>();
            listTaskId=new ArrayList<>();
            listPackageNo=new ArrayList<>();
            adapter.notifyDataSetChanged();
            db_helper.deleteContent();
            getToken();
        }
        return true;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }
}





