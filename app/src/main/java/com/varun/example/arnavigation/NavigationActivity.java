package com.varun.example.arnavigation;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.Style;
//import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.listeners.BannerInstructionsListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.InstructionListListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.SpeechAnnouncementListener;
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechAnnouncement;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

public class NavigationActivity extends AppCompatActivity implements OnNavigationReadyCallback,
        NavigationListener, ProgressChangeListener, InstructionListListener, SpeechAnnouncementListener,
        BannerInstructionsListener {

    private static final String TAG = "NAVIGATION_ACTIVITY";
    private static Point ORIGIN = Point.fromLngLat(-77.03194990754128, 38.909664963450105);
    private static Point DESTINATION = Point.fromLngLat(-77.0270025730133, 38.91057077063121);
    private static final int INITIAL_ZOOM = 16;
    public static String BaseUrl = "https://api.openweathermap.org/";
    public static String AppId = "8b169c96067f6d0d3b59c3733add1d4b";
    public static int count;
    public static final String originLat = "Origin_Lat";
    public static final String originLong = "Origin_Long";
    public static final String destLat = "Dest_lat";
    public static final String destLong = "Dest_Long";
    private Drawable weatherIcon = null;
    private NavigationView navigationView;
    private View spacerWeather;
    private View spacer;
    private TextView speedWidget;
    private TextView weatherWidget;
    private FloatingActionButton fabNightModeToggle;
    private FloatingActionButton fabStyleToggle;
    private OkHttpClient client;
    private String iconid;
    private boolean bottomSheetVisible = true;
    private boolean instructionListShown = false;

    private StyleCycle styleCycle = new StyleCycle();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_AppCompat_Light_NoActionBar);
        initNightMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        navigationView = findViewById(R.id.navigationView);
        fabNightModeToggle = findViewById(R.id.fabToggleNightMode);
        fabStyleToggle = findViewById(R.id.fabToggleStyle);
        speedWidget = findViewById(R.id.speed_limit);
        weatherWidget = findViewById(R.id.weather_view);
        spacer = findViewById(R.id.spacer);
        spacerWeather = findViewById(R.id.spacerWeather);
        setSpeedWidgetAnchor(R.id.summaryBottomSheet);
        setWeatherWidgetAnchor(R.id.speed_limit);

        ORIGIN = Point.fromLngLat(getIntent().getDoubleExtra(originLong,ORIGIN.longitude()),getIntent().getDoubleExtra(originLat,ORIGIN.latitude()));
        DESTINATION = Point.fromLngLat(getIntent().getDoubleExtra(destLong,DESTINATION.longitude()),getIntent().getDoubleExtra(destLat,DESTINATION.latitude()));
        CameraPosition initialPosition = new CameraPosition.Builder()
                .target(new LatLng(ORIGIN.latitude(), ORIGIN.longitude()))
                .zoom(INITIAL_ZOOM)
                .build();
        navigationView.onCreate(savedInstanceState);
        navigationView.initialize(this, initialPosition);
    }

    @Override
    public void onNavigationReady(boolean isRunning) {
        fetchRoute();
    }

    @Override
    public void onStart() {
        super.onStart();
        navigationView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        navigationView.onResume();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        navigationView.onLowMemory();
    }

    @Override
    public void onBackPressed() {
// If the navigation view didn't need to do anything, call super
        if (!navigationView.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        navigationView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        navigationView.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        navigationView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        navigationView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        navigationView.onDestroy();
        FullscreenActivity.clearMap("origin");
        FullscreenActivity.clearMap("destination");

        /*
        if (isFinishing()) {
            saveNightModeToPreferences(AppCompatDelegate.MODE_NIGHT_AUTO);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
        }

         */
    }

    @Override
    public void onCancelNavigation() {
// Navigation canceled, finish the activity
        finish();
    }

    @Override
    public void onNavigationFinished() {
// Intentionally empty
    }

    @Override
    public void onNavigationRunning() {
// Intentionally empty
    }

    @Override
    public void onProgressChange(Location location, RouteProgress routeProgress) {
        setSpeed(location);

        if(count == 10){
            try {
                getCurrentData(location);
            } catch (IOException e) {
                e.printStackTrace();
            }
            count = 0;
        }
        count++;


    }

    @Override
    public void onInstructionListVisibilityChanged(boolean shown) {
        instructionListShown = shown;
        speedWidget.setVisibility(shown ? View.GONE : View.VISIBLE);
        weatherWidget.setVisibility(shown ? View.GONE : View.VISIBLE);
        if (instructionListShown) {
            fabNightModeToggle.hide();
        } else if (bottomSheetVisible) {
            fabNightModeToggle.show();
        }
    }
/*
    @Override
    public VoiceInstructions willVoice(VoiceInstructions announcement) {
        return VoiceInstructions.builder().announcement("All announcements will be the same.").build();
    }
*/
    @Override
    public BannerInstructions willDisplay(BannerInstructions instructions) {
        return instructions;
    }

    private void startNavigation(DirectionsRoute directionsRoute) {
        NavigationViewOptions.Builder options =
                NavigationViewOptions.builder()
                        .navigationListener(this)
                        .directionsRoute(directionsRoute)
                        .shouldSimulateRoute(true)
                        .progressChangeListener(this)
                        .instructionListListener(this)
                        .speechAnnouncementListener(this)
                        .bannerInstructionsListener(this)
                        .offlineRoutingTilesPath(obtainOfflineDirectory())
                        .offlineRoutingTilesVersion(obtainOfflineTileVersion());
        setBottomSheetCallback(options);
        setupStyleFab();
        setupNightModeFab();

        navigationView.startNavigation(options.build());
    }

    private String obtainOfflineDirectory() {
        File offline = Environment.getExternalStoragePublicDirectory("Offline");
        if (!offline.exists()) {
            Timber.d("Offline directory does not exist");
            offline.mkdirs();
        }
        return offline.getAbsolutePath();
    }

    private String obtainOfflineTileVersion() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.offline_version_key), "");
    }

    private void fetchRoute() {
        DirectionsRoute directionsRoute = FullscreenActivity.currentRoute;
        startNavigation(directionsRoute);
        /*
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(ORIGIN)
                .destination(DESTINATION)
                .alternatives(true)
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
                        DirectionsRoute directionsRoute = FullscreenActivity.currentRoute;//response.body().routes().get(0);
                        String org = ORIGIN.toString();
                        String dest = DESTINATION.toString();
                        double dist = directionsRoute.distance();
                        double dur = directionsRoute.duration();
                        startNavigation(directionsRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Timber.e(t, t.getMessage());
                    }
                });
        */
    }

    /**
     * Sets the anchor of the spacer for the speed widget, thus setting the anchor for the speed widget
     * (The speed widget is anchored to the spacer, which is there because padding between items and
     * their anchors in CoordinatorLayouts is finicky.
     *
     * @param res resource for view of which to anchor the spacer
     */
    private void setSpeedWidgetAnchor(@IdRes int res) {
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) spacer.getLayoutParams();
        layoutParams.setAnchorId(res);
        spacer.setLayoutParams(layoutParams);
    }

    private void setWeatherWidgetAnchor(@IdRes int res) {
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) spacerWeather.getLayoutParams();
        layoutParams.setAnchorId(res);
        spacerWeather.setLayoutParams(layoutParams);
    }

    private void setBottomSheetCallback(NavigationViewOptions.Builder options) {
        options.bottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        bottomSheetVisible = false;
                        fabNightModeToggle.hide();
                        setSpeedWidgetAnchor(R.id.recenterBtn);
                        setWeatherWidgetAnchor(R.id.recenterBtn);
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        bottomSheetVisible = true;
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        if (!bottomSheetVisible) {
// View needs to be anchored to the bottom sheet before it is finished expanding
// because of the animation
                            fabNightModeToggle.show();
                            setSpeedWidgetAnchor(R.id.summaryBottomSheet);
                            setWeatherWidgetAnchor(R.id.speed_limit);
                        }
                        break;
                    default:
                        return;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });
    }

    private void setupNightModeFab() {
        fabNightModeToggle.setOnClickListener(view -> toggleNightMode());
    }

    private void setupStyleFab() {
        fabStyleToggle.setOnClickListener(view ->
                navigationView.retrieveNavigationMapboxMap().retrieveMap().setStyle(styleCycle.getNextStyle()));
    }

    @Override
    public SpeechAnnouncement willVoice(SpeechAnnouncement announcement) {
        return null;
    }

    private static class StyleCycle {
        private static final String[] STYLES = new String[]{
                Style.MAPBOX_STREETS,
                Style.OUTDOORS,
                Style.LIGHT,
                Style.DARK,
                Style.SATELLITE_STREETS
        };

        private int index;

        private String getNextStyle() {
            index++;
            if (index == STYLES.length) {
                index = 0;
            }
            return getStyle();
        }

        private String getStyle() {
            return STYLES[index];
        }
    }

    private void toggleNightMode() {
        int currentNightMode = getCurrentNightMode();
        alternateNightMode(currentNightMode);
    }

    private void initNightMode() {
        int nightMode = retrieveNightModeFromPreferences();
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    private int getCurrentNightMode() {
        return getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
    }

    private void alternateNightMode(int currentNightMode) {
        int newNightMode;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            newNightMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else {
            newNightMode = AppCompatDelegate.MODE_NIGHT_YES;
        }
        saveNightModeToPreferences(newNightMode);
        recreate();
    }

    private int retrieveNightModeFromPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getInt(getString(R.string.current_night_mode), AppCompatDelegate.MODE_NIGHT_AUTO);
    }

    private void saveNightModeToPreferences(int nightMode) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(getString(R.string.current_night_mode), nightMode);
        editor.apply();
    }

    private void setSpeed(Location location) {
        String string = String.format("%d\nMPH", (int) (location.getSpeed() * 2.2369));
        int mphTextSize = getResources().getDimensionPixelSize(R.dimen.mph_text_size);
        int speedTextSize = getResources().getDimensionPixelSize(R.dimen.speed_text_size);

        SpannableString spannableString = new SpannableString(string);
        spannableString.setSpan(new AbsoluteSizeSpan(mphTextSize),
                string.length() - 4, string.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        spannableString.setSpan(new AbsoluteSizeSpan(speedTextSize),
                0, string.length() - 3, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        speedWidget.setText(spannableString);
        if (!instructionListShown) {
            speedWidget.setVisibility(View.VISIBLE);
        }
    }

    void getCurrentData(Location loc) throws IOException {
        String lat = String.valueOf(loc.getLatitude());
        String lon = String.valueOf(loc.getLongitude());

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BaseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        weatherService service = retrofit.create(weatherService.class);
        Call<weatherResponse> call = service.getCurrentWeatherData(lat, lon, AppId);
        call.enqueue(new Callback<weatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<weatherResponse> call, @NonNull Response<weatherResponse> response) {
                if (response.code() == 200) {
                    weatherResponse weatherResp = response.body();
                    assert weatherResp != null;
                    iconid = weatherResp.weather.get(0).icon;
                    double te = weatherResp.main.feels_like;
                    te = te - 273.15;
                    te = te * 9.0;
                    te = te/5.0;
                    te = te + 32;
                    //double tem = ((weatherResp.main.feels_like - 273.15) * (9/5)) + 32;
                    String temp = String.format("%.3f",te) + " F";
                    String data = temp + "\n" + weatherResp.name;
                    int cityTextSize = getResources().getDimensionPixelSize(R.dimen.city_text_size);
                    int tempTextSize = getResources().getDimensionPixelSize(R.dimen.temp_text_size);

                    loadWeatherIcon();

                    SpannableString spannableString = new SpannableString(data);
                    spannableString.setSpan(new AbsoluteSizeSpan(cityTextSize),
                            data.length() - weatherResp.name.length(), data.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                    spannableString.setSpan(new AbsoluteSizeSpan(tempTextSize),
                            0, data.length() - temp.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                    weatherWidget.setText(spannableString);
                    if(weatherIcon !=null){
                        weatherWidget.setBackground(weatherIcon);
                    }
                    if (!instructionListShown) {
                        weatherWidget.setVisibility(View.VISIBLE);
                    }

                }
                else{

                }
            }

            @Override
            public void onFailure(@NonNull Call<weatherResponse> call, @NonNull Throwable t) {
                Timber.d(t.getMessage());
            }
        });

    }

    public void loadWeatherIcon(){
        client = new OkHttpClient();
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://openweathermap.org/img/wn/" + iconid + "@2x.png").newBuilder();
        String url = urlBuilder.build().toString();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                if (!response.isSuccessful()) {
                    try {
                        throw new IOException("Unexpected code " + response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {

                    try {
                        Bitmap icon;
                        icon = BitmapFactory.decodeStream(response.body().byteStream());
                        weatherIcon = new BitmapDrawable(getResources(), icon);
                    } catch (Exception e) {
                        String ex = e.toString();
                        e.printStackTrace();
                    }

                }
            }
        });

    }

}