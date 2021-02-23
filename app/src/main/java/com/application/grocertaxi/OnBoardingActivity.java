package com.application.grocertaxi;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.ViewPager;

import com.application.grocertaxi.Adapters.OnBoardingSliderAdapter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import maes.tech.intentanim.CustomIntent;

public class OnBoardingActivity extends AppCompatActivity {

    private TextView skipBtn, textNextBtn;
    private ViewPager sliderViewPager;
    private LinearLayout indicatorsLayout;
    private ConstraintLayout nextBtn;

    int CURRENT_POSITION;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        initViews();
        setActionOnViews();

        OnBoardingSliderAdapter onBoardingSliderAdapter = new OnBoardingSliderAdapter(OnBoardingActivity.this);
        sliderViewPager.setAdapter(onBoardingSliderAdapter);
        sliderViewPager.addOnPageChangeListener(onPageChangeListener);

        createIndicatorsLayout(0);
    }

    private void initViews() {
        skipBtn = findViewById(R.id.skip_btn);
        sliderViewPager = findViewById(R.id.view_pager_on_board);
        indicatorsLayout = findViewById(R.id.indicators_layout);
        nextBtn = findViewById(R.id.next_btn);
        textNextBtn = findViewById(R.id.text_next_btn);
    }

    private void setActionOnViews() {
        skipBtn.setOnClickListener(v -> {
            startActivity(new Intent(OnBoardingActivity.this, WelcomeActivity.class));
            CustomIntent.customType(OnBoardingActivity.this, "fadein-to-fadeout");
            finish();
        });

        nextBtn.setOnClickListener(v -> sliderViewPager.setCurrentItem(CURRENT_POSITION + 1));
    }

    private void createIndicatorsLayout(int position) {
        TextView[] indicators = new TextView[5];
        indicatorsLayout.removeAllViews();

        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new TextView(OnBoardingActivity.this);
            indicators[i].setText(Html.fromHtml("&#183;"));
            indicators[i].setTextSize(48);
            indicators[i].setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            indicators[i].setTextColor(getColor(R.color.colorViews));

            indicatorsLayout.addView(indicators[i]);
        }

        indicators[position].setText(Html.fromHtml("&#183;"));
        indicators[position].setTextSize(48);
        indicators[position].setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        indicators[position].setTextColor(getColor(R.color.colorAccent));
    }

    ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            createIndicatorsLayout(position);
            CURRENT_POSITION = position;
            if (position == 0) {
                textNextBtn.setText("NEXT");
                skipBtn.setVisibility(View.VISIBLE);
                skipBtn.setEnabled(true);
                nextBtn.setOnClickListener(v -> sliderViewPager.setCurrentItem(position + 1));
            } else if (position == 1) {
                textNextBtn.setText("NEXT");
                skipBtn.setVisibility(View.VISIBLE);
                skipBtn.setEnabled(true);
                nextBtn.setOnClickListener(v -> sliderViewPager.setCurrentItem(position + 1));
            } else if (position == 2) {
                textNextBtn.setText("NEXT");
                skipBtn.setVisibility(View.VISIBLE);
                skipBtn.setEnabled(true);
                nextBtn.setOnClickListener(v -> sliderViewPager.setCurrentItem(position + 1));
            } else if (position == 3) {
                textNextBtn.setText("NEXT");
                skipBtn.setVisibility(View.VISIBLE);
                skipBtn.setEnabled(true);
                nextBtn.setOnClickListener(v -> sliderViewPager.setCurrentItem(position + 1));
            } else if (position == 4) {
                textNextBtn.setText("START");
                skipBtn.setVisibility(View.INVISIBLE);
                skipBtn.setEnabled(false);
                nextBtn.setOnClickListener(v -> {
                    startActivity(new Intent(OnBoardingActivity.this, WelcomeActivity.class));
                    CustomIntent.customType(OnBoardingActivity.this, "fadein-to-fadeout");
                    finish();
                });
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        KeyboardVisibilityEvent.setEventListener(OnBoardingActivity.this, isOpen -> {
            if (isOpen) {
                UIUtil.hideKeyboard(OnBoardingActivity.this);
            }
        });
        finishAffinity();
    }
}