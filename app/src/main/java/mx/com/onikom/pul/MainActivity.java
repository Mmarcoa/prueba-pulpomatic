package mx.com.onikom.pul;

import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

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
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "MainActivity";
    private GoogleMap googleMap;

    private GoogleApiClient googleApiClient;
    private boolean locationPermissionGranted;
    private Location lastKnownLocation;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

    private void getDeviceLocation() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

    }

    /**
     * Set the location controls on the map. If the user has granted location permission,
     * enable the My Location layer and the related control on the map, otherwise disable
     * the layer and the control, and set the current location to null
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
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
    }
}
