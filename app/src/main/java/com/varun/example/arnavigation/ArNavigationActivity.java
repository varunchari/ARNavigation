package com.varun.example.arnavigation;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.LinkMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.route.RouteFetcher;
import com.mapbox.services.android.navigation.v5.route.RouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.vision.VisionManager;
import com.mapbox.vision.ar.VisionArManager;
import com.mapbox.vision.ar.core.models.ManeuverType;
import com.mapbox.vision.ar.core.models.Route;
import com.mapbox.vision.ar.core.models.RoutePoint;
import com.mapbox.vision.ar.view.gl.VisionArView;
import com.mapbox.vision.mobile.core.interfaces.VisionEventsListener;
import com.mapbox.vision.mobile.core.models.AuthorizationStatus;
import com.mapbox.vision.mobile.core.models.Camera;
import com.mapbox.vision.mobile.core.models.Country;
import com.mapbox.vision.mobile.core.models.FrameSegmentation;
import com.mapbox.vision.mobile.core.models.classification.FrameSignClassifications;
import com.mapbox.vision.mobile.core.models.detection.FrameDetections;
import com.mapbox.vision.mobile.core.models.position.GeoCoordinate;
import com.mapbox.vision.mobile.core.models.position.VehicleState;
import com.mapbox.vision.mobile.core.models.road.RoadDescription;
import com.mapbox.vision.mobile.core.models.world.WorldDescription;
import com.mapbox.vision.mobile.core.utils.SystemInfoUtils;
import com.mapbox.vision.performance.ModelPerformance;
import com.mapbox.vision.performance.ModelPerformanceMode;
import com.mapbox.vision.performance.ModelPerformanceRate;
import com.mapbox.vision.utils.VisionLogger;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class ArNavigationActivity extends AppCompatActivity implements RouteListener, ProgressChangeListener, OffRouteListener {
    private static final String TAG = ArNavigationActivity.class.getSimpleName();
    private static final String PERMISSION_FOREGROUND_SERVICE = "android.permission.FOREGROUND_SERVICE";
    // Handles navigation.
    private MapboxNavigation mapboxNavigation;
    // Fetches route from points.
    private RouteFetcher routeFetcher;
    private RouteProgress lastRouteProgress;
    private LocationEngine locationEngine;
    private LocationEngineCallback<LocationEngineResult> locationCallback;

    private boolean visionManagerWasInit = false;
    private boolean navigationWasStarted = false;

    // This dummy points will be used to build route. For real world test this needs to be changed to real values for
    // source and target locations.
    private  Point ROUTE_ORIGIN = Point.fromLngLat(27.654285, 53.928057);
    private  Point ROUTE_DESTINATION = Point.fromLngLat(27.655637, 53.935712);
    public static final String originLat = "Origin_Lat";
    public static final String originLong = "Origin_Long";
    public static final String destLat = "Dest_lat";
    public static final String destLong = "Dest_Long";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (!SystemInfoUtils.INSTANCE.isVisionSupported()) {
            final TextView textView = new TextView(this);
            final int padding = (int) dpToPx(20f);
            textView.setPadding(padding, padding, padding, padding);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            textView.setClickable(true);
            textView.setText(
                    HtmlCompat.fromHtml(
                            getString(R.string.vision_not_supported_message),
                            HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
            );
            new AlertDialog.Builder(this)
                    .setTitle(R.string.vision_not_supported_title)
                    .setView(textView)
                    .setCancelable(false)
                    .show();

            VisionLogger.Companion.e(
                    "BoardNotSupported",
                    "System Info: [" + SystemInfoUtils.INSTANCE.obtainSystemInfo() + "]"
            );
        }
        setContentView(R.layout.trial);
        ROUTE_ORIGIN = Point.fromLngLat(getIntent().getDoubleExtra(originLong,ROUTE_ORIGIN.longitude()),getIntent().getDoubleExtra(originLat,ROUTE_ORIGIN.latitude()));
        ROUTE_DESTINATION = Point.fromLngLat(getIntent().getDoubleExtra(destLong,ROUTE_DESTINATION.longitude()),getIntent().getDoubleExtra(destLat,ROUTE_DESTINATION.latitude()));
        VisionManager.init(getApplication(), getResources().getString(R.string.mapbox_access_token));
    }
    private float dpToPx(final float dp) {
        return dp * getApplicationContext().getResources().getDisplayMetrics().density;
    }
    protected boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // PERMISSION_FOREGROUND_SERVICE was added for targetSdkVersion >= 28, it is normal and always granted, but should be added to the Manifest file
                // on devices with Android < P(9) checkSelfPermission(PERMISSION_FOREGROUND_SERVICE) can return PERMISSION_DENIED, but in fact it is GRANTED, so skip it
                // https://developer.android.com/guide/components/services#Foreground
                if (permission.equals(PERMISSION_FOREGROUND_SERVICE)) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    private String[] getRequiredPermissions() {
        String[] permissions;
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] requestedPermissions = info.requestedPermissions;
            if (requestedPermissions != null && requestedPermissions.length > 0) {
                permissions = requestedPermissions;
            } else {
                permissions = new String[]{};
            }
        } catch (PackageManager.NameNotFoundException e) {
            permissions = new String[]{};
        }

        return permissions;
    }
    protected void setArRenderOptions(@NotNull final VisionArView visionArView) {
        visionArView.setFenceVisible(true);
    }
    @Override
    protected void onStart() {
        super.onStart();
        startVisionManager();
        startNavigation();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopVisionManager();
        stopNavigation();
        FullscreenActivity.clearMap("origin");
        FullscreenActivity.clearMap("destination");
        this.finish();
    }
