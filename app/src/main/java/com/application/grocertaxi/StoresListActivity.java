package com.application.grocertaxi;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.paging.LoadState;
import androidx.paging.PagingConfig;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.application.grocertaxi.Model.Store;
import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.paging.FirestorePagingAdapter;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.makeramen.roundedimageview.RoundedImageView;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.ArrayList;
import java.util.Locale;

import maes.tech.intentanim.CustomIntent;
import per.wsj.library.AndRatingBar;

public class StoresListActivity extends AppCompatActivity {

    private ImageView speechToText;
    private EditText inputStoreSearch;
    private RecyclerView recyclerStores;
    private BottomNavigationView bottomBar;
    private FloatingActionButton cartBtn;
    private ConstraintLayout layoutContent, layoutEmpty, layoutNoInternet, retryBtn, sortBtn;
    private SwipeRefreshLayout refreshLayout;
    private ShimmerFrameLayout shimmerLayout;

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
        } else if (preferenceManager.getString(Constants.KEY_USER_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_CITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).isEmpty()) {
            Intent intent = new Intent(StoresListActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(StoresListActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        ////////////////////////////////////////////////////////////////////////////////////////////

        layoutContent = findViewById(R.id.layout_content);
        layoutEmpty = findViewById(R.id.layout_empty);
        layoutNoInternet = findViewById(R.id.layout_no_internet);
        retryBtn = findViewById(R.id.retry_btn);

        refreshLayout = findViewById(R.id.refresh_layout);

        speechToText = findViewById(R.id.speech_to_text);
        sortBtn = findViewById(R.id.sort_btn);
        inputStoreSearch = findViewById(R.id.input_store_search_field);
        recyclerStores = findViewById(R.id.recycler_stores);
        shimmerLayout = findViewById(R.id.shimmer_layout);

        bottomBar = findViewById(R.id.bottom_bar);
        cartBtn = findViewById(R.id.cart_btn);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkNetworkConnection();
    }

    private void checkNetworkConnection() {
        if (!isConnectedToInternet(StoresListActivity.this)) {
            layoutContent.setVisibility(View.GONE);
            layoutNoInternet.setVisibility(View.VISIBLE);
            retryBtn.setOnClickListener(v -> checkNetworkConnection());
        } else {
            initFirebase();
            setActionOnViews();
        }
    }

    private void initFirebase() {
        storesRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_LOCALITY))
                .collection(Constants.KEY_COLLECTION_STORES);
    }

    private void setActionOnViews() {
        layoutNoInternet.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        refreshLayout.setRefreshing(false);
        refreshLayout.setOnRefreshListener(this::checkNetworkConnection);

        ////////////////////////////////////////////////////////////////////////////////////////////

        KeyboardVisibilityEvent.setEventListener(StoresListActivity.this, isOpen -> {
            if (!isOpen) {
                inputStoreSearch.clearFocus();
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        loadStores();

        ////////////////////////////////////////////////////////////////////////////////////////////

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
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        inputStoreSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Query updatedQuery;
                if (s.toString().isEmpty()) {
                    updatedQuery = storesRef;
                } else {
                    updatedQuery = storesRef.orderBy(Constants.KEY_STORE_SEARCH_KEYWORD, Query.Direction.ASCENDING)
                            .startAt(s.toString().toLowerCase().trim()).endAt(s.toString().toLowerCase().trim() + "\uf8ff");
                }

                PagingConfig updatedConfig = new PagingConfig(4, 4, false);
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
                            updatedQuery = storesRef;
                        } else {
                            updatedQuery = storesRef.orderBy(Constants.KEY_STORE_SEARCH_KEYWORD, Query.Direction.ASCENDING)
                                    .startAt(s.toString().toLowerCase().trim()).endAt(s.toString().toLowerCase().trim() + "\uf8ff");
                        }

                        PagingConfig updatedConfig = new PagingConfig(4, 4, false);
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

        ////////////////////////////////////////////////////////////////////////////////////////////

        sortBtn.setOnClickListener(v -> {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(StoresListActivity.this);
            bottomSheetDialog.setContentView(R.layout.bottom_sheet_sort_stores);
            bottomSheetDialog.setCanceledOnTouchOutside(false);

            ImageView closeBtn = bottomSheetDialog.findViewById(R.id.close_bottom_sheet_btn);
            ConstraintLayout relevance = bottomSheetDialog.findViewById(R.id.relevance);
            ConstraintLayout newestFirst = bottomSheetDialog.findViewById(R.id.newest_first);
            ConstraintLayout nameAZ = bottomSheetDialog.findViewById(R.id.name_a_z);
            ConstraintLayout nameZA = bottomSheetDialog.findViewById(R.id.name_z_a);
            ConstraintLayout customerRating = bottomSheetDialog.findViewById(R.id.customer_rating);
            ConstraintLayout openOnly = bottomSheetDialog.findViewById(R.id.open_only);

            closeBtn.setOnClickListener(v1 -> bottomSheetDialog.dismiss());

            relevance.setOnClickListener(v13 -> {
                Query sortQuery = storesRef;
                PagingConfig sortConfig = new PagingConfig(4, 4, false);
                FirestorePagingOptions<Store> sortOptions = new FirestorePagingOptions.Builder<Store>()
                        .setLifecycleOwner(StoresListActivity.this)
                        .setQuery(sortQuery, sortConfig, Store.class)
                        .build();

                storeAdapter.updateOptions(sortOptions);

                bottomSheetDialog.dismiss();
            });

            newestFirst.setOnClickListener(v12 -> {
                Query sortQuery = storesRef.orderBy(Constants.KEY_STORE_TIMESTAMP, Query.Direction.DESCENDING);
                PagingConfig sortConfig = new PagingConfig(4, 4, false);
                FirestorePagingOptions<Store> sortOptions = new FirestorePagingOptions.Builder<Store>()
                        .setLifecycleOwner(StoresListActivity.this)
                        .setQuery(sortQuery, sortConfig, Store.class)
                        .build();

                storeAdapter.updateOptions(sortOptions);

                bottomSheetDialog.dismiss();
            });

            nameAZ.setOnClickListener(v15 -> {
                Query sortQuery = storesRef.orderBy(Constants.KEY_STORE_NAME, Query.Direction.ASCENDING);
                PagingConfig sortConfig = new PagingConfig(4, 4, false);
                FirestorePagingOptions<Store> sortOptions = new FirestorePagingOptions.Builder<Store>()
                        .setLifecycleOwner(StoresListActivity.this)
                        .setQuery(sortQuery, sortConfig, Store.class)
                        .build();

                storeAdapter.updateOptions(sortOptions);

                bottomSheetDialog.dismiss();
            });

            nameZA.setOnClickListener(v14 -> {
                Query sortQuery = storesRef.orderBy(Constants.KEY_STORE_NAME, Query.Direction.DESCENDING);
                PagingConfig sortConfig = new PagingConfig(4, 4, false);
                FirestorePagingOptions<Store> sortOptions = new FirestorePagingOptions.Builder<Store>()
                        .setLifecycleOwner(StoresListActivity.this)
                        .setQuery(sortQuery, sortConfig, Store.class)
                        .build();

                storeAdapter.updateOptions(sortOptions);

                bottomSheetDialog.dismiss();
            });

            customerRating.setOnClickListener(v16 -> {
                Query sortQuery = storesRef.orderBy(Constants.KEY_STORE_AVERAGE_RATING, Query.Direction.DESCENDING);
                PagingConfig sortConfig = new PagingConfig(4, 4, false);
                FirestorePagingOptions<Store> sortOptions = new FirestorePagingOptions.Builder<Store>()
                        .setLifecycleOwner(StoresListActivity.this)
                        .setQuery(sortQuery, sortConfig, Store.class)
                        .build();

                storeAdapter.updateOptions(sortOptions);

                bottomSheetDialog.dismiss();
            });

            openOnly.setOnClickListener(v17 -> {
                Query sortQuery = storesRef.whereEqualTo(Constants.KEY_STORE_STATUS, true);
                PagingConfig sortConfig = new PagingConfig(4, 4, false);
                FirestorePagingOptions<Store> sortOptions = new FirestorePagingOptions.Builder<Store>()
                        .setLifecycleOwner(StoresListActivity.this)
                        .setQuery(sortQuery, sortConfig, Store.class)
                        .build();

                storeAdapter.updateOptions(sortOptions);

                bottomSheetDialog.dismiss();
            });

            bottomSheetDialog.show();
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        bottomBar.setSelectedItemId(R.id.menu_stores);
        bottomBar.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_home:
                    startActivity(new Intent(StoresListActivity.this, MainActivity.class));
                    CustomIntent.customType(StoresListActivity.this, "fadein-to-fadeout");
                    finish();
                    break;
                case R.id.menu_categories:
                    startActivity(new Intent(StoresListActivity.this, CategoriesActivity.class));
                    CustomIntent.customType(StoresListActivity.this, "fadein-to-fadeout");
                    break;
                case R.id.menu_stores:
                    break;
                case R.id.menu_profile:
                    startActivity(new Intent(StoresListActivity.this, ProfileActivity.class));
                    CustomIntent.customType(StoresListActivity.this, "fadein-to-fadeout");
                    break;
            }
            return true;
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        cartBtn.setOnClickListener(v -> {
            preferenceManager.putString(Constants.KEY_COUPON, "");
            preferenceManager.putString(Constants.KEY_COUPON_DISCOUNT_PERCENT, String.valueOf(0));

            preferenceManager.putString(Constants.KEY_ORDER_ID, "");
            preferenceManager.putString(Constants.KEY_ORDER_BY_USERID, "");
            preferenceManager.putString(Constants.KEY_ORDER_BY_USERNAME, "");
            preferenceManager.putString(Constants.KEY_ORDER_FROM_STOREID, "");
            preferenceManager.putString(Constants.KEY_ORDER_FROM_STORENAME, "");
            preferenceManager.putString(Constants.KEY_ORDER_CUSTOMER_NAME, "");
            preferenceManager.putString(Constants.KEY_ORDER_CUSTOMER_MOBILE, "");
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_LOCATION, "");
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_ADDRESS, "");
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_LATITUDE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_LONGITUDE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_DISTANCE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_NO_OF_ITEMS, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_TOTAL_MRP, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_TOTAL_RETAIL_PRICE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_COUPON_APPLIED, "");
            preferenceManager.putString(Constants.KEY_ORDER_COUPON_DISCOUNT, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_TOTAL_DISCOUNT, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_CHARGES, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_TIP_AMOUNT, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_SUB_TOTAL, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_PAYMENT_MODE, "");
            preferenceManager.putString(Constants.KEY_ORDER_CONVENIENCE_FEE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_TOTAL_PAYABLE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_INSTRUCTIONS, "");
            preferenceManager.putString(Constants.KEY_ORDER_STATUS, "");
            preferenceManager.putString(Constants.KEY_ORDER_PLACED_TIME, "");
            preferenceManager.putString(Constants.KEY_ORDER_COMPLETION_TIME, "");
            preferenceManager.putString(Constants.KEY_ORDER_CANCELLATION_TIME, "");
            preferenceManager.putString(Constants.KEY_ORDER_TIMESTAMP, "");
            startActivity(new Intent(getApplicationContext(), CartActivity.class));
            CustomIntent.customType(StoresListActivity.this, "bottom-to-up");
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

    //////////////////////////////////////// Load Stores ///////////////////////////////////////////

    @SuppressLint("NotifyDataSetChanged")
    private void loadStores() {
        shimmerLayout.setVisibility(View.VISIBLE);
        shimmerLayout.startShimmer();

        Query query = storesRef;
        PagingConfig config = new PagingConfig(4, 4, false);
        FirestorePagingOptions<Store> options = new FirestorePagingOptions.Builder<Store>()
                .setLifecycleOwner(StoresListActivity.this)
                .setQuery(query, config, Store.class)
                .build();

        storeAdapter = new FirestorePagingAdapter<Store, StoreViewHolder>(options) {

            @NonNull
            @Override
            public StoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_store, parent, false);
                return new StoreViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull StoreViewHolder holder, int position, @NonNull Store model) {
                Glide.with(holder.storeImage.getContext()).load(model.getStoreImage())
                        .placeholder(R.drawable.thumbnail).centerCrop().into(holder.storeImage);

                ////////////////////////////////////////////////////////////////////////////////////

                holder.storeName.setText(model.getStoreName());
                holder.storeAddress.setText(model.getStoreAddress());

                ////////////////////////////////////////////////////////////////////////////////////

                if (model.isStoreStatus()) {
                    holder.storeImage.clearColorFilter();

                    holder.storeStatus.setText("Store's Open");
                    holder.storeStatus.setTextColor(getColor(R.color.successColor));
                } else {
                    ColorMatrix matrix = new ColorMatrix();
                    matrix.setSaturation(0);

                    ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
                    holder.storeImage.setColorFilter(filter);

                    holder.storeStatus.setText("Store's Closed");
                    holder.storeStatus.setTextColor(getColor(R.color.errorColor));
                }

                ////////////////////////////////////////////////////////////////////////////////////

                if (model.getStoreAverageRating() == 0) {
                    holder.storeRating.setVisibility(View.GONE);
                    holder.storeRatingBar.setVisibility(View.GONE);
                } else {
                    holder.storeRating.setVisibility(View.VISIBLE);
                    holder.storeRating.setText(String.valueOf(model.getStoreAverageRating()));
                    holder.storeRatingBar.setVisibility(View.VISIBLE);
                    holder.storeRatingBar.setRating((float) model.getStoreAverageRating());
                }

                ////////////////////////////////////////////////////////////////////////////////////

                holder.clickListener.setOnClickListener(v -> {
                    preferenceManager.putString(Constants.KEY_STORE, model.getStoreID());
                    startActivity(new Intent(StoresListActivity.this, StoreDetailsActivity.class));
                    CustomIntent.customType(StoresListActivity.this, "bottom-to-up");
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
        };

        storeAdapter.notifyDataSetChanged();

        storeAdapter.addLoadStateListener(states -> {
            LoadState refresh = states.getRefresh();
            LoadState append = states.getAppend();

            if (refresh instanceof LoadState.Error || append instanceof LoadState.Error) {
                refreshLayout.setRefreshing(false);
                shimmerLayout.stopShimmer();
                shimmerLayout.setVisibility(View.GONE);
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
            }

            if (refresh instanceof LoadState.Loading) {

            }

            if (append instanceof LoadState.Loading) {
                layoutEmpty.setVisibility(View.GONE);
            }

            if (append instanceof LoadState.NotLoading) {
                LoadState.NotLoading notLoading = (LoadState.NotLoading) append;
                if (notLoading.getEndOfPaginationReached()) {
                    refreshLayout.setRefreshing(false);
                    shimmerLayout.stopShimmer();
                    shimmerLayout.setVisibility(View.GONE);

                    if (storeAdapter.getItemCount() == 0) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                    }
                    return null;
                }

                if (refresh instanceof LoadState.NotLoading) {
                    return null;
                }
            }

            return null;
        });

        recyclerStores.setHasFixedSize(true);
        recyclerStores.setLayoutManager(new LinearLayoutManager(StoresListActivity.this));
        recyclerStores.setAdapter(storeAdapter);
    }

    //////////////////////////////////// StoreViewHolder ///////////////////////////////////////////

    public static class StoreViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout clickListener;
        RoundedImageView storeImage;
        TextView storeName, storeAddress, storeRating, storeStatus;
        AndRatingBar storeRatingBar;

        public StoreViewHolder(@NonNull View itemView) {
            super(itemView);

            clickListener = itemView.findViewById(R.id.click_listener);
            storeImage = itemView.findViewById(R.id.store_image);
            storeName = itemView.findViewById(R.id.store_name);
            storeAddress = itemView.findViewById(R.id.store_address);
            storeRating = itemView.findViewById(R.id.store_rating);
            storeRatingBar = itemView.findViewById(R.id.store_rating_bar);
            storeStatus = itemView.findViewById(R.id.store_status);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(StoresListActivity storesListActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) storesListActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

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
        KeyboardVisibilityEvent.setEventListener(StoresListActivity.this, isOpen -> {
            if (isOpen) {
                UIUtil.hideKeyboard(StoresListActivity.this);
            }
        });
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        CustomIntent.customType(StoresListActivity.this, "fadein-to-fadeout");
    }
}