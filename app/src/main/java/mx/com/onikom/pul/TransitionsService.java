package mx.com.onikom.pul;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.GoogleMap;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Servicio para el cálculo de la distancia en segundo plano.
 */

public class TransitionsService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks {
    private static final String TAG = "TransitionsService";

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Location markerLocation;
    private int lastDistance;
    private boolean isConnected;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();

        locationRequest = new LocationRequest();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            markerLocation = intent.getParcelableExtra("position");
            lastDistance = intent.getIntExtra("last_distance", 500);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroyed");
        if (isConnected) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        int actualDistance = Math.round(location.distanceTo(markerLocation));
        Log.d(TAG, "Distance to marker: last: " + lastDistance + ", actual: " + actualDistance);
        if ((lastDistance > 200 && actualDistance <= 200)
                || (lastDistance <= 200 && actualDistance > 200)
                || (lastDistance > 100 && actualDistance <= 100)
                || (lastDistance <= 100 && actualDistance > 100)
                || (lastDistance > 50 && actualDistance <= 50)
                || (lastDistance <= 50 && actualDistance > 50)
                || (lastDistance > 10 && actualDistance <= 10)
                || (lastDistance <= 10 && actualDistance > 10)) {
            sendNotification(actualDistance);
            if (actualDistance <= 10) {
                // TODO: 24/04/17 Agregar publicación de Tweet.
                TweetComposer.Builder builder = new TweetComposer.Builder(this)
                        .text("El usuario está en el punto objetivo. Latitud: " + markerLocation.getLatitude() + ", Longitud: " + markerLocation.getLongitude());
                builder.show();
            }
        }

        lastDistance = actualDistance;

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

    private void sendNotification(int distance) {

        String message = "";

        if (distance > 200)
            message = getResources().getString(R.string.msg_far_away);
        if (distance > 100 && distance <= 200)
            message = getResources().getString(R.string.msg_far);
        if (distance > 50 && distance <= 100)
            message = getResources().getString(R.string.msg_close);
        if (distance > 10 && distance <= 50)
            message = getResources().getString(R.string.msg_very_close);
        if (distance < 10)
            message = getResources().getString(R.string.msg_at_target);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.icon_map_marker)
                .setContentTitle("Pulpo")
                .setContentText(message)
                .setContentIntent(pendingIntent)
                ;

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(1, builder.build());
    }
}
