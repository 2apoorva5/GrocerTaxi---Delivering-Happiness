package com.application.grocertaxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.application.grocertaxi.Interfaces.ICityLoadListener;
import com.application.grocertaxi.Model.City;
import com.application.grocertaxi.Model.Product;
import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.firebase.ui.firestore.paging.FirestorePagingAdapter;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.firebase.ui.firestore.paging.LoadingState;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.mateware.snacky.Snacky;
import maes.tech.intentanim.CustomIntent;
import pl.droidsonroids.gif.GifImageView;

public class ChooseCityActivity extends AppCompatActivity {

    private ImageView closeBtn, speechToText;
    private TextView chooseCityTitle;
    private GifImageView chooseCityGif;
    private EditText inputCitySearchField;
    private RecyclerView recyclerCity;
    private ProgressBar progressBar;

    private CollectionReference citiesRef, userRef;
    private FirestoreRecyclerAdapter<City, CityViewHolder> cityAdapter;

    private PreferenceManager preferenceManager;
    private static int LAST_POSITION = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_city);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(ChooseCityActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(ChooseCityActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        initViews();
        initFirebase();
        setActionOnViews();
    }

    private void initViews() {
        closeBtn = findViewById(R.id.close_btn);
        chooseCityTitle = findViewById(R.id.choose_city_title);
        chooseCityGif = findViewById(R.id.choose_city_gif);
        inputCitySearchField = findViewById(R.id.input_city_search_field);
        speechToText = findViewById(R.id.speech_to_text);
        recyclerCity = findViewById(R.id.recycler_city);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void initFirebase() {
        citiesRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_CITIES);
        userRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS);
    }

    private void setActionOnViews() {
        closeBtn.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });

        String name = preferenceManager.getString(Constants.KEY_USER_NAME);
        String[] splitName = name.split(" ", 2);

        chooseCityTitle.setText(String.format("Hi, %s", splitName[0]));

        KeyboardVisibilityEvent.setEventListener(ChooseCityActivity.this, isOpen -> {
            if (!isOpen) {
                inputCitySearchField.clearFocus();
            }
        });

        progressBar.setVisibility(View.VISIBLE);

        speechToText.setOnClickListener(view -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, String.format("Hi, %s! What city you're in?", splitName[0]));

            try {
                startActivityForResult(intent, 123);
            } catch (ActivityNotFoundException e) {
                Alerter.create(ChooseCityActivity.this)
                        .setText("Whoa! Something broke. Try again!")
                        .setTextAppearance(R.style.AlertText)
                        .setBackgroundColorRes(R.color.errorColor)
                        .setIcon(R.drawable.ic_error)
                        .setDuration(3000)
                        .enableIconPulse(true)
                        .enableVibration(true)
                        .disableOutsideTouch()
                        .enableProgress(true)
                        .setProgressColorInt(getColor(android.R.color.white))
                        .show();
                return;
            }
        });

        inputCitySearchField.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                chooseCityGif.setForeground(new ColorDrawable(Color.parseColor("#80000000")));
            } else {
                chooseCityGif.setForeground(new ColorDrawable(Color.parseColor("#00000000")));
            }
        });

        inputCitySearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                Query updatedQuery;
                if (s.toString().isEmpty()) {
                    updatedQuery = citiesRef.orderBy("name", Query.Direction.ASCENDING);
                } else {
                    updatedQuery = citiesRef.orderBy("searchKeyword", Query.Direction.ASCENDING)
                            .startAt(s.toString().toLowerCase().trim()).endAt(s.toString().toLowerCase().trim() + "\uf8ff");
                }

                FirestoreRecyclerOptions<City> updatedOptions = new FirestoreRecyclerOptions.Builder<City>()
                        .setLifecycleOwner(ChooseCityActivity.this)
                        .setQuery(updatedQuery, City.class)
                        .build();

                cityAdapter.updateOptions(updatedOptions);
            }

            @Override
            public void afterTextChanged(Editable s) {
                inputCitySearchField.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        UIUtil.hideKeyboard(ChooseCityActivity.this);
                        Query updatedQuery;
                        if (s.toString().isEmpty()) {
                            updatedQuery = citiesRef.orderBy("name", Query.Direction.ASCENDING);
                        } else {
                            updatedQuery = citiesRef.orderBy("searchKeyword", Query.Direction.ASCENDING)
                                    .startAt(s.toString().toLowerCase().trim()).endAt(s.toString().toLowerCase().trim() + "\uf8ff");
                        }

                        FirestoreRecyclerOptions<City> updatedOptions = new FirestoreRecyclerOptions.Builder<City>()
                                .setLifecycleOwner(ChooseCityActivity.this)
                                .setQuery(updatedQuery, City.class)
                                .build();

                        cityAdapter.updateOptions(updatedOptions);
                        return true;
                    }
                    return false;
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 123:
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    inputCitySearchField.setText(result.get(0));
                    inputCitySearchField.clearFocus();
                }
                break;
        }
    }

    private void loadCities() {
        Query query = citiesRef.orderBy("name", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<City> options = new FirestoreRecyclerOptions.Builder<City>()
                .setLifecycleOwner(ChooseCityActivity.this)
                .setQuery(query, City.class)
                .build();

        cityAdapter = new FirestoreRecyclerAdapter<City, CityViewHolder>(options) {

            @NonNull
            @Override
            public CityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_city_item, parent, false);
                return new CityViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull CityViewHolder holder, int position, @NonNull City model) {
                holder.cityName.setText(model.getName());

                holder.clickListener.setOnClickListener(view -> {
                    UIUtil.hideKeyboard(ChooseCityActivity.this);
                    notifyDataSetChanged();
                    progressBar.setVisibility(View.VISIBLE);

                    userRef.document(preferenceManager.getString(Constants.KEY_USER_ID))
                            .update(Constants.KEY_USER_CITY, model.getName())
                            .addOnSuccessListener(aVoid -> {
                                progressBar.setVisibility(View.GONE);

                                preferenceManager.putString(Constants.KEY_USER_CITY, model.getName());

                                startActivity(new Intent(ChooseCityActivity.this, ChooseLocalityActivity.class));
                                CustomIntent.customType(ChooseCityActivity.this, "left-to-right");
                            }).addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Alerter.create(ChooseCityActivity.this)
                                        .setText("Whoa! Something broke. Try again!")
                                        .setTextAppearance(R.style.AlertText)
                                        .setBackgroundColorRes(R.color.errorColor)
                                        .setIcon(R.drawable.ic_error)
                                        .setDuration(3000)
                                        .enableIconPulse(true)
                                        .enableVibration(true)
                                        .disableOutsideTouch()
                                        .enableProgress(true)
                                        .setProgressColorInt(getColor(android.R.color.white))
                                        .show();
                                return;
                            });
                });

                setAnimation(holder.itemView, position);
            }

            public void setAnimation(View viewToAnimate, int position) {
                if (position > LAST_POSITION) {
                    ScaleAnimation scaleAnimation = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f,
                            Animation.RELATIVE_TO_SELF, 0.5f,
                            Animation.RELATIVE_TO_SELF, 0.5f);
                    scaleAnimation.setDuration(1000);

                    viewToAnimate.setAnimation(scaleAnimation);
                    LAST_POSITION = position;
                }
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();

                progressBar.setVisibility(View.GONE);
                Snacky.builder()
                        .setActivity(ChooseCityActivity.this)
                        .setText(String.format("We're currently servicing in %d cities.", getItemCount()))
                        .setDuration(Snacky.LENGTH_INDEFINITE)
                        .info()
                        .show();
            }

            @Override
            public void onError(@NonNull FirebaseFirestoreException e) {
                super.onError(e);
                Alerter.create(ChooseCityActivity.this)
                        .setText("Whoa! Something Broke. Try again!")
                        .setTextAppearance(R.style.AlertText)
                        .setBackgroundColorRes(R.color.errorColor)
                        .setIcon(R.drawable.ic_error)
                        .setDuration(3000)
                        .enableIconPulse(true)
                        .enableVibration(true)
                        .disableOutsideTouch()
                        .enableProgress(true)
                        .setProgressColorInt(getColor(android.R.color.white))
                        .show();
            }
        };

        cityAdapter.notifyDataSetChanged();

        recyclerCity.setHasFixedSize(true);
        recyclerCity.setLayoutManager(new LinearLayoutManager(ChooseCityActivity.this));
        recyclerCity.setAdapter(cityAdapter);
    }

    public static class CityViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout clickListener;
        TextView cityName;

        public CityViewHolder(@NonNull View itemView) {
            super(itemView);

            clickListener = itemView.findViewById(R.id.click_listener);
            cityName = itemView.findViewById(R.id.item_city_name);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        loadCities();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        CustomIntent.customType(ChooseCityActivity.this, "fadein-to-fadeout");
    }
}