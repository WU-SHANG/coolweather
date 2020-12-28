package com.example.coolweather;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.coolweather.gson.Forecast;
import com.example.coolweather.gson.Weather;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private ScrollView sv_weather_layout;
    private TextView tv_title_city;
    private TextView tv_title_update_time;
    private TextView tv_degree_text;
    private TextView tv_weather_info_text;
    private LinearLayout ll_forecast_layout;
    private TextView tv_aqi_text;
    private TextView tv_pm25_text;
    private TextView tv_comfort_text;
    private TextView tv_car_wash_text;
    private TextView tv_sport_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        initView();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        if (weatherString != null) {
            //有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            showWeatherInfo(weather);
        } else {
            //无缓存时去服务器查询天气
            String weatherId = getIntent().getStringExtra("weather_id");
            sv_weather_layout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }
    }

    private void initView() {
        sv_weather_layout = findViewById(R.id.sv_weather_layout);
        tv_title_city = findViewById(R.id.tv_title_city);
        tv_title_update_time = findViewById(R.id.tv_title_update_time);
        tv_degree_text = findViewById(R.id.tv_degree_text);
        tv_weather_info_text = findViewById(R.id.tv_weather_info_text);
        ll_forecast_layout = findViewById(R.id.ll_forecast_layout);
        tv_aqi_text = findViewById(R.id.tv_aqi_text);
        tv_pm25_text = findViewById(R.id.tv_pm25_text);
        tv_comfort_text = findViewById(R.id.tv_comfort_text);
        tv_car_wash_text = findViewById(R.id.tv_car_wash_text);
        tv_sport_text = findViewById(R.id.tv_sport_text);
    }

    /**
     * 根据天气id请求城市天气信息
     * @param weatherId
     */
    public void requestWeather(final String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId
                + "&key=" + getString(R.string.key);
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(()->{
                    Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(()->{
                    if (weather != null && "ok".equals(weather.status)) {
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                        editor.putString("weather", responseText);
                        editor.apply();
                        showWeatherInfo(weather);
                    } else {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }

    /**
     * 处理并展示Weather实体类中的数据
     * @param weather
     */
    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        tv_title_city.setText(cityName);
        tv_title_update_time.setText(updateTime);
        tv_degree_text.setText(degree);
        tv_weather_info_text.setText(weatherInfo);
        ll_forecast_layout.removeAllViews();
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, ll_forecast_layout, false);
            TextView tv_date_text = view.findViewById(R.id.tv_date_text);
            TextView tv_info_text = view.findViewById(R.id.tv_info_text);
            TextView tv_max_text = view.findViewById(R.id.tv_max_text);
            TextView tv_min_text = view.findViewById(R.id.tv_min_text);
            tv_date_text.setText(forecast.date);
            tv_info_text.setText(forecast.more.info);
            tv_max_text.setText(forecast.temperature.max);
            tv_min_text.setText(forecast.temperature.min);
            ll_forecast_layout.addView(view);
        }
        if (weather.aqi != null) {
            tv_aqi_text.setText(weather.aqi.city.aqi);
            tv_pm25_text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运动建议：" + weather.suggestion.sport.info;
        tv_comfort_text.setText(comfort);
        tv_car_wash_text.setText(carWash);
        tv_sport_text.setText(sport);
        sv_weather_layout.setVisibility(View.VISIBLE);
    }
}