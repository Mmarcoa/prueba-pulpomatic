package mx.com.onikom.pul;

import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.transition.TransitionManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.transition.Fade;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import mx.com.onikom.pul.helpers.Constants;

/**
 El objeto de la aplicación será seleccionar un punto dentro de un mapa (punto objetivo)
 y controlar mediante GPS cuando el usuario de la aplicación se acerca al citado punto.

 Al abrirse la aplicación te debe mostrar el mapa de Google Maps centrado en la posición
 del usuario, con un pin o marcador en su lugar.
 La selección del punto objetivo podrá realizarse mediante una caja de búsqueda de dirección,
 o desplazando el mapa hasta dejar el pin en el punto objetivo.
 Debe existir un botón para confirmar el punto objetivo.
 Una vez pulsado el punto objetivo se mostrará una caja de texto al pie de la pantalla en la
 que se indique la distancia del usuario al punto objetivo en línea recta con los siguientes mensajes.
 Distancia > 200m: "Estás muy lejos del punto objetivo"
 Distancia > 100m y <= 200m: "Estás lejos del punto objetivo"
 Distancia > 50m y <= 100m: "Estás próximo al punto objetivo"
 Distancia > 10m y <= 50m: "Estás muy próximo al punto objetivo"
 Distancia < 10m: "Estás en el punto objetivo"
 Mientras el usuario se mueve, se debe mostrar en el mapa su posición, junto con varios círculos
 concéntricos en el punto objetivo representando las diferentes áreas de proximidad detalladas en
 el punto anterior.
 Tanto la distancia como el mensaje de información debe actualizarse de manera automática en la
 caja de texto.
 Cuando la aplicación está en segundo plano, debe seguir calculando la distancia al punto objetivo
 y mostrar un aviso cuando haya superado una barrera de distancia indicando el mensaje que corresponda.
 Cada vez que el usuario esté en el rango de menos de 10m del punto objetivo debe publicar un tweet
 en una cuenta que crees para esta prueba indicando que el usuario está en el punto objetivo, junto
 con la latitud y longitud del punto objetivo y una captura de mapa donde se indique el punto en
 cuestión. Si el usuario sale del punto objetivo y vuelve a entrar debe volver a publicar el mensaje
 en tweet.
 Debe aparecer siempre un botón para reiniciar el proceso de la aplicación, para volver a definir
 una dirección objetivo.

 Se valorará:
 La calidad del interfaz
 La precisión y estabilidad en el funcionamiento de la geolocalización
 El tiempo para realizar el ejercicio
 Calidad del código y arquitectura de la aplicación
 Mejoras de funcionalidad incluidas en la aplicación
 Fecha límite del ejercicio: Una semana a partir del envío.
*/

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener, LocationListener{

    private static final String TAG = "MainActivity";
    private GoogleMap googleMap;
    private CameraPosition cameraPosition;

    // Punto de entrada a los servicios de Google Play
    private GoogleApiClient googleApiClient;

    // A default location and default zoom to use when location permission is
    // not granted.
    private final LatLng defaultLocation = new LatLng(19.4237049,-99.163055);
    private static final int DEFAULT_ZOOM = 16;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location lastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    private ViewGroup rootView;
    private CardView fixPositionButton;
    private ImageView marker;
    private CardView searchView;
    private LinearLayout resetView;
    private CardView infoView;
    private TextView distanceText;
    private TextView messageText;

    private static final int IN_POINT = 10;
    private static final int VERY_CLOSE = 50;
    private static final int CLOSE = 100;
    private static final int FAR = 200;

    private static final int REQUEST_CHECK_SETTINGS = 2;

    private Marker objectivePointMarker;

    private LocationRequest locationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "OnCreate");

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_main);

        rootView = (ViewGroup) findViewById(R.id.mainLayout);
        fixPositionButton = (CardView) findViewById(R.id.fix_position_cardview);
        marker = (ImageView) findViewById(R.id.marker);
        searchView = (CardView) findViewById(R.id.search_form);
        resetView = (LinearLayout) findViewById(R.id.reset_button);
        infoView = (CardView) findViewById(R.id.info_cardview);
        distanceText = (TextView) findViewById(R.id.distance_text);
        messageText = (TextView) findViewById(R.id.message);

        // Se agrega Google Places y Location.
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
        googleApiClient.connect();
    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (googleMap != null) {
            // FIXME: 20/04/17 Guardar estado cuando ya se fijó marcador: marcador, círculos, etc.
            outState.putParcelable(KEY_CAMERA_POSITION, googleMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onBackPressed() {
        if (ViewCompat.isAttachedToWindow(resetView)) {
            resetUI();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
        // FIXME: 18/04/17 Pide permiso y no regresa a la actividad. Desinstalar app para probar
        Log.d(TAG, "On request result.");
    }

    /**
     * Builds the map when the Google Play services client is successfully connected.
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Handles suspension of the connection to the Google Play services client.
     */
    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Play services connection suspended");
    }

    /**
     * Handles failure to connect to the Google Play services client.
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "Play services connection failed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            // Return null here, so that getInfoContents() is called next.
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents,
                        (FrameLayout)findViewById(R.id.map), false);

                TextView title = ((TextView) infoWindow.findViewById(R.id.title));
                title.setText(marker.getTitle());

                TextView snippet = ((TextView) infoWindow.findViewById(R.id.snippet));
                snippet.setText(marker.getSnippet());

                return infoWindow;
            }
        });

        fixPositionButton.setOnClickListener(this);

        createLocationRequest();
    }

    @Override
    public void onClick(View v) {

        Fade fade  = new Fade(Fade.OUT);
        // Start recording changes to the view hierarchy
        TransitionManager.beginDelayedTransition(rootView, fade);

        switch (v.getId()) {

            case R.id.fix_position_cardview:

                updateMarkerFixedUI();
                break;

            case R.id.reset_button:

                resetUI();
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Current location: " + location);
        Log.d(TAG, "Provider: " + location.getProvider());
        Location markerLocation = new Location(LocationManager.GPS_PROVIDER);
        if (objectivePointMarker != null) {
            markerLocation.setLatitude(objectivePointMarker.getPosition().latitude);
            markerLocation.setLongitude(objectivePointMarker.getPosition().longitude);
            int distance = Math.round(location.distanceTo(markerLocation));
            Log.d(TAG, "Distance to marker: " + distance);
            distanceText.setText(String.format(getResources().getString(R.string.distance), distance));
            String message = "";
            if (distance > FAR)
                message = getResources().getString(R.string.msg_far_away);
            if (distance > CLOSE && distance <= FAR)
                message = getResources().getString(R.string.msg_far);
            if (distance > VERY_CLOSE && distance <= CLOSE)
                message = getResources().getString(R.string.msg_close);
            if (distance > IN_POINT && distance <= VERY_CLOSE)
                message = getResources().getString(R.string.msg_very_close);
            if (distance < IN_POINT)
                message = getResources().getString(R.string.msg_at_target);
            messageText.setText(message);
        }
    }

    /**
     * Se añade un 'listener' al PlaceAutocompleteFragment
     */
    private void setAutocompleteFragment() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (locationPermissionGranted) {
            PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                    getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

            autocompleteFragment.setHint(getResources().getString(R.string.select_point));

            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(Place place) {
                    Log.d(TAG, "Place: " + place.getName());
                    googleMap.animateCamera(CameraUpdateFactory.newLatLng(place.getLatLng()));
                }

                @Override
                public void onError(Status status) {
                    Log.d(TAG, "Ocurrió un error: " + status);
                }
            });
        }
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (googleMap == null) {
            return;
        }

    /*
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (locationPermissionGranted) {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            googleMap.setMyLocationEnabled(false);
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            lastKnownLocation = null;
        }
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        if (locationPermissionGranted) {
            lastKnownLocation = LocationServices.FusedLocationApi
                    .getLastLocation(googleApiClient);
        }

        // Set the map's camera position to the current location of the device.
        if (cameraPosition != null) {
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else if (lastKnownLocation != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(lastKnownLocation.getLatitude(),
                            lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
        } else {
            Log.d(TAG, "Current location is null. Using defaults.");
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    /**
     * Actualiza UI, coloca marcador y oculta vistas para fijar posición, muestra vista de información
     * y botón para reiniciar selección
     */
    private void updateMarkerFixedUI() {
        LatLng markerPosition = googleMap.getCameraPosition().target;
        Log.d(TAG, "Latitud: " + markerPosition.latitude);
        Log.d(TAG, "Longitud: " + markerPosition.longitude);

        // Quita el botón con animación
        rootView.removeView(fixPositionButton);
        rootView.removeView(marker);
        rootView.removeView(searchView);

//        rootView.removeView(resetView);
        rootView.removeView(infoView);

//        resetView.setVisibility(View.VISIBLE);
        infoView.setVisibility(View.VISIBLE);

//        rootView.addView(resetView);
        rootView.addView(infoView);

        // Instantiates a new CircleOptions object and defines the center and radius
        CircleOptions circle10m = new CircleOptions()
                .strokeColor(Constants.BLUE)
                .fillColor(Constants.BLUE_10M)
                .center(markerPosition)
                .radius(IN_POINT); // In meters
        CircleOptions circle50m = new CircleOptions()
                .strokeColor(Constants.BLUE_10M)
                .fillColor(Constants.BLUE_50M)
                .center(markerPosition)
                .radius(VERY_CLOSE); // In meters
        CircleOptions circle100m = new CircleOptions()
                .strokeColor(Constants.BLUE_50M)
                .fillColor(Constants.BLUE_100M)
                .center(markerPosition)
                .radius(CLOSE); // In meters
        CircleOptions circle200m = new CircleOptions()
                .strokeColor(Constants.BLUE_100M)
                .fillColor(Constants.BLUE_200M)
                .center(markerPosition)
                .radius(FAR); // In meters

        // Get back the mutable Circle
        googleMap.addCircle(circle10m);
        googleMap.addCircle(circle50m);
        googleMap.addCircle(circle100m);
        googleMap.addCircle(circle200m);
        objectivePointMarker = googleMap.addMarker(new MarkerOptions().position(markerPosition));

        startLocationUpdates();
        resetView.setOnClickListener(this);
    }

    /**
     * Se reinica la selección de punto objetivo, remueve marcador y círculos
     * se mueve la cámara a la posición del dispositivo.
     */
    private void resetUI() {
        Log.d(TAG, "Reiniciar selección");

//        rootView.removeView(resetView);
        rootView.removeView(infoView);

        rootView.addView(fixPositionButton);
        rootView.addView(marker);
        rootView.addView(searchView);

        googleMap.clear();

        // Detiene las actualizaciones de posición del dispositivo.
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);

        getDeviceLocation();
    }


    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
//                final LocationSettingsStates settingsStates = locationSettingsResult.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.
                        Log.d(TAG, "Success");

                        setAutocompleteFragment();

                        // Turn on the My Location layer and the related control on the map.
                        updateLocationUI();

                        // Get the current location of the device and set the position of the map.
                        getDeviceLocation();

                        startLocationUpdates();

                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        Log.d(TAG, "Resolution required");
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    MainActivity.this,
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        Log.d(TAG, "Settings unavailable");
                        break;
                }
            }
        });
    }

    private void startLocationUpdates() {
        if (googleMap == null) {
            return;
        }

    /*
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (locationPermissionGranted) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    googleApiClient, locationRequest, this);
        }
    }
}
