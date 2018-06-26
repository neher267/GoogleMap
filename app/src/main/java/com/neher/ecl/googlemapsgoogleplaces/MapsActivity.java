package com.neher.ecl.googlemapsgoogleplaces;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMarkerDragListener{

    private GoogleMap mMap;
    private static final String TAG = "MapsActivity";
    public static final int ERROR_DIALOG_REQUEST = 404;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 500;

    public boolean mLocationPermissionGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private static final float DEFAULT_ZOOM = 15f;
    private PlaceAutocompleteAdapter mPlaceAutocompleteAdapter;

    private AutoCompleteTextView mSearchText;
    private ImageView mMyLocationView;
    protected GeoDataClient mGeoDataClient;

    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(new LatLng(20.797425846129503, 87.86243451562495), new LatLng(26.745824752661083, 92.70740521874995));


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(TAG, "onCreate: Android Version is M or later");
            getLocationPermission();
        } else {
            Log.d(TAG, "onCreate: Android version is below M");
            initMap();
            init();
        }
    }

    private void init(){
        Log.d(TAG, "init: initializing");
        mSearchText = findViewById(R.id.input_search);
        mMyLocationView = findViewById(R.id.my_location);

        mGeoDataClient = Places.getGeoDataClient(this);

        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || event.getAction() == KeyEvent.ACTION_DOWN
                        || event.getAction() == KeyEvent.KEYCODE_ENTER){

                    //executing our method for searching

                    geoLocate("");

                }
                return false;
            }
        });

        mSearchText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchText.setText("");
            }
        });

        mPlaceAutocompleteAdapter =  new PlaceAutocompleteAdapter(this, mGeoDataClient, LAT_LNG_BOUNDS, null);

        mSearchText.setAdapter(mPlaceAutocompleteAdapter);
        mSearchText.setOnItemClickListener(mAutocompleteClickListener);

        mMyLocationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDeviceLocation();
            }
        });

    }

    private void initMap() {
        Log.d(TAG, "initMap: initialize the map");
        if (isServicesOk()) {
            Toast.makeText(MapsActivity.this, "Map is Ready", Toast.LENGTH_LONG).show();
            Log.d(TAG, "initMap: Map is ready");
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }
    }

    private void geoLocate(@Nullable String searchString){
        if (mMap != null){
            mMap.clear();
        }
        Log.d(TAG, "geoLocate: geolocating");
        if (searchString == null){
            searchString = mSearchText.getText().toString();
        }
        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> list = new ArrayList<>();

        try{
            list = geocoder.getFromLocationName(searchString, 1);
        }catch (IOException e){
            Log.d(TAG, "geoLocate: exception: " + e.getMessage());
        }

        if (list.size()>0){
            Address address = list.get(0);
            Log.d(TAG, "geoLocate: Address: "+address.toString());
            if (address.hasLatitude() && address.hasLongitude()){
                Log.d(TAG, "geoLocate: has latitude and longitude");
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                moveCamera(latLng, DEFAULT_ZOOM, address.getAddressLine(0));
            }
            else {
                Toast.makeText(MapsActivity.this, "No Location Found", Toast.LENGTH_LONG).toString();
            }
        }

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (mLocationPermissionGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            getDeviceLocation();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onMapReady: location permission is not granted");
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.setOnMarkerClickListener(this);
            mMap.setOnMarkerDragListener(this);
        }
    }

    private void getDeviceLocation(){
        /*if (mMap != null){
            mMap.clear();
        }*/
        Log.d(TAG, "getDeviceLocation: getting the device current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try{
            if (mLocationPermissionGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()){
                            Log.d(TAG, "onComplete: Find Location");
                            Location currentLocation = (Location) task.getResult();
                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "My Location");

                        }
                        else{
                            Log.d(TAG, "onComplete: Could'n find location");
                            Toast.makeText(MapsActivity.this, "Unable to get current location", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }catch (SecurityException e){
            Log.d(TAG, "getDeviceLocation: SecurityException: "+e.getMessage());
        }
    }

    private void moveCamera(LatLng latLng, float zoom, String title){
        Log.d(TAG, "moveCamera: moving camera: lat: "+latLng.latitude+", lng: "+latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        if(!title.equals("My Location")){
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .title(title);

            markerOptions.draggable(true);

            mMap.addMarker(markerOptions);
        }
    }

    public boolean isServicesOk(){
        Log.d(TAG, "isServicesOk: Checking google services version");
         int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MapsActivity.this);

         if (ConnectionResult.SUCCESS == available){
             // Everything is fine and user can make map request
             Log.d(TAG, "isServicesOk: Google play services is working");
             return true;
         }
         else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            // An error occured but we can resolve it
             Log.d(TAG, "isServicesOk: An error occured but we can fix it");

             Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MapsActivity.this, available, ERROR_DIALOG_REQUEST);
             dialog.show();
         }
         else {
             Toast.makeText(this, "You can't make maps request", Toast.LENGTH_LONG).show();
         }

         return false;
    }

    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: getting Location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if(ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED){
            // Permission Granted
            Log.d(TAG, "getLocationPermission: Permission Granted");
            initMap();

        }
        else{
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if (grantResults.length>0){
                    for (int i = 0; i<grantResults.length; i++){
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            Log.d(TAG, "onRequestPermissionsResult: Permission Denied");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: Permission Granted");
                    mLocationPermissionGranted = true;
                    //initialize our map
                    initMap();
                }
            }
        }
    }

    private AdapterView.OnItemClickListener mAutocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final AutocompletePrediction item = mPlaceAutocompleteAdapter.getItem(position);
            final String placeId = item.getPlaceId();
            final CharSequence primaryText = item.getPrimaryText(null);

            geoLocate(primaryText.toString());

            Log.d(TAG, "Autocomplete item selected: " + primaryText);
        }
    };


    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.setDraggable(true);
        return false;
    }
}
