package com.varun.example.arnavigation;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;


public interface weatherService {
    @GET("data/2.5/weather?")
    Call<weatherResponse> getCurrentWeatherData(@Query("lat") String lat, @Query("lon") String lon, @Query("appid") String app_id);
}
       /*
        client = new OkHttpClient();
        HttpUrl.Builder urlBuilder = HttpUrl.parse(BaseUrl + "data/2.5/weather?").newBuilder();
        urlBuilder.addQueryParameter("lat", lat);
        urlBuilder.addQueryParameter("lon", lon);
        urlBuilder.addQueryParameter("appid", AppId);
        String url = urlBuilder.build().toString();

        Request request = new Request.Builder()
                .url(url)
                .build();
        //okhttp3.Response response = client.newCall(request).execute();

        client.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.d("weatherDebug",e.toString());
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
                    Log.d("weatherDebug",response.body().toString());
                    String jsonData = response.body().string();
                    try {
                        JSONObject Jobject = new JSONObject(jsonData);
                        int ii = 0;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }
        });

         */