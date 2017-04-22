package mx.com.onikom.pul;

import android.Manifest;
import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.places.Places;

/**
 * Servicio para el cálculo de la distancia en segundo plano.
 */

public class TransitionsIntentService extends IntentService implements LocationListener, GoogleApiClient.ConnectionCallbacks {
    private static final String TAG = "TransitionsService";

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Location markerLocation;
    private boolean isConnected;

    /**
     * A constructor is required, and must call the super IntentService(String)
     * constructor with a name for the worker thread.
     */
    public TransitionsIntentService() {
        super(TAG);
    }
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        if (intent != null)
            markerLocation = intent.getParcelableExtra("position");

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();

        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.build();

    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroyed");
        if (isConnected)
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        int distance = Math.round(location.distanceTo(markerLocation));
        Log.d(TAG, "Distance to marker: " + distance);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        isConnected = true;
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
    }
}
