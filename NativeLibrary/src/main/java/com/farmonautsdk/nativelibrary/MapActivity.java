package com.farmonautsdk.nativelibrary;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Button removeLastPoint;
    private Button removeAllPoints;
    private Button submitFarm;

    private Button myLocationButton;
    private Polygon mapPolygon;
    private Location lastKnownLocation;

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;
    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient fusedLocationProviderClient;
    private int DEFAULT_ZOOM = 19;

    private List<LatLng> latLngList = new ArrayList<>();
    private List<Marker> markerList = new ArrayList<>();
    private LatLng mapLocation;
    private String UID = "BpkwnSJdwHTjKhdm8ZWKJBO1HUn2";
    private Spinner monthsSpinner;
    private int numberOfMonths = 1;

    private static final String FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = android.Manifest.permission.ACCESS_COARSE_LOCATION;

    private String url;
    private EditText farmNameEditText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


            url = "https://us-central1-farmbase-b2f7e.cloudfunctions.net/submitField";

            getLocationPermission();

        removeLastPoint = findViewById(R.id.remove_last_point);
        removeAllPoints = findViewById(R.id.remove_all_points);
        submitFarm = findViewById(R.id.submit_farm);
        monthsSpinner = findViewById(R.id.months_spinner);
        farmNameEditText = findViewById(R.id.farm_name);
        myLocationButton = findViewById(R.id.gps_location);

        monthsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String nMonths = monthsSpinner.getSelectedItem().toString();
                nMonths = nMonths.replace(" ", "");
                nMonths = nMonths.replace("Months", "");
                nMonths = nMonths.replace("Month", "");
                 numberOfMonths = Integer.parseInt(nMonths);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        myLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myLocation();
            }
        });

        removeAllPoints.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               resetMap();
            }
        });

        removeLastPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println(latLngList);
                System.out.println(latLngList.size());
                if(latLngList.size() > 0){
                    int currentIndex = latLngList.size()- 1;
                    latLngList.remove(currentIndex);
                    markerList.get(currentIndex).remove();
                    markerList.remove(currentIndex);
                }

                updatePolygon();

            }
        });

        submitFarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(latLngList.size()>2){
                    JSONObject farmObject = new JSONObject();
                    showToast("Submitting. Please Wait.");
                    try {
                        String farmName = farmNameEditText.getText().toString();
                        if(farmName.isEmpty()){
                            farmName = "-";
                        }


                        farmObject.put("Points", makePointsArray());
                        farmObject.put("PaymentType", Integer.toString(numberOfMonths));
                        farmObject.put("UID", UID);
                        farmObject.put("CropCode", "999");
                        farmObject.put("FieldName", farmName);
                       // String jsonString = farmObject.toString();
                       submitFarmHTTP(farmObject);
                    } catch (JSONException e) {
                        showToast(e.getMessage());
                        throw new RuntimeException(e);

                    }
                }else{
                    showToast("Please select at least 3 points");
                }

            }
        });
    }


    public void showToast(String toastText) {
        Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();
    }



    private void submitFarmHTTP(JSONObject Query_text) {

        //out.println("yes we at line 45");

        OkHttpClient httpClient = new OkHttpClient();


        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, Query_text.toString());

         // Replace with your API endpoint URL
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();


        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
            @Override public void onResponse(Call call, Response response){
                ResponseBody responseBody = response.body();
                String resp = "";
                if (!response.isSuccessful()) {


                }else {
                    try {
                        resp = responseBody.string();
                    } catch (IOException e) {
                        resp = "There is a problem";

                    }
                }

                runOnUiThread(responseRunnable(resp));


            }
        });

//            Response response = httpClient.newCall(request).execute();

//            if (response.isSuccessful()) {
//                String responseBody = response.body().string();
//                // Handle the successful response here
//                // responseBody contains the response data as a string
//            } else {
//                // Handle the error response here
//                // You can get the error code and message using response.code() and response.message()
//            }


//        HttpUrl.Builder httpBuider =
//                HttpUrl.parse(url).newBuilder();
//
//        httpBuider.addQueryParameter("text", Query_text.toString());
//
//        okhttp3.Request request = new Request.Builder().
//                url(httpBuider.build()).build();




    }

    private void resetMap(){
        latLngList.clear();
        markerList.clear();
        mMap.clear();
        mapPolygon.remove();
    }
    private Runnable responseRunnable(final String responseStr) {
        Runnable resRunnable = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            public void run() {
                try{
                    //showToast(responseStr);
                    JSONObject responseObj = new JSONObject(responseStr);
                    showToast("Farm Submitted Successfully!");
                    resetMap();

                } catch (JSONException e) {
                    showToast(responseStr);
                    throw new RuntimeException(e);
                }



            }
        };
        return resRunnable;
    }



    public JSONObject makePointsArray() throws JSONException {
//        List<List<List<Double>>> mainPointsArray = new ArrayList<>();
//        List<List<Double>> allPointsArray = new ArrayList<>();
        JSONObject pointsObj = new JSONObject();
        for(int i =0; i < latLngList.size(); i++){
            JSONObject singlePointObj = new JSONObject();

            //List<Double> singlePoint = new ArrayList<>();
            LatLng tempLatLng = latLngList.get(i);
//            singlePoint.add(tempLatLng.latitude);
//            singlePoint.add(tempLatLng.longitude);
//            System.out.println(singlePoint);
//            allPointsArray.add(singlePoint);
//            System.out.println(allPointsArray);
            singlePointObj.put("Latitude", tempLatLng.latitude);
            singlePointObj.put("Longitude", tempLatLng.longitude);
            if(i == 0){
                pointsObj.put("a", singlePointObj);
            }else{
                pointsObj.put(("P_"+i), singlePointObj);
            }
        }

        //mainPointsArray.add(allPointsArray);
        System.out.println(pointsObj);
        return pointsObj;
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     *
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull LatLng latLng) {
              //  latLngList.add(latLng);
                System.out.println(latLng);
                mapLocation = latLng;
                addMarkersInArray();

                // Add polygons to indicate areas on the map.
                updatePolygon();

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        if (requestCode
                == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        getDeviceLocation();
       // updateLocationUI();
    }

    public void updateLocationUI(){

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mapLocation,19));

    }

    private void myLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();

                            if (lastKnownLocation != null) {
                                mapLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                                addMarkersInArray();
                                updatePolygon();

                            }
                        } else {
//
                            getDeviceLocation();
                        }

                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    private void getLocationPermission(){
        String [] permissions = {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
        };

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                locationPermissionGranted = true;


            }
            getDeviceLocation();

        }else{
            ActivityCompat.requestPermissions(this,
                    permissions,1);

        }
    }
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();

                            if (lastKnownLocation != null) {
                                mapLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        mapLocation, DEFAULT_ZOOM));
                            }
                        } else {
//
                        }

                    }
                });
            }else{

            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }


    private void addMarkersInArray(){
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
//                mapLocation, DEFAULT_ZOOM));

        latLngList.add(mapLocation);

        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(mapLocation)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker1)));
        markerList.add(marker);

    }
    public void updatePolygon(){
        if(latLngList.size() > 0){
            if(mapPolygon != null){
                mapPolygon.remove();
            }



            mapPolygon = mMap.addPolygon(new PolygonOptions()
                    .clickable(true)
                    .addAll(latLngList));
        }


    }

}
