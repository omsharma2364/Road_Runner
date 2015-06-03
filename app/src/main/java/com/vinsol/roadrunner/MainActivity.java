package com.vinsol.roadrunner;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    //UI elements
    ListView parent;
    TextView lastDelivery;
    ProgressBar refreshProgress;
    Button refresh;
    ProgressDialog gettingToken;

    //constatns
    public final static String BASE_URL = "http://1146a6d6.ngrok.com/";
    public final static String TOKEN_TAG = "";
    public final static String PACKAGE_TAG = "";
    public final static String ADDRESS_TAG = "";
    public final static String LATITUDE_TAG = "";
    public final static String LONGITUDE_TAG = "";
    public final static String RESPONSE_TAG = "";
    public final static String TOKEN_PAGE = "drivers/register_token";
    public final static String MARK_DONE_PAGE = "";
    public final static String GET_DELIVERIES_PAGE = "";
    public final static String LOCATION_PAGE = "";
    public final static int SUCCESS_RESPONSE = 200;

    //variables used
    int responseCode = 0;
    DB_Helper db_helper;
    MyAdapter adapter;
    ArrayList<String> listAddress, listPackageNo;
    String TOKEN;
    GoogleApiClient mGoogleApiClient;
    LocationRequest locationRequest;
    SendLocationAsyncTask sendLocationAsyncTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
            sendLocation();
            getDeliveriesToBeDone();
        }
    }

    public void initUI() {
        db_helper = new DB_Helper(this);
        parent = (ListView) findViewById(R.id.list);
        lastDelivery = (TextView) findViewById(R.id.footer);
        refreshProgress = (ProgressBar) findViewById(R.id.progressBar);
        refresh = (Button) findViewById(R.id.refresh);
        gettingToken = new ProgressDialog(this);
        gettingToken.setTitle("Getting Token...");
        gettingToken.setMessage("Please Wait.");
        gettingToken.setIndeterminate(true);
        refreshProgress.setVisibility(View.GONE);
    }

    public void onRefresh(View v) {

        getDeliveriesToBeDone();

    }

    public void onDone(View v) {
        Toast.makeText(getApplicationContext(), "Done!!", Toast.LENGTH_SHORT).show();
        View parentRow = (View) v.getParent();
        parentRow.setBackgroundColor(Color.RED);
        markDeliveryDone(parentRow);
    }

    @Override
    public void onConnected(Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(3000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (latLng != null) {
            String lat = String.valueOf(latLng.latitude);
            String lng = String.valueOf(latLng.longitude);
            if (sendLocationAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                sendLocationAsyncTask.cancel(true);
                sendLocationAsyncTask = null;
            }
            sendLocationAsyncTask = new SendLocationAsyncTask();
            sendLocationAsyncTask.execute(lat, lng);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(getApplicationContext(),"GPS COnnection Lost!!",Toast.LENGTH_LONG).show();
        mGoogleApiClient.reconnect();
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
        responseCode = checkStatus(BASE_URL + TOKEN_PAGE);
        if (responseCode == SUCCESS_RESPONSE)
            new GetTokenAsyncTask().execute();
        else
            Toast.makeText(getApplicationContext(), "Connection Failed with Error Code: " + responseCode, Toast.LENGTH_LONG).show();
    }

    public void getDeliveriesToBeDone() {

        //check if url working
        responseCode = checkStatus(BASE_URL + GET_DELIVERIES_PAGE);
        if (responseCode == SUCCESS_RESPONSE)
            new GetDeliveryListAsyncTask().execute(TOKEN);
        else
            Toast.makeText(getApplicationContext(), "Connection Failed with Error Code: " + responseCode, Toast.LENGTH_LONG).show();


    }

    public void markDeliveryDone(View toBeMarked) {

        //check if url working
        responseCode = checkStatus(BASE_URL + MARK_DONE_PAGE);
        if (responseCode == SUCCESS_RESPONSE)
            new MarkDeliveryAsyncTask().execute(toBeMarked);
        else
            Toast.makeText(getApplicationContext(), "Connection Failed with Error Code: " + responseCode, Toast.LENGTH_LONG).show();
    }

    public void sendLocation() {
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        mGoogleApiClient.connect();
    }

    public class MarkDeliveryAsyncTask extends AsyncTask<View, Void, String> {
        View row;
        String markedAddress;

        @Override
        protected void onPostExecute(String response) {

            if (response.equals("")) {
                //if success
                //remove the list item
                ListView listView = (ListView) row.getParent();
                int position = listView.getPositionForView(row);
                listAddress.remove(position);
                listPackageNo.remove(position);
                adapter.notifyDataSetChanged();
                lastDelivery.setText(markedAddress);
                lastDelivery.setBackgroundColor(Color.DKGRAY);

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
        }


        @Override
        protected String doInBackground(View... delivery) {
            row = delivery[0];
            String packageNum, response = null;
            URL url = null;
            JSONObject jsonObjectOut = null;
            HttpURLConnection httpURLConnection = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;
            //sending mark delivery request
            RelativeLayout parentRow = (RelativeLayout) delivery[0];
            TextView packageNo = (TextView) parentRow.getChildAt(0);
            TextView address = (TextView) parentRow.getChildAt(1);
            packageNum = packageNo.getText().toString();
            markedAddress = address.getText().toString();

            try {
                url = new URL(BASE_URL + MARK_DONE_PAGE);
            } catch (MalformedURLException exception) {
                exception.printStackTrace();
            }
            try {
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestProperty("Content-Type", "application/json");
                httpURLConnection.setRequestProperty("Accept", "application/json");
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);

                dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());

                JSONObject jsonObject = new JSONObject();
                jsonObject.put(TOKEN_TAG, TOKEN);
                jsonObject.put(PACKAGE_TAG, packageNum);
                dataOutputStream.writeChars(jsonObject.toString());


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
            db_helper.insertToken(params[0].toString());
            TOKEN = params[0].toString();
            sendLocation();
            getDeliveriesToBeDone();
            //remove the progress dialog
            gettingToken.dismiss();
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
                    response[0] = jsonObject.getInt(TOKEN_TAG);
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

    public class GetDeliveryListAsyncTask extends AsyncTask<String, Void, JSONArray> {

        @Override
        protected JSONArray doInBackground(String... strings) {
            URL url = null;
            JSONArray jsonArray = null;
            try {
                url = new URL(BASE_URL + GET_DELIVERIES_PAGE);
            } catch (MalformedURLException exception) {
                exception.printStackTrace();
            }
            HttpURLConnection httpURLConnection = null;
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;
            try {
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestProperty("Content-Type", "application/json");
                httpURLConnection.setRequestProperty("Accept", "application/json");
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);

                dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());

                JSONObject jsonObject = new JSONObject();
                jsonObject.put(TOKEN_TAG, TOKEN);
                dataOutputStream.writeChars(jsonObject.toString());

                dataInputStream = new DataInputStream(httpURLConnection.getInputStream());
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(dataInputStream, "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();

                String inputStr;
                while ((inputStr = streamReader.readLine()) != null)
                    responseStrBuilder.append(inputStr);

                try {
                    jsonArray = new JSONArray(responseStrBuilder.toString());
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
            return jsonArray;
        }

        @Override
        protected void onPostExecute(JSONArray jsonArray) {
            //update the list view

            refreshProgress.setVisibility(View.GONE);
            refresh.setVisibility(View.VISIBLE);

            int deliveryCount = jsonArray.length();
            JSONObject deliveryObject;
            listPackageNo = null;
            listPackageNo = new ArrayList<>();
            listAddress = new ArrayList<>();
            try {
                for (int i = 0; i < deliveryCount; i++) {
                    deliveryObject = jsonArray.getJSONObject(i);
                    listAddress.add(deliveryObject.getString(ADDRESS_TAG));
                    listPackageNo.add(deliveryObject.getString(PACKAGE_TAG));
                    adapter.notifyDataSetChanged();
                }
            } catch (JSONException e) {
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

    public class SendLocationAsyncTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            URL url = null;
            JSONObject jsonObjectOut = null;
            HttpURLConnection httpURLConnection = null;
            //DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;
            //sending lat,lng
            try {
                url = new URL(BASE_URL + LOCATION_PAGE);
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

                dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());

                JSONObject jsonObject = new JSONObject();
                jsonObject.put(TOKEN_TAG, TOKEN);
                jsonObject.put(LATITUDE_TAG, strings[0]);
                jsonObject.put(LONGITUDE_TAG, strings[1]);
                dataOutputStream.writeChars(jsonObject.toString());


            } catch (Exception exception) {
                exception.printStackTrace();
            } finally {
                if (dataOutputStream != null) {
                    try {
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

    public int checkStatus(String Url) {
        int statusCode = 0;
        URL url = null;
        HttpURLConnection httpURLConnection = null;
        try {
            url = new URL(Url);
        } catch (MalformedURLException exception) {
            exception.printStackTrace();
        }
        try {
            httpURLConnection = (HttpURLConnection) url.openConnection();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId()==R.id.requestToken){
            getToken();
        }
        return true;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater=getMenuInflater();
        menuInflater.inflate(R.menu.menu_main,menu);
        return super.onCreateOptionsMenu(menu);
    }
}
