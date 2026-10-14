package com.mt.browser;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_splash);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent[] st = "downloads".equals(getSharedPreferences("mt", 0).getString("screen", "")) ? new Intent[]{new Intent(this, MainActivity.class), new Intent(this, DownloadsActivity.class)} : new Intent[]{new Intent(this, MainActivity.class)}; startActivities(st);
            finish();
        }, 700);
    }
}