/*
    @Override
    protected void onDestroy() {

        super.onDestroy();
        stopVisionManager();
        stopNavigation();
        FullscreenActivity.clearMap("origin");
        FullscreenActivity.clearMap("destination");
        this.finish();

    }

 */

    private void startVisionManager() {
        if (allPermissionsGranted() && !visionManagerWasInit) {
            // Create and start VisionManager.
            VisionManager.create();
            VisionManager.setModelPerformance(new ModelPerformance.On(ModelPerformanceMode.DYNAMIC, ModelPerformanceRate.LOW.INSTANCE));
            VisionManager.start();
            VisionManager.setVisionEventsListener(new VisionEventsListener() {
                @Override
                public void onAuthorizationStatusUpdated(@NotNull AuthorizationStatus authorizationStatus) {
                }

                @Override
                public void onFrameSegmentationUpdated(@NotNull FrameSegmentation frameSegmentation) {
                }

                @Override
                public void onFrameDetectionsUpdated(@NotNull FrameDetections frameDetections) {
                }

                @Override
                public void onFrameSignClassificationsUpdated(@NotNull FrameSignClassifications frameSignClassifications) {
                }

                @Override
                public void onRoadDescriptionUpdated(@NotNull RoadDescription roadDescription) {
                }

                @Override
                public void onWorldDescriptionUpdated(@NotNull WorldDescription worldDescription) {
                }

                @Override
                public void onVehicleStateUpdated(@NotNull VehicleState vehicleState) {
                }

                @Override
                public void onCameraUpdated(@NotNull Camera camera) {
                }

                @Override
                public void onCountryUpdated(@NotNull Country country) {
                }

                @Override
                public void onUpdateCompleted() {
                }
            });

            VisionArView visionArView = findViewById(R.id.mapbox_ar_view);

            // Create VisionArManager.
            VisionArManager.create(VisionManager.INSTANCE);
            visionArView.setArManager(VisionArManager.INSTANCE);
            setArRenderOptions(visionArView);

            visionManagerWasInit = true;
        }
    }

    private void stopVisionManager() {
        if (visionManagerWasInit) {
            VisionArManager.destroy();
            VisionManager.stop();
            VisionManager.destroy();

            visionManagerWasInit = false;
        }
    }
    private void startNavigation() {
        if (allPermissionsGranted() && !navigationWasStarted) {
            // Initialize navigation with your Mapbox access token.
            mapboxNavigation = new MapboxNavigation(
                    this,
                    getString(R.string.mapbox_access_token),
                    MapboxNavigationOptions.builder().build()
            );

            // Initialize route fetcher with your Mapbox access token.
            routeFetcher = new RouteFetcher(this, getString(R.string.mapbox_access_token));
            routeFetcher.addRouteListener(this);

            locationEngine = LocationEngineProvider.getBestLocationEngine(this);

            LocationEngineRequest arLocationEngineRequest = new LocationEngineRequest.Builder(0)
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                    .setFastestInterval(1000)
                    .build();

            locationCallback = new LocationEngineCallback<LocationEngineResult>() {
                @Override
                public void onSuccess(LocationEngineResult result) {

                }

                @Override
                public void onFailure(@NonNull Exception exception) {

                }
            };

            try {
                locationEngine.requestLocationUpdates(arLocationEngineRequest, locationCallback, Looper.getMainLooper());
            } catch (SecurityException se) {
                VisionLogger.Companion.e(TAG, se.toString());
            }

            initDirectionsRoute();

            // Route need to be reestablished if off route happens.
            mapboxNavigation.addOffRouteListener(this);
            mapboxNavigation.addProgressChangeListener(this);

            navigationWasStarted = true;
        }
    }

    private void stopNavigation() {
        if (navigationWasStarted) {
            locationEngine.removeLocationUpdates(locationCallback);

            mapboxNavigation.removeProgressChangeListener(this);
            mapboxNavigation.removeOffRouteListener(this);
            mapboxNavigation.stopNavigation();

            navigationWasStarted = false;
        }
    }
    private void initDirectionsRoute() {
        // Get route from predefined points.
        DirectionsRoute route = FullscreenActivity.currentRoute;
        mapboxNavigation.startNavigation(route);

        // Set route progress.
        VisionArManager.setRoute(new Route(
                getRoutePoints(route),
                route.duration().floatValue(),
                "",
                ""
        ));
        /*
        NavigationRoute.builder(this)
                .accessToken(getString(R.string.mapbox_access_token))
                .origin(ROUTE_ORIGIN)
                .destination(ROUTE_DESTINATION)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if (response.body() == null || response.body().routes().isEmpty()) {
                            return;
                        }

                        // Start navigation session with retrieved route.
                        DirectionsRoute route = response.body().routes().get(0);
                        mapboxNavigation.startNavigation(route);

                        // Set route progress.
                        VisionArManager.setRoute(new Route(
                                getRoutePoints(route),
                                route.duration().floatValue(),
                                "",
                                ""
                        ));
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                    }
                });
        */
    }
    @Override
    public void onErrorReceived(Throwable throwable) {
        if (throwable != null) {
            throwable.printStackTrace();
        }

        mapboxNavigation.stopNavigation();
        Toast.makeText(this, "Can not calculate the route requested", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResponseReceived(@NotNull DirectionsResponse response, RouteProgress routeProgress) {
        mapboxNavigation.stopNavigation();
        if (response.routes().isEmpty()) {
            Toast.makeText(this, "Can not calculate the route requested", Toast.LENGTH_SHORT).show();
        } else {
            DirectionsRoute route = response.routes().get(0);

            mapboxNavigation.startNavigation(route);

            // Set route progress.
            VisionArManager.setRoute(new Route(
                    getRoutePoints(route),
                    (float) routeProgress.durationRemaining(),
                    "",
                    ""
            ));
        }
    }

    @Override
    public void onProgressChange(Location location, RouteProgress routeProgress) {
        lastRouteProgress = routeProgress;
    }

    @Override
    public void userOffRoute(Location location) {
        routeFetcher.findRouteFromRouteProgress(location, lastRouteProgress);
    }
    private RoutePoint[] getRoutePoints(@NotNull DirectionsRoute route) {
        ArrayList<RoutePoint> routePoints = new ArrayList<>();

        List<RouteLeg> legs = route.legs();
        if (legs != null) {
            for (RouteLeg leg : legs) {

                List<LegStep> steps = leg.steps();
                if (steps != null) {
                    for (LegStep step : steps) {
                        RoutePoint point = new RoutePoint((new GeoCoordinate(
                                step.maneuver().location().latitude(),
                                step.maneuver().location().longitude()
                        )), mapToManeuverType(step.maneuver().type()));

                        routePoints.add(point);

                        List<Point> geometryPoints = buildStepPointsFromGeometry(step.geometry());
                        for (Point geometryPoint : geometryPoints) {
                            point = new RoutePoint((new GeoCoordinate(
                                    geometryPoint.latitude(),
                                    geometryPoint.longitude()
                            )), ManeuverType.None);

                            routePoints.add(point);
                        }
                    }
                }
            }
        }

        return routePoints.toArray(new RoutePoint[0]);
    }

    private List<Point> buildStepPointsFromGeometry(String geometry) {
        return PolylineUtils.decode(geometry, Constants.PRECISION_6);
    }

    private ManeuverType mapToManeuverType(@Nullable String maneuver) {
        if (maneuver == null) {
            return ManeuverType.None;
        }
        switch (maneuver) {
            case "turn":
                return ManeuverType.Turn;
            case "depart":
                return ManeuverType.Depart;
            case "arrive":
                return ManeuverType.Arrive;
            case "merge":
                return ManeuverType.Merge;
            case "on ramp":
                return ManeuverType.OnRamp;
            case "off ramp":
                return ManeuverType.OffRamp;
            case "fork":
                return ManeuverType.Fork;
            case "roundabout":
                return ManeuverType.Roundabout;
            case "exit roundabout":
                return ManeuverType.RoundaboutExit;
            case "end of road":
                return ManeuverType.EndOfRoad;
            case "new name":
                return ManeuverType.NewName;
            case "continue":
                return ManeuverType.Continue;
            case "rotary":
                return ManeuverType.Rotary;
            case "roundabout turn":
                return ManeuverType.RoundaboutTurn;
            case "notification":
                return ManeuverType.Notification;
            case "exit rotary":
                return ManeuverType.RotaryExit;
            default:
                return ManeuverType.None;
        }
    }

}
