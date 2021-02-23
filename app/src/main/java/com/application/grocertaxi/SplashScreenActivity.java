package com.application.grocertaxi;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import maes.tech.intentanim.CustomIntent;

public class SplashScreenActivity extends AppCompatActivity {

    private SharedPreferences onBoardPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    @Override
    protected void onStart() {
        super.onStart();
        int SPLASH_TIMER = 3000;

        onBoardPreference = getSharedPreferences("onBoardPreference", MODE_PRIVATE);
        final boolean isFirstTime = onBoardPreference.getBoolean("firstTime", true);

        new Handler().postDelayed(() -> {
            if (isFirstTime) {
                @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = onBoardPreference.edit();
                editor.putBoolean("firstTime", false);
                editor.apply();

                startActivity(new Intent(SplashScreenActivity.this, OnBoardingActivity.class));
            } else {
                startActivity(new Intent(SplashScreenActivity.this, WelcomeActivity.class));
            }
            CustomIntent.customType(SplashScreenActivity.this, "fadein-to-fadeout");
            finish();
        }, SPLASH_TIMER);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        KeyboardVisibilityEvent.setEventListener(SplashScreenActivity.this, isOpen -> {
            if (isOpen) {
                UIUtil.hideKeyboard(SplashScreenActivity.this);
            }
        });
        finishAffinity();
    }
}