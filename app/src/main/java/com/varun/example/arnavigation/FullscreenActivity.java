package com.varun.example.arnavigation;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

// classes needed to initialize map
import com.google.gson.JsonObject;
import com.mapbox.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.core.exceptions.ServicesException;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

// classes needed to add the location component
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;

// classes needed to add a marker
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;

// classes to calculate a route
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import android.util.Log;

// classes needed to launch navigation UI
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class FullscreenActivity extends AppCompatActivity implements OnMapReadyCallback, MapboxMap.OnMapClickListener,
        PermissionsListener, View.OnClickListener{
    // variables for adding location layer
    private MapView mapView;
    private static MapboxMap mapboxMap = null;
    // variables for adding location layer
    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;
    // variables for calculating and drawing a route
    public static  DirectionsRoute currentRoute = null;
    private static final String TAG = "DirectionsActivity";
    private NavigationMapRoute navigationMapRoute;
    // variables needed to initialize navigation
    //private Button button;
    private ImageButton button;
    private ImageButton arNavButton;
    private ImageButton currentLocationButton;
    private ImageButton directionsButton;
    private TextView routeInfo;
    private TextView originLoc;
    private TextView destLoc;
    private static final int REQUEST_CODE_AUTOCOMPLETE_ORIGIN = 1;
    private static final int REQUEST_CODE_AUTOCOMPLETE_DESTINATION = 2;
    private static final int REQUEST_CODE_DIRECTIONS = 3;
    private CarmenFeature home;
    private CarmenFeature work;
    private CarmenFeature current;
    private String geojsonSourceLayerId = "geojsonSourceLayerId";
    private String symbolIconId = "symbolIconId";
    private Point originPoint;
    private Point destinationPoint;
    private static final String SOURCE_ID_ORG = "SOURCE_ID_ORG";
    private static final String ICON_ID_ORG = "ICON_ID_ORG";
    private static final String LAYER_ID_ORG = "LAYER_ID_ORG";
    private static final String SOURCE_ID_DEST = "SOURCE_ID_DEST";
    private static final String ICON_ID_DEST = "ICON_ID_DEST";
    private static final String LAYER_ID_DEST = "LAYER_ID_DEST";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_fullscreen);
        directionsButton = findViewById(R.id.directionsButton);
        directionsButton.setEnabled(false);
        routeInfo = findViewById(R.id.routeInfo);
        routeInfo.setEnabled(false);

        routeInfo.setVisibility(View.INVISIBLE);
        originLoc = findViewById(R.id.start_loc);
        originLoc.setVisibility(View.INVISIBLE);
        destLoc = findViewById(R.id.dest_loc);
        destLoc.setVisibility(View.INVISIBLE);
        directionsButton.setOnClickListener(this);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(getString(R.string.navigation_guidance_day), new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationComponent(style);

                addDestinationIconSymbolLayer(style);

                initSearchFab();

                addUserLocations();

                mapboxMap.addOnMapClickListener(FullscreenActivity.this);
                button = findViewById(R.id.startButton);
                arNavButton = findViewById(R.id.startArNavButton);
                button.setEnabled(false);
                arNavButton.setEnabled(false);
                currentLocationButton = findViewById(R.id.currentLocationButton);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadNav();

                        boolean simulateRoute = true;
                        NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                                .directionsRoute(currentRoute)
                                .shouldSimulateRoute(simulateRoute)

                                .build();

                        // Call this method with Context from within an Activity
                        //NavigationLauncher.startNavigation(FullscreenActivity.this, options);

                    }
                });

                arNavButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadArNav();
                    }
                });

                currentLocationButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        enableLocationComponent(style);
                    }
                });
            }
        });
    }

    private void loadNav(){
        Intent intent = new Intent(getApplicationContext(),NavigationActivity.class);
        intent.putExtra(String.valueOf(NavigationActivity.originLat),originPoint.latitude());
        intent.putExtra(String.valueOf(NavigationActivity.originLong),originPoint.longitude());
        intent.putExtra(String.valueOf(NavigationActivity.destLat),destinationPoint.latitude());
        intent.putExtra(String.valueOf(NavigationActivity.destLong),originPoint.longitude());
        startActivity(intent);
    }

    private void loadArNav(){
        Intent intent = new Intent(getApplicationContext(),ArNavigationActivity.class);
        intent.putExtra(String.valueOf(NavigationActivity.originLat),originPoint.latitude());
        intent.putExtra(String.valueOf(NavigationActivity.originLong),originPoint.longitude());
        intent.putExtra(String.valueOf(NavigationActivity.destLat),destinationPoint.latitude());
        intent.putExtra(String.valueOf(NavigationActivity.destLong),originPoint.longitude());
        startActivity(intent);
    }

    private void addDestinationIconSymbolLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage("destination-icon-id",
                BitmapFactory.decodeResource(this.getResources(), R.drawable.mapbox_marker_icon_default));
        GeoJsonSource geoJsonSource = new GeoJsonSource("destination-source-id");
        loadedMapStyle.addSource(geoJsonSource);
        SymbolLayer destinationSymbolLayer = new SymbolLayer("destination-symbol-layer-id", "destination-source-id");
        destinationSymbolLayer.withProperties(
                iconImage("destination-icon-id"),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
        );
        loadedMapStyle.addLayer(destinationSymbolLayer);
    }

    @SuppressWarnings( {"MissingPermission"})
    @Override
    public boolean onMapClick(@NonNull LatLng point) {

        destinationPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
        originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                locationComponent.getLastKnownLocation().getLatitude());

        GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");
        if (source != null) {
            source.setGeoJson(Feature.fromGeometry(destinationPoint));
        }

        try {
            MapboxGeocoding client = MapboxGeocoding.builder()
                    .accessToken(getString(R.string.mapbox_access_token))
                    .query(Point.fromLngLat(originPoint.longitude(), originPoint.latitude()))
                    .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
                    .mode(GeocodingCriteria.MODE_PLACES)
                    .build();

            client.enqueueCall(new Callback<GeocodingResponse>() {
                @Override
                public void onResponse(Call<GeocodingResponse> call,
                                       Response<GeocodingResponse> response) {
                    if (response.body() != null) {
                        List<CarmenFeature> results = response.body().features();
                        if (results.size() > 0) {
                            CarmenFeature carmenFeature;
                            // Get the first Feature from the successful geocoding response
                            carmenFeature = CarmenFeature.builder().text("Your Location")
                                    .geometry(Point.fromLngLat(originPoint.longitude(), originPoint.latitude()))
                                    .placeName(results.get(0).placeName())
                                    .id("current-loc")
                                    .properties(new JsonObject())
                                    .build();
                            originLoc.setVisibility(View.VISIBLE);
                            originLoc.setText("");
                            originLoc.setText(carmenFeature.placeName());
                        } else {

                        }
                    }
                }

                @Override
                public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
                    Timber.e("Geocoding Failure: " + throwable.getMessage());
                }
            });
        } catch (ServicesException servicesException) {
            Timber.e("Error geocoding: " + servicesException.toString());
            servicesException.printStackTrace();
        }


        try {
            // Build a Mapbox geocoding request
            MapboxGeocoding client = MapboxGeocoding.builder()
                    .accessToken(getString(R.string.mapbox_access_token))
                    .query(Point.fromLngLat(destinationPoint.longitude(), destinationPoint.latitude()))
                    .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
                    .mode(GeocodingCriteria.MODE_PLACES)
                    .build();

            client.enqueueCall(new Callback<GeocodingResponse>() {
                @Override
                public void onResponse(Call<GeocodingResponse> call,
                                       Response<GeocodingResponse> response) {
                    if (response.body() != null) {
                        List<CarmenFeature> results = response.body().features();
                        if (results.size() > 0) {
                            CarmenFeature carmenFeature;
                            // Get the first Feature from the successful geocoding response
                            carmenFeature = CarmenFeature.builder().text("Your Location")
                                    .geometry(Point.fromLngLat(destinationPoint.longitude(), destinationPoint.latitude()))
                                    .placeName(results.get(0).placeName())
                                    .id("current-loc")
                                    .properties(new JsonObject())
                                    .build();
                            destLoc.setVisibility(View.VISIBLE);
                            destLoc.setText("");
                            destLoc.setText(carmenFeature.placeName());
                        } else {

                        }
                    }
                }

                @Override
                public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
                    Timber.e("Geocoding Failure: " + throwable.getMessage());
                }
            });
        } catch (ServicesException servicesException) {
            Timber.e("Error geocoding: " + servicesException.toString());
            servicesException.printStackTrace();
        }

        getRoute(originPoint, destinationPoint);
        button.setEnabled(true);
        arNavButton.setEnabled(true);

        button.setBackgroundResource(R.color.mapboxBlue);
        arNavButton.setBackgroundResource(R.color.mapboxRed);

        /*
        originLoc.setVisibility(View.VISIBLE);
        originLoc.setText("");
        originLoc.setText(originPoint.coordinates().toString());

        destLoc.setVisibility(View.VISIBLE);
        destLoc.setText("");
        destLoc.setText(destinationPoint.coordinates().toString());
        */
        return true;
    }

    private void getRoute(Point origin, Point destination) {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        // You can get the generic HTTP info about the response
                        Log.d(TAG, "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e(TAG, "No routes found");
                            return;
                        }
                        currentRoute = response.body().routes().get(0);
                        double dist = currentRoute.distance()/1666;
                        double dur = currentRoute.duration()/60;
                        routeInfo.setEnabled(true);
                        routeInfo.setVisibility(View.VISIBLE);
                        routeInfo.setText("");
                        routeInfo.setText("Distance : " + String.format("%.3f",dist) + " Mi" + "\nDuration : " + String.format("%.3f",dur) + " Min");
                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e(TAG, "Error: " + throwable.getMessage());
                    }
                });
    }

    public static final void clearMap(String source){
        Style style= FullscreenActivity.mapboxMap.getStyle();
        if (source == "origin"){
            if(style.getSource(SOURCE_ID_ORG)!=null){
                style.removeSource(style.getSource(SOURCE_ID_ORG));
            }
            if(style.getLayer(LAYER_ID_ORG)!=null){
                style.removeLayer(style.getLayer(LAYER_ID_ORG));
            }
            if (style.getImage(ICON_ID_ORG)!=null){
                style.removeImage(ICON_ID_ORG);
            }
        }
        if (source == "destination"){
            if(style.getSource(SOURCE_ID_DEST)!=null){
                style.removeSource(style.getSource(SOURCE_ID_DEST));
            }
            if(style.getLayer(LAYER_ID_DEST)!=null){
                style.removeLayer(style.getLayer(LAYER_ID_DEST));
            }
            if (style.getImage(ICON_ID_DEST)!=null){
                style.removeImage(ICON_ID_DEST);
            }
        }

    }

    private void initSearchFab() {
        findViewById(R.id.fab_search_origin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] address = current.placeName().split(",");
                String country = address[address.length-1];

                Intent intent = new PlaceAutocomplete.IntentBuilder()
                        .accessToken(Mapbox.getAccessToken() != null ? Mapbox.getAccessToken() : getString(R.string.mapbox_access_token))
                        .placeOptions(PlaceOptions.builder()
                                .backgroundColor(Color.parseColor("#EEEEEE"))
                                .limit(10)
                                .proximity(Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),locationComponent.getLastKnownLocation().getLatitude()))
                                .addInjectedFeature(home)
                                .addInjectedFeature(work)
                                .addInjectedFeature(current)
                                .build(PlaceOptions.MODE_CARDS))
                        .build(FullscreenActivity.this);
                startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE_ORIGIN);
            }
        });
        findViewById(R.id.fab_search_dest).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                String[] address = current.placeName().split(",");
                String country = address[address.length-1];
                Intent intent = new PlaceAutocomplete.IntentBuilder()
                        .accessToken(Mapbox.getAccessToken() != null ? Mapbox.getAccessToken() : getString(R.string.mapbox_access_token))
                        .placeOptions(PlaceOptions.builder()
                                .backgroundColor(Color.parseColor("#EEEEEE"))
                                .limit(10)
                                .proximity(Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),locationComponent.getLastKnownLocation().getLatitude()))
                                .addInjectedFeature(home)
                                .addInjectedFeature(work)
                                .addInjectedFeature(current)
                                .build(PlaceOptions.MODE_CARDS))
                        .build(FullscreenActivity.this);
                startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE_DESTINATION);
            }
        });
    }

    private void addUserLocations() {
        home = CarmenFeature.builder().text("Mapbox SF Office")
                .geometry(Point.fromLngLat(-122.3964485, 37.7912561))
                .placeName("50 Beale St, San Francisco, CA")
                .id("mapbox-sf")
                .properties(new JsonObject())
                .build();

        work = CarmenFeature.builder().text("Mapbox DC Office")
                .placeName("740 15th Street NW, Washington DC")
                .geometry(Point.fromLngLat(-77.0338348, 38.899750))
                .id("mapbox-dc")
                .properties(new JsonObject())
                .build();
        Location loc = locationComponent.getLastKnownLocation();
        try {
            // Build a Mapbox geocoding request
            MapboxGeocoding client = MapboxGeocoding.builder()
                    .accessToken(getString(R.string.mapbox_access_token))
                    .query(Point.fromLngLat(loc.getLongitude(), loc.getLatitude()))
                    .geocodingTypes(GeocodingCriteria.TYPE_PLACE)
                    .mode(GeocodingCriteria.MODE_PLACES)
                    .build();

            client.enqueueCall(new Callback<GeocodingResponse>() {
                @Override
                public void onResponse(Call<GeocodingResponse> call,
                                       Response<GeocodingResponse> response) {
                    if (response.body() != null) {
                        List<CarmenFeature> results = response.body().features();
                        if (results.size() > 0) {

                        // Get the first Feature from the successful geocoding response
                            current = CarmenFeature.builder().text("Your Location")
                                    .geometry(Point.fromLngLat(loc.getLongitude(),loc.getLatitude()))
                                    .placeName(results.get(0).placeName())
                                    .id("current-loc")
                                    .properties(new JsonObject())
                                    .build();
                            //current = results.get(0);

                            //geocodeResultTextView.setText(feature.toString());
                            //animateCameraToNewPosition(latLng);
                        } else {
                            /*
                            Toast.makeText(GeocodingActivity.this, R.string.no_results,
                                    Toast.LENGTH_SHORT).show();

                             */
                        }
                    }
                }

                @Override
                public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
                    Timber.e("Geocoding Failure: " + throwable.getMessage());
                }
            });
        } catch (ServicesException servicesException) {
            Timber.e("Error geocoding: " + servicesException.toString());
            servicesException.printStackTrace();
        }
        /*
        current = CarmenFeature.builder().text("Your Location")
                    .geometry(Point.fromLngLat(loc.getLongitude(),loc.getLatitude()))
                    .id("current-loc")
                    .properties(new JsonObject())
                    .build();

         */
    }

    private void setUpSource(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addSource(new GeoJsonSource(geojsonSourceLayerId));
    }

    private void setupLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addLayer(new SymbolLayer("SYMBOL_LAYER_ID", geojsonSourceLayerId).withProperties(
                iconImage(symbolIconId),
                iconOffset(new Float[] {0f, -8f})
        ));
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Activate the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(this, loadedMapStyle);
            locationComponent.setLocationComponentEnabled(true);
            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE_ORIGIN) {

            // Retrieve selected location's CarmenFeature
            CarmenFeature selectedCarmenFeature = PlaceAutocomplete.getPlace(data);


            // Create a new FeatureCollection and add a new Feature to it using selectedCarmenFeature above.
            // Then retrieve and update the source designated for showing a selected location's symbol layer icon

            if (mapboxMap != null) {
                Style style = mapboxMap.getStyle();
                if (style != null) {
                    GeoJsonSource source = style.getSourceAs(geojsonSourceLayerId);
                    if (source != null) {
                        source.setGeoJson(FeatureCollection.fromFeatures(
                                new Feature[] {Feature.fromJson(selectedCarmenFeature.toJson())}));
                    }

                    // Move map camera to the selected location
                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(((Point) selectedCarmenFeature.geometry()).latitude(),
                                            ((Point) selectedCarmenFeature.geometry()).longitude()))
                                    .zoom(14)

                                    .build()), 4000);

                    LatLng point = new LatLng(((Point) selectedCarmenFeature.geometry()).latitude(),
                            ((Point) selectedCarmenFeature.geometry()).longitude());
                    originPoint =  Point.fromLngLat(point.getLongitude(), point.getLatitude());


                    //GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");
                    if (source != null) {
                        source.setGeoJson(Feature.fromGeometry(originPoint));
                    }
                    clearMap("origin");
                    //mapboxMap.addMarker(MarkerOptions().position(destinationPoint));
                    List<Feature> symbolLayerIconFeatureList = new ArrayList<>();
                    symbolLayerIconFeatureList.add(Feature.fromGeometry(
                            originPoint));
                    style.addImage(ICON_ID_ORG, BitmapFactory.decodeResource(
                            FullscreenActivity.this.getResources(), R.drawable.mapbox_marker_icon_default));
                    style.removeSource(SOURCE_ID_ORG);
                    style.addSource(new GeoJsonSource(SOURCE_ID_ORG,
                            FeatureCollection.fromFeatures(symbolLayerIconFeatureList)));
                    style.addLayer(new SymbolLayer(LAYER_ID_ORG, SOURCE_ID_ORG)
                            .withProperties(
                                    iconImage(ICON_ID_ORG),
                                    iconAllowOverlap(true),
                                    iconIgnorePlacement(true)
                            ));
                    String data1 = selectedCarmenFeature.address();
                    originLoc.setVisibility(View.VISIBLE);
                    if(current.center() == selectedCarmenFeature.center()){

                        originLoc.setText(current.placeName());
                    }
                    originLoc.setText(selectedCarmenFeature.placeName());
                    //directionsButton.setEnabled(true);

                }
            }
        }
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE_DESTINATION) {
            CarmenFeature selectedCarmenFeature = PlaceAutocomplete.getPlace(data);
            if (mapboxMap != null) {
                Style style = mapboxMap.getStyle();
                if (style != null) {
                    GeoJsonSource source = style.getSourceAs(geojsonSourceLayerId);
                    if (source != null) {
                        source.setGeoJson(FeatureCollection.fromFeatures(
                                new Feature[]{Feature.fromJson(selectedCarmenFeature.toJson())}));
                    }

                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(((Point) selectedCarmenFeature.geometry()).latitude(),
                                            ((Point) selectedCarmenFeature.geometry()).longitude()))
                                    .zoom(14)

                                    .build()), 4000);

                    LatLng point = new LatLng(((Point) selectedCarmenFeature.geometry()).latitude(),
                            ((Point) selectedCarmenFeature.geometry()).longitude());
                    destinationPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
                    if (source != null) {
                        source.setGeoJson(Feature.fromGeometry(originPoint));
                    }
                    clearMap("destination");
                    //mapboxMap.addMarker(MarkerOptions().position(destinationPoint));
                    List<Feature> symbolLayerIconFeatureList = new ArrayList<>();
                    symbolLayerIconFeatureList.add(Feature.fromGeometry(
                            destinationPoint));
                    style.addImage(ICON_ID_DEST, BitmapFactory.decodeResource(
                            FullscreenActivity.this.getResources(), R.drawable.mapbox_marker_icon_default));
                    style.removeSource(SOURCE_ID_DEST);
                    style.addSource(new GeoJsonSource(SOURCE_ID_DEST,
                            FeatureCollection.fromFeatures(symbolLayerIconFeatureList)));
                    style.addLayer(new SymbolLayer(LAYER_ID_DEST, SOURCE_ID_DEST)
                            .withProperties(
                                    iconImage(ICON_ID_DEST),
                                    iconAllowOverlap(true),
                                    iconIgnorePlacement(true)
                            ));
                    destLoc.setVisibility(View.VISIBLE);
                    destLoc.setText(selectedCarmenFeature.placeName());
                    directionsButton.setEnabled(true);

                }
            }
        }

    }
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.directionsButton:

                getRoute(originPoint, destinationPoint);
                button.setEnabled(true);
                arNavButton.setEnabled(true);

                button.setBackgroundResource(R.color.mapboxBlue);
                arNavButton.setBackgroundResource(R.color.mapboxRed);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationComponent(mapboxMap.getStyle());
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

}