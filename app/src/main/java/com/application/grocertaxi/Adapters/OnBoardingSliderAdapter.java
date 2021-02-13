package com.application.grocertaxi.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.PagerAdapter;

import com.application.grocertaxi.R;

public class OnBoardingSliderAdapter extends PagerAdapter {
    Context context;
    LayoutInflater layoutInflater;

    public OnBoardingSliderAdapter(Context context) {
        this.context = context;
    }

    int images[] = {
            R.drawable.illustration_onboarding1,
            R.drawable.illustration_onboarding2,
            R.drawable.illustration_onboarding3,
            R.drawable.illustration_onboarding4,
            R.drawable.illustration_onboarding5
    };

    int titles[] = {
            R.string.onboarding_title1,
            R.string.onboarding_title2,
            R.string.onboarding_title3,
            R.string.onboarding_title4,
            R.string.onboarding_title5
    };

    int descriptions[] = {
            R.string.onboarding_description1,
            R.string.onboarding_description2,
            R.string.onboarding_description3,
            R.string.onboarding_description4,
            R.string.onboarding_description5
    };

    @Override
    public int getCount() {
        return titles.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == (ConstraintLayout) object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {

        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.layout_onboarding_slider, container, false);

        ImageView sliderImage = view.findViewById(R.id.illustration_on_boarding);
        TextView sliderTitle = view.findViewById(R.id.title_on_boarding);
        TextView sliderDesc = view.findViewById(R.id.description_on_boarding);

        sliderImage.setImageResource(images[position]);
        sliderTitle.setText(titles[position]);
        sliderDesc.setText(descriptions[position]);

        container.addView(view);

        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((ConstraintLayout) object);
    }
}
