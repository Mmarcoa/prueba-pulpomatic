package mx.com.onikom.pul;

import android.content.pm.PackageManager;
import android.location.Location;
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
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
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
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

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
    private Button fixPositionButton;
    private ImageView marker;
    private CardView searchView;
    private CardView resetView;
    private CardView infoView;

    private static final int IN_POINT = 10;
    private static final int VERY_CLOSE = 50;
    private static final int CLOSE = 100;
    private static final int FAR = 200;

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
        fixPositionButton = (Button) findViewById(R.id.fix_position_button);
        marker = (ImageView) findViewById(R.id.marker);
        searchView = (CardView) findViewById(R.id.search_form);
        resetView = (CardView) findViewById(R.id.reset_cardview);
        infoView = (CardView) findViewById(R.id.info_cardview);

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

        setAutocompleteFragment();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
    }

    @Override
    public void onClick(View v) {

        Fade fade  = new Fade(Fade.OUT);
        // Start recording changes to the view hierarchy
        TransitionManager.beginDelayedTransition(rootView, fade);

        switch (v.getId()) {

            case R.id.fix_position_button:

                updateMarkerFixedUI();
                break;

            case R.id.reset_cardview:

                resetUI();
                break;
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
        LatLng centerPosition = googleMap.getCameraPosition().target;
        Log.d(TAG, "Latitud: " + centerPosition.latitude);
        Log.d(TAG, "Longitud: " + centerPosition.longitude);

        // Quita el botón con animación
        rootView.removeView(fixPositionButton);
        rootView.removeView(marker);
        rootView.removeView(searchView);

        rootView.removeView(resetView);
        rootView.removeView(infoView);

        resetView.setVisibility(View.VISIBLE);
        infoView.setVisibility(View.VISIBLE);

        rootView.addView(resetView);
        rootView.addView(infoView);

        // Instantiates a new CircleOptions object and defines the center and radius
        CircleOptions circle10m = new CircleOptions()
                .strokeColor(Constants.BLUE)
                .fillColor(Constants.BLUE_10M)
                .center(centerPosition)
                .radius(IN_POINT); // In meters
        CircleOptions circle50m = new CircleOptions()
                .strokeColor(Constants.BLUE_10M)
                .fillColor(Constants.BLUE_50M)
                .center(centerPosition)
                .radius(VERY_CLOSE); // In meters
        CircleOptions circle100m = new CircleOptions()
                .strokeColor(Constants.BLUE_50M)
                .fillColor(Constants.BLUE_100M)
                .center(centerPosition)
                .radius(CLOSE); // In meters
        CircleOptions circle200m = new CircleOptions()
                .strokeColor(Constants.BLUE_100M)
                .fillColor(Constants.BLUE_200M)
                .center(centerPosition)
                .radius(FAR); // In meters

        // Get back the mutable Circle
        Circle circle = googleMap.addCircle(circle10m);
        googleMap.addCircle(circle50m);
        googleMap.addCircle(circle100m);
        googleMap.addCircle(circle200m);
        googleMap.addMarker(new MarkerOptions().position(centerPosition));

        resetView.setOnClickListener(this);
    }

    /**
     * Se reinica la selección de punto objetivo, remueve marcador y círculos
     * se mueve la cámara a la posición del dispositivo.
     */
    private void resetUI() {
        Log.d(TAG, "Reiniciar selección");

        rootView.removeView(resetView);
        rootView.removeView(infoView);

        rootView.addView(fixPositionButton);
        rootView.addView(marker);
        rootView.addView(searchView);

        googleMap.clear();

        getDeviceLocation();
    }
}
