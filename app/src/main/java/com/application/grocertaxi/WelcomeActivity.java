package com.application.grocertaxi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import maes.tech.intentanim.CustomIntent;

public class WelcomeActivity extends AppCompatActivity {

    private ConstraintLayout signUpBtn, signInBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());

        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(WelcomeActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        initViews();
        setActionOnViews();
    }

    private void initViews() {
        signUpBtn = findViewById(R.id.sign_up_btn);
        signInBtn = findViewById(R.id.sign_in_btn);
    }

    private void setActionOnViews() {
        signUpBtn.setOnClickListener(view -> {
            startActivity(new Intent(WelcomeActivity.this, SignUpActivity.class));
            CustomIntent.customType(WelcomeActivity.this, "bottom-to-up");
            finish();
        });

        signInBtn.setOnClickListener(view -> {
            startActivity(new Intent(WelcomeActivity.this, SignInActivity.class));
            CustomIntent.customType(WelcomeActivity.this, "bottom-to-up");
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        KeyboardVisibilityEvent.setEventListener(WelcomeActivity.this, isOpen -> {
            if (isOpen) {
                UIUtil.hideKeyboard(WelcomeActivity.this);
            }
        });
        finishAffinity();
    }
}