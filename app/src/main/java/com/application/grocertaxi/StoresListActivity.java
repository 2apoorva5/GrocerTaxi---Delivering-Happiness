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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
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
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import com.application.grocertaxi.Model.Store;
import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.baoyz.widget.PullRefreshLayout;
import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.paging.FirestorePagingAdapter;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.firebase.ui.firestore.paging.LoadingState;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.ArrayList;
import java.util.Locale;

import maes.tech.intentanim.CustomIntent;

public class StoresListActivity extends AppCompatActivity {

    private ImageView backBtn, speechToText, illustrationEmpty, menuHome, menuCategory, menuStore, menuProfile;
    private EditText inputStoreSearch;
    private RecyclerView recyclerStores;
    private TextView textEmpty;
    private ProgressBar storesProgressBar;
    private FloatingActionButton cartBtn;
    private ConstraintLayout bottomBarContainer, sortBtn;
    private PullRefreshLayout pullRefreshLayout;

    private CollectionReference storesRef;
    private FirestorePagingAdapter<Store, StoreViewHolder> storeAdapter;

    private PreferenceManager preferenceManager;
    private static int LAST_POSITION = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stores_list);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(StoresListActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(StoresListActivity.this, "fadein-to-fadeout");
            finish();
        } else if (preferenceManager.getString(Constants.KEY_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_CITY) == null ||
                preferenceManager.getString(Constants.KEY_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_LOCALITY).equals("") ||
                preferenceManager.getString(Constants.KEY_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_LOCALITY).isEmpty()) {
            Intent intent = new Intent(StoresListActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(StoresListActivity.this, "fadein-to-fadeout");
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
        backBtn = findViewById(R.id.back_btn);
        speechToText = findViewById(R.id.speech_to_text);
        sortBtn = findViewById(R.id.sort_btn);
        illustrationEmpty = findViewById(R.id.illustration_empty);
        inputStoreSearch = findViewById(R.id.input_store_search_field);
        recyclerStores = findViewById(R.id.recycler_stores);
        textEmpty = findViewById(R.id.text_empty);
        storesProgressBar = findViewById(R.id.stores_progress_bar);
        menuHome = findViewById(R.id.menu_home);
        menuCategory = findViewById(R.id.menu_category);
        menuStore = findViewById(R.id.menu_store);
        menuProfile = findViewById(R.id.menu_profile);
        cartBtn = findViewById(R.id.cart_btn);
        bottomBarContainer = findViewById(R.id.bottom_bar_container);
        pullRefreshLayout = findViewById(R.id.pull_refresh_layout);
    }

    private void initFirebase() {
        storesRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                .collection(Constants.KEY_COLLECTION_STORES);
    }

    private void setActionOnViews() {
        backBtn.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });

        KeyboardVisibilityEvent.setEventListener(StoresListActivity.this, isOpen -> {
            if (!isOpen) {
                inputStoreSearch.clearFocus();
                bottomBarContainer.setVisibility(View.VISIBLE);
                cartBtn.setVisibility(View.VISIBLE);
            } else {
                bottomBarContainer.setVisibility(View.GONE);
                cartBtn.setVisibility(View.GONE);
            }
        });

        storesProgressBar.setVisibility(View.VISIBLE);

        speechToText.setOnClickListener(view -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, String.format("Name a store!"));

            try {
                startActivityForResult(intent, 123);
            } catch (ActivityNotFoundException e) {
                Alerter.create(StoresListActivity.this)
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

        inputStoreSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Query updatedQuery;
                if (s.toString().isEmpty()) {
                    updatedQuery = storesRef.orderBy(Constants.KEY_STORE_NAME, Query.Direction.ASCENDING);
                } else {
                    updatedQuery = storesRef.orderBy(Constants.KEY_STORE_SEARCH_KEYWORD, Query.Direction.ASCENDING)
                            .startAt(s.toString().toLowerCase().trim()).endAt(s.toString().toLowerCase().trim() + "\uf8ff");
                }

                PagedList.Config updatedConfig = new PagedList.Config.Builder()
                        .setInitialLoadSizeHint(8)
                        .setPageSize(4)
                        .build();

                FirestorePagingOptions<Store> updatedOptions = new FirestorePagingOptions.Builder<Store>()
                        .setQuery(updatedQuery, updatedConfig, Store.class)
                        .build();

                storeAdapter.updateOptions(updatedOptions);
            }

            @Override
            public void afterTextChanged(Editable s) {
                inputStoreSearch.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        UIUtil.hideKeyboard(StoresListActivity.this);
                        Query updatedQuery;
                        if (s.toString().isEmpty()) {
                            updatedQuery = storesRef.orderBy(Constants.KEY_STORE_NAME, Query.Direction.ASCENDING);
                        } else {
                            updatedQuery = storesRef.orderBy(Constants.KEY_STORE_SEARCH_KEYWORD, Query.Direction.ASCENDING)
                                    .startAt(s.toString().toLowerCase().trim()).endAt(s.toString().toLowerCase().trim() + "\uf8ff");
                        }

                        PagedList.Config updatedConfig = new PagedList.Config.Builder()
                                .setInitialLoadSizeHint(8)
                                .setPageSize(4)
                                .build();

                        FirestorePagingOptions<Store> updatedOptions = new FirestorePagingOptions.Builder<Store>()
                                .setLifecycleOwner(StoresListActivity.this)
                                .setQuery(updatedQuery, updatedConfig, Store.class)
                                .build();

                        storeAdapter.updateOptions(updatedOptions);
                        return true;
                    }
                    return false;
                });
            }
        });

        sortBtn.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
            popupMenu.inflate(R.menu.menu_store_sort);
            popupMenu.setOnMenuItemClickListener(item -> {
                Query updatedQuery;
                PagedList.Config updatedConfig;
                FirestorePagingOptions<Store> updatedOptions;
                switch (item.getItemId()) {
                    case R.id.menu_name_az:
                        updatedQuery = storesRef.orderBy(Constants.KEY_STORE_NAME, Query.Direction.ASCENDING);

                        updatedConfig = new PagedList.Config.Builder()
                                .setInitialLoadSizeHint(8)
                                .setPageSize(4)
                                .build();

                        updatedOptions = new FirestorePagingOptions.Builder<Store>()
                                .setLifecycleOwner(StoresListActivity.this)
                                .setQuery(updatedQuery, updatedConfig, Store.class)
                                .build();

                        storeAdapter.updateOptions(updatedOptions);
                        return true;
                    case R.id.menu_name_za:
                        updatedQuery = storesRef.orderBy(Constants.KEY_STORE_NAME, Query.Direction.DESCENDING);

                        updatedConfig = new PagedList.Config.Builder()
                                .setInitialLoadSizeHint(8)
                                .setPageSize(4)
                                .build();

                        updatedOptions = new FirestorePagingOptions.Builder<Store>()
                                .setLifecycleOwner(StoresListActivity.this)
                                .setQuery(updatedQuery, updatedConfig, Store.class)
                                .build();

                        storeAdapter.updateOptions(updatedOptions);
                        return true;
                    case R.id.menu_newest_first:
                        updatedQuery = storesRef.orderBy(Constants.KEY_STORE_TIMESTAMP, Query.Direction.DESCENDING);

                        updatedConfig = new PagedList.Config.Builder()
                                .setInitialLoadSizeHint(8)
                                .setPageSize(4)
                                .build();

                        updatedOptions = new FirestorePagingOptions.Builder<Store>()
                                .setLifecycleOwner(StoresListActivity.this)
                                .setQuery(updatedQuery, updatedConfig, Store.class)
                                .build();

                        storeAdapter.updateOptions(updatedOptions);
                        return true;
                    case R.id.menu_customer_rating:
                        updatedQuery = storesRef.orderBy(Constants.KEY_STORE_AVERAGE_RATING, Query.Direction.DESCENDING);

                        updatedConfig = new PagedList.Config.Builder()
                                .setInitialLoadSizeHint(8)
                                .setPageSize(4)
                                .build();

                        updatedOptions = new FirestorePagingOptions.Builder<Store>()
                                .setLifecycleOwner(StoresListActivity.this)
                                .setQuery(updatedQuery, updatedConfig, Store.class)
                                .build();

                        storeAdapter.updateOptions(updatedOptions);
                        return true;
                    case R.id.menu_open_only:
                        updatedQuery = storesRef.whereEqualTo(Constants.KEY_STORE_STATUS, true)
                                .orderBy(Constants.KEY_STORE_NAME, Query.Direction.ASCENDING);

                        updatedConfig = new PagedList.Config.Builder()
                                .setInitialLoadSizeHint(8)
                                .setPageSize(4)
                                .build();

                        updatedOptions = new FirestorePagingOptions.Builder<Store>()
                                .setLifecycleOwner(StoresListActivity.this)
                                .setQuery(updatedQuery, updatedConfig, Store.class)
                                .build();

                        storeAdapter.updateOptions(updatedOptions);
                        return true;

                    default:
                        return false;
                }
            });
            popupMenu.show();
        });

        menuHome.setOnClickListener(view -> {
            startActivity(new Intent(StoresListActivity.this, MainActivity.class));
            CustomIntent.customType(StoresListActivity.this, "fadein-to-fadeout");
            finish();
        });

        menuCategory.setOnClickListener(view -> {
            startActivity(new Intent(StoresListActivity.this, CategoriesActivity.class));
            CustomIntent.customType(StoresListActivity.this, "fadein-to-fadeout");
        });

        menuStore.setOnClickListener(view -> {
            return;
        });

        menuProfile.setOnClickListener(view -> {
            startActivity(new Intent(StoresListActivity.this, ProfileActivity.class));
            CustomIntent.customType(StoresListActivity.this, "fadein-to-fadeout");
        });

        cartBtn.setOnClickListener(v -> {

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 123:
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    inputStoreSearch.setText(result.get(0));
                    inputStoreSearch.clearFocus();
                }
                break;
        }
    }

    private void loadStores() {
        Query query = storesRef.orderBy(Constants.KEY_STORE_NAME, Query.Direction.ASCENDING);

        PagedList.Config config = new PagedList.Config.Builder()
                .setInitialLoadSizeHint(8)
                .setPageSize(4)
                .build();

        FirestorePagingOptions<Store> options = new FirestorePagingOptions.Builder<Store>()
                .setLifecycleOwner(StoresListActivity.this)
                .setQuery(query, config, Store.class)
                .build();

        storeAdapter = new FirestorePagingAdapter<Store, StoreViewHolder>(options) {

            @NonNull
            @Override
            public StoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_store_item, parent, false);
                return new StoreViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull StoreViewHolder holder, int position, @NonNull Store model) {
                Glide.with(holder.storeImage.getContext()).load(model.getStoreImage())
                        .placeholder(R.drawable.thumbnail).centerCrop().into(holder.storeImage);

                holder.storeName.setText(model.getStoreName());
                holder.storeAddress.setText(model.getStoreAddress());

                if (model.isStoreStatus()) {
                    holder.storeStatus.setText("Store's Open");
                    holder.storeStatus.setTextColor(getColor(R.color.successColor));
                } else {
                    holder.storeStatus.setText("Store's Closed");
                    holder.storeStatus.setTextColor(getColor(R.color.errorColor));
                }

                if (model.getStoreAverageRating() == 0) {
                    holder.storeRating.setVisibility(View.GONE);
                    holder.storeRatingBar.setVisibility(View.GONE);
                } else {
                    holder.storeRating.setVisibility(View.VISIBLE);
                    holder.storeRating.setText(String.valueOf(model.getStoreAverageRating()));
                    holder.storeRatingBar.setRating((float) model.getStoreAverageRating());
                }

                holder.clickListener.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
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
                        storesProgressBar.setVisibility(View.VISIBLE);
                        illustrationEmpty.setVisibility(View.GONE);
                        textEmpty.setVisibility(View.GONE);
                        break;
                    case LOADED:
                    case FINISHED:
                        pullRefreshLayout.setRefreshing(false);
                        storesProgressBar.setVisibility(View.GONE);

                        if (getItemCount() == 0) {
                            illustrationEmpty.setVisibility(View.VISIBLE);
                            textEmpty.setVisibility(View.VISIBLE);
                        } else {
                            illustrationEmpty.setVisibility(View.GONE);
                            textEmpty.setVisibility(View.GONE);
                        }
                        break;
                    case ERROR:
                        pullRefreshLayout.setRefreshing(false);
                        Alerter.create(StoresListActivity.this)
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

        storeAdapter.notifyDataSetChanged();

        recyclerStores.setHasFixedSize(true);
        recyclerStores.setLayoutManager(new LinearLayoutManager(StoresListActivity.this));
        recyclerStores.setAdapter(storeAdapter);
    }

    public static class StoreViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout clickListener;
        ImageView storeImage;
        TextView storeName, storeAddress, storeRating, storeStatus;
        RatingBar storeRatingBar;

        public StoreViewHolder(@NonNull View itemView) {
            super(itemView);

            clickListener = itemView.findViewById(R.id.click_listener);
            storeImage = itemView.findViewById(R.id.store_image);
            storeName = itemView.findViewById(R.id.store_name);
            storeAddress = itemView.findViewById(R.id.store_address);
            storeRating = itemView.findViewById(R.id.store_rating);
            storeStatus = itemView.findViewById(R.id.store_status);
            storeRatingBar = itemView.findViewById(R.id.store_rating_bar);
        }
    }

    private boolean isConnectedToInternet(StoresListActivity storesListActivity) {
        ConnectivityManager connectivityManager = (ConnectivityManager) storesListActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if ((wifiConn != null && wifiConn.isConnected()) || (mobileConn != null && mobileConn.isConnected())) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(StoresListActivity.this)
                .setTitle("No Internet Connection!")
                .setMessage("Please connect to a network first to proceed from here!")
                .setCancelable(false)
                .setAnimation(R.raw.no_internet_connection)
                .setPositiveButton("Connect", R.drawable.ic_dialog_connect, (dialogInterface, which) -> {
                    dialogInterface.dismiss();
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                })
                .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
        materialDialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();

        loadStores();

        pullRefreshLayout.setColor(getColor(R.color.colorAccent));
        pullRefreshLayout.setOnRefreshListener(() -> {
            if(!isConnectedToInternet(StoresListActivity.this)) {
                pullRefreshLayout.setRefreshing(false);
                showConnectToInternetDialog();
                return;
            } else {
                UIUtil.hideKeyboard(StoresListActivity.this);
                inputStoreSearch.setText(null);
                inputStoreSearch.clearFocus();
                loadStores();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void finish() {
        super.finish();
        CustomIntent.customType(StoresListActivity.this, "fadein-to-fadeout");
    }
}