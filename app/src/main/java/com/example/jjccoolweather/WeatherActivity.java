package com.example.jjccoolweather;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.jjccoolweather.gson.Forecast;
import com.example.jjccoolweather.gson.Weather;
import com.example.jjccoolweather.service.AutoUpdateService;
import com.example.jjccoolweather.util.HttpUtil;
import com.example.jjccoolweather.util.Utility;

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
    private ImageView iv_bing_pic;

    public SwipeRefreshLayout swipeRefresh;
    private String mWeatherId;

    public DrawerLayout drawer_layout;
    private Button btn_nav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //版本号21即5.0以上系统才支持。改变系统UI显示，活动的布局会显示在状态栏上面，
        //然后将状态栏设置成透明色
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);

        initView();

        swipeRefresh.setColorSchemeResources(R.color.design_default_color_primary);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        String bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(iv_bing_pic);
        } else {
            loadBingPic();
        }
        if (weatherString != null) {
            //有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else {
            //无缓存时去服务器查询天气
            mWeatherId = getIntent().getStringExtra("weather_id");
            sv_weather_layout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }

        //下拉刷新，重新请求服务器
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });

        btn_nav.setOnClickListener(v -> {
            //打开滑动菜单
            drawer_layout.openDrawer(GravityCompat.START);
        });
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
        iv_bing_pic = findViewById(R.id.iv_bing_pic);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        drawer_layout = findViewById(R.id.drawer_layout);
        btn_nav = findViewById(R.id.btn_nav);
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
                    e.printStackTrace();
                    Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                    swipeRefresh.setRefreshing(false);
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
                        mWeatherId = weather.basic.weatherId;
                        showWeatherInfo(weather);
                    } else {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                    }
                    swipeRefresh.setRefreshing(false);
                });
            }
        });
        //每次请求天气信息时刷新图片
        loadBingPic();
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

        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }

    /**
     * 加载必应每日一图
     */
    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_bic", bingPic);
                editor.apply();
                runOnUiThread(()->{
                    Glide.with(WeatherActivity.this).load(bingPic).into(iv_bing_pic);
                });
            }
        });
    }
}