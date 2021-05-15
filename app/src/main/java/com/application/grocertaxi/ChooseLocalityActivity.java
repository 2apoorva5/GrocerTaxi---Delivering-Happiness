package com.application.grocertaxi;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.application.grocertaxi.Model.Locality;
import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.firebase.ui.firestore.paging.FirestorePagingAdapter;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.firebase.ui.firestore.paging.LoadingState;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.ArrayList;
import java.util.Locale;

import maes.tech.intentanim.CustomIntent;
import pl.droidsonroids.gif.GifImageView;

public class ChooseLocalityActivity extends AppCompatActivity {

    private ImageView backBtn, speechToText;
    private TextView chooseLocalityTitle, chooseLocalitySubtitle;
    private GifImageView chooseLocalityGif;
    private EditText inputLocalitySearchField;
    private RecyclerView recyclerLocality;
    private ProgressBar progressBar;
    private ConstraintLayout layoutContent, layoutNoInternet, retryBtn;

    private CollectionReference localitiesRef, userRef;
    private FirestorePagingAdapter<Locality, LocalityViewHolder> localityAdapter;

    private PreferenceManager preferenceManager;
    private static int LAST_POSITION = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_locality);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(ChooseLocalityActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(ChooseLocalityActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        ////////////////////////////////////////////////////////////////////////////////////////////

        layoutContent = findViewById(R.id.layout_content);
        layoutNoInternet = findViewById(R.id.layout_no_internet);
        retryBtn = findViewById(R.id.retry_btn);

        backBtn = findViewById(R.id.back_btn);
        chooseLocalityTitle = findViewById(R.id.choose_locality_title);
        chooseLocalitySubtitle = findViewById(R.id.choose_locality_subtitle);
        chooseLocalityGif = findViewById(R.id.choose_locality_gif);
        inputLocalitySearchField = findViewById(R.id.input_locality_search_field);
        speechToText = findViewById(R.id.speech_to_text);
        recyclerLocality = findViewById(R.id.recycler_locality);
        progressBar = findViewById(R.id.progress_bar);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkNetworkConnection();
    }

    private void checkNetworkConnection() {
        if (!isConnectedToInternet(ChooseLocalityActivity.this)) {
            layoutContent.setVisibility(View.GONE);
            layoutNoInternet.setVisibility(View.VISIBLE);
            retryBtn.setOnClickListener(v -> checkNetworkConnection());
        } else {
            initFirebase();
            setActionOnViews();
        }
    }

    private void initFirebase() {
        localitiesRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES);
        userRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS);
    }

    private void setActionOnViews() {
        layoutNoInternet.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);

        ////////////////////////////////////////////////////////////////////////////////////////////

        backBtn.setOnClickListener(view -> onBackPressed());

        ////////////////////////////////////////////////////////////////////////////////////////////

        String name = preferenceManager.getString(Constants.KEY_USER_NAME);
        String[] splitName = name.split(" ", 2);
        chooseLocalityTitle.setText(String.format("Well, %s", splitName[0]));
        chooseLocalitySubtitle.setText(String.format("Where in %s?", preferenceManager.getString(Constants.KEY_USER_CITY)));

        ////////////////////////////////////////////////////////////////////////////////////////////

        KeyboardVisibilityEvent.setEventListener(ChooseLocalityActivity.this, isOpen -> {
            if (!isOpen) {
                inputLocalitySearchField.clearFocus();
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        loadLocalities();
        progressBar.setVisibility(View.VISIBLE);

        ////////////////////////////////////////////////////////////////////////////////////////////

        speechToText.setOnClickListener(view -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, String.format("Well, %s! Where in %s?", splitName[0], preferenceManager.getString(Constants.KEY_USER_CITY)));

            try {
                startActivityForResult(intent, 123);
            } catch (ActivityNotFoundException e) {
                Alerter.create(ChooseLocalityActivity.this)
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
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        inputLocalitySearchField.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                chooseLocalityGif.setForeground(new ColorDrawable(Color.parseColor("#80000000")));
            } else {
                chooseLocalityGif.setForeground(new ColorDrawable(Color.parseColor("#00000000")));
            }
        });

        inputLocalitySearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                Query updatedQuery;
                if (s.toString().isEmpty()) {
                    updatedQuery = localitiesRef.orderBy("name", Query.Direction.ASCENDING);
                } else {
                    updatedQuery = localitiesRef.orderBy("searchKeyword", Query.Direction.ASCENDING)
                            .startAt(s.toString().toLowerCase().trim()).endAt(s.toString().toLowerCase().trim() + "\uf8ff");
                }

                PagedList.Config updatedConfig = new PagedList.Config.Builder()
                        .setInitialLoadSizeHint(4)
                        .setPageSize(4)
                        .build();
                FirestorePagingOptions<Locality> updatedOptions = new FirestorePagingOptions.Builder<Locality>()
                        .setQuery(updatedQuery, updatedConfig, Locality.class)
                        .build();

                localityAdapter.updateOptions(updatedOptions);
            }

            @Override
            public void afterTextChanged(Editable s) {
                inputLocalitySearchField.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        UIUtil.hideKeyboard(ChooseLocalityActivity.this);
                        Query updatedQuery;
                        if (s.toString().isEmpty()) {
                            updatedQuery = localitiesRef.orderBy("name", Query.Direction.ASCENDING);
                        } else {
                            updatedQuery = localitiesRef.orderBy("searchKeyword", Query.Direction.ASCENDING)
                                    .startAt(s.toString().toLowerCase().trim()).endAt(s.toString().toLowerCase().trim() + "\uf8ff");
                        }

                        PagedList.Config updatedConfig = new PagedList.Config.Builder()
                                .setInitialLoadSizeHint(4)
                                .setPageSize(4)
                                .build();
                        FirestorePagingOptions<Locality> updatedOptions = new FirestorePagingOptions.Builder<Locality>()
                                .setQuery(updatedQuery, updatedConfig, Locality.class)
                                .build();

                        localityAdapter.updateOptions(updatedOptions);
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
                    inputLocalitySearchField.setText(result.get(0));
                    inputLocalitySearchField.clearFocus();
                }
                break;
        }
    }

    /////////////////////////////////////// LoadLocalities /////////////////////////////////////////

    private void loadLocalities() {
        Query query = localitiesRef.orderBy("name", Query.Direction.ASCENDING);
        PagedList.Config config = new PagedList.Config.Builder()
                .setInitialLoadSizeHint(4)
                .setPageSize(4)
                .build();
        FirestorePagingOptions<Locality> options = new FirestorePagingOptions.Builder<Locality>()
                .setLifecycleOwner(ChooseLocalityActivity.this)
                .setQuery(query, config, Locality.class)
                .build();

        localityAdapter = new FirestorePagingAdapter<Locality, LocalityViewHolder>(options) {

            @NonNull
            @Override
            public LocalityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_locality, parent, false);
                return new LocalityViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull LocalityViewHolder holder, int position, @NonNull Locality model) {
                holder.localityName.setText(model.getName());

                holder.clickListener.setOnClickListener(view -> {
                    UIUtil.hideKeyboard(ChooseLocalityActivity.this);
                    notifyDataSetChanged();
                    progressBar.setVisibility(View.VISIBLE);

                    userRef.document(preferenceManager.getString(Constants.KEY_USER_ID))
                            .update(Constants.KEY_USER_LOCALITY, model.getName())
                            .addOnSuccessListener(aVoid -> {
                                progressBar.setVisibility(View.GONE);

                                preferenceManager.putString(Constants.KEY_USER_LOCALITY, model.getName());

                                startActivity(new Intent(ChooseLocalityActivity.this, MainActivity.class));
                                CustomIntent.customType(ChooseLocalityActivity.this, "fadein-to-fadeout");
                                finish();

                            }).addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Alerter.create(ChooseLocalityActivity.this)
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
            protected void onLoadingStateChanged(@NonNull LoadingState state) {
                super.onLoadingStateChanged(state);
                switch (state) {
                    case LOADING_INITIAL:
                    case LOADING_MORE:
                        progressBar.setVisibility(View.VISIBLE);
                        break;
                    case LOADED:
                    case FINISHED:
                        progressBar.setVisibility(View.GONE);
                        break;
                    case ERROR:
                        progressBar.setVisibility(View.GONE);
                        Alerter.create(ChooseLocalityActivity.this)
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
                        break;
                }
            }
        };

        localityAdapter.notifyDataSetChanged();

        recyclerLocality.setHasFixedSize(true);
        recyclerLocality.setLayoutManager(new LinearLayoutManager(ChooseLocalityActivity.this));
        recyclerLocality.setAdapter(localityAdapter);
    }

    ///////////////////////////////////// LocalityViewHolder ///////////////////////////////////////

    public static class LocalityViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout clickListener;
        TextView localityName;

        public LocalityViewHolder(@NonNull View itemView) {
            super(itemView);

            clickListener = itemView.findViewById(R.id.click_listener);
            localityName = itemView.findViewById(R.id.locality_name);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(ChooseLocalityActivity chooseLocalityActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) chooseLocalityActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        KeyboardVisibilityEvent.setEventListener(ChooseLocalityActivity.this, isOpen -> {
            if (isOpen) {
                UIUtil.hideKeyboard(ChooseLocalityActivity.this);
            }
        });
        CustomIntent.customType(ChooseLocalityActivity.this, "right-to-left");
    }
}