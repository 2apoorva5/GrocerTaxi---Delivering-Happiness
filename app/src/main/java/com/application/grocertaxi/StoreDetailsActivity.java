package com.application.grocertaxi;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.application.grocertaxi.Model.Category;
import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.bumptech.glide.Glide;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;

import java.util.HashMap;

import de.mateware.snacky.Snacky;
import dev.shreyaspatil.MaterialDialog.MaterialDialog;
import maes.tech.intentanim.CustomIntent;
import per.wsj.library.AndRatingBar;

public class StoreDetailsActivity extends AppCompatActivity {

    private ImageView closeBtn, cartBtn, storeImage;
    private FloatingActionButton emailBtn, callBtn;
    private TextView storeStatus1, storeName, storeID, storeRating, storeOwner, storeAddress,
            storeEmail, storeMobile, text, storeTiming, storeStatus2, storeMinimumOrderAmount, seeAllReviewsBtn;
    private AndRatingBar storeRatingBar, ratingBar;
    private TextInputLayout ratingComment;
    private RecyclerView recyclerCategories;
    private CardView storeStatusContainer, cartIndicator, saveBtnContainer;
    private ConstraintLayout layoutContent, layoutNoInternet, retryBtn, saveBtn;
    private ProgressBar progressBar, rateProgressBar;
    private SwipeRefreshLayout refreshLayout;

    private CollectionReference storeRef, categoriesRef, cartRef;
    private FirestoreRecyclerAdapter<Category, CategoryViewHolder> categoryAdapter;

    private String cart_location;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_details);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(StoreDetailsActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(StoreDetailsActivity.this, "fadein-to-fadeout");
            finish();
        } else if (preferenceManager.getString(Constants.KEY_USER_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_CITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).isEmpty()) {
            Intent intent = new Intent(StoreDetailsActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(StoreDetailsActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        ////////////////////////////////////////////////////////////////////////////////////////////

        cart_location = String.format("%s, %s", preferenceManager.getString(Constants.KEY_USER_LOCALITY), preferenceManager.getString(Constants.KEY_USER_CITY));

        ////////////////////////////////////////////////////////////////////////////////////////////

        layoutContent = findViewById(R.id.layout_content);
        layoutNoInternet = findViewById(R.id.layout_no_internet);
        retryBtn = findViewById(R.id.retry_btn);

        refreshLayout = findViewById(R.id.refresh_layout);

        closeBtn = findViewById(R.id.close_btn);
        cartBtn = findViewById(R.id.cart_btn);
        cartIndicator = findViewById(R.id.cart_indicator);
        storeImage = findViewById(R.id.store_image);
        emailBtn = findViewById(R.id.email_btn);
        callBtn = findViewById(R.id.call_btn);
        storeStatus1 = findViewById(R.id.store_status1);
        storeStatusContainer = findViewById(R.id.store_status_container);
        storeName = findViewById(R.id.store_name);
        storeID = findViewById(R.id.store_id);
        storeRating = findViewById(R.id.store_rating);
        storeRatingBar = findViewById(R.id.store_rating_bar);
        storeOwner = findViewById(R.id.store_owner);
        storeAddress = findViewById(R.id.store_address);
        storeEmail = findViewById(R.id.store_email);
        storeMobile = findViewById(R.id.store_mobile);
        text = findViewById(R.id.text);
        recyclerCategories = findViewById(R.id.recycler_categories);
        progressBar = findViewById(R.id.progress_bar);
        storeTiming = findViewById(R.id.store_timing);
        storeStatus2 = findViewById(R.id.store_status2);
        storeMinimumOrderAmount = findViewById(R.id.minimum_order_amount);

        ratingBar = findViewById(R.id.rating_bar);
        ratingComment = findViewById(R.id.rating_comment);
        saveBtnContainer = findViewById(R.id.save_btn_container);
        saveBtn = findViewById(R.id.save_btn);
        rateProgressBar = findViewById(R.id.rate_progress_bar);
        seeAllReviewsBtn = findViewById(R.id.see_all_reviews_btn);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkNetworkConnection();
    }

    private void checkNetworkConnection() {
        if (!isConnectedToInternet(StoreDetailsActivity.this)) {
            layoutContent.setVisibility(View.GONE);
            layoutNoInternet.setVisibility(View.VISIBLE);
            retryBtn.setOnClickListener(v -> checkNetworkConnection());
        } else {
            initFirebase();
            setActionOnViews();
        }
    }

    private void initFirebase() {
        storeRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_LOCALITY))
                .collection(Constants.KEY_COLLECTION_STORES);

        categoriesRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_LOCALITY))
                .collection(Constants.KEY_COLLECTION_STORES)
                .document(preferenceManager.getString(Constants.KEY_STORE))
                .collection(Constants.KEY_COLLECTION_CATEGORIES);

        cartRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .collection(Constants.KEY_COLLECTION_CART);
    }

    private void setActionOnViews() {
        layoutNoInternet.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);

        refreshLayout.setRefreshing(false);
        refreshLayout.setOnRefreshListener(this::checkNetworkConnection);

        ////////////////////////////////////////////////////////////////////////////////////////////

        closeBtn.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        progressBar.setVisibility(View.VISIBLE);
        loadStoreDetails();
        loadCategories();

        ////////////////////////////////////////////////////////////////////////////////////////////

        storeRef.document(preferenceManager.getString(Constants.KEY_STORE))
                .collection(Constants.KEY_COLLECTION_REVIEWS)
                .whereEqualTo("byUserID", preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener((queryDocumentSnapshots1, error) -> {
                    if (error != null) {
                        Alerter.create(StoreDetailsActivity.this)
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
                    } else {
                        if (queryDocumentSnapshots1.size() == 0 || queryDocumentSnapshots1.isEmpty()) {
                            ratingBar.setRating((float) 0);
                            ratingComment.getEditText().setText("");

                            KeyboardVisibilityEvent.setEventListener(StoreDetailsActivity.this, isOpen -> {
                                if (!isOpen) {
                                    ratingComment.clearFocus();
                                }
                            });

                            saveBtn.setOnClickListener(v -> {
                                if (ratingBar.getRating() == 0) {
                                    YoYo.with(Techniques.Shake).duration(700).repeat(1).playOn(ratingBar);
                                } else {
                                    if (!isConnectedToInternet(StoreDetailsActivity.this)) {
                                        showConnectToInternetDialog();
                                        return;
                                    } else {
                                        String[] split = preferenceManager.getString(Constants.KEY_USER_ID).split("-", 2);
                                        String review_id = "#" + split[1];

                                        HashMap<String, Object> newReview = new HashMap<>();
                                        newReview.put("reviewID", review_id);
                                        newReview.put("rating", Math.round(((double) ratingBar.getRating()) * 100.0) / 100.0);
                                        newReview.put("comment", ratingComment.getEditText().getText().toString().trim());
                                        newReview.put("byUserID", preferenceManager.getString(Constants.KEY_USER_ID));
                                        newReview.put("byUserName", preferenceManager.getString(Constants.KEY_USER_NAME));

                                        saveBtnContainer.setVisibility(View.INVISIBLE);
                                        saveBtn.setEnabled(false);
                                        rateProgressBar.setVisibility(View.VISIBLE);

                                        storeRef.document(preferenceManager.getString(Constants.KEY_STORE))
                                                .collection(Constants.KEY_COLLECTION_REVIEWS)
                                                .document(review_id)
                                                .set(newReview)
                                                .addOnSuccessListener(aVoid ->
                                                        storeRef.document(preferenceManager.getString(Constants.KEY_STORE))
                                                                .collection(Constants.KEY_COLLECTION_REVIEWS)
                                                                .addSnapshotListener((queryDocumentSnapshots2, error1) -> {
                                                                    if (error1 != null) {
                                                                        rateProgressBar.setVisibility(View.GONE);
                                                                        saveBtn.setEnabled(true);
                                                                        saveBtnContainer.setVisibility(View.VISIBLE);

                                                                        Alerter.create(StoreDetailsActivity.this)
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
                                                                    } else {
                                                                        if (queryDocumentSnapshots2.size() != 0 && !queryDocumentSnapshots2.isEmpty()) {
                                                                            int count = queryDocumentSnapshots2.size();
                                                                            double total_rating = 0;

                                                                            for (int i = 0; i < queryDocumentSnapshots2.size(); i++) {
                                                                                total_rating += Math.round(queryDocumentSnapshots2.getDocuments().get(i).getDouble("rating") * 100.0) / 100.0;
                                                                            }

                                                                            double average_rating = Math.round((total_rating / count) * 100.0) / 100.0;

                                                                            storeRef.document(preferenceManager.getString(Constants.KEY_STORE))
                                                                                    .update(Constants.KEY_STORE_AVERAGE_RATING, average_rating)
                                                                                    .addOnSuccessListener(aVoid1 -> {
                                                                                        rateProgressBar.setVisibility(View.GONE);
                                                                                        saveBtn.setEnabled(true);
                                                                                        saveBtnContainer.setVisibility(View.VISIBLE);

                                                                                        Alerter.create(StoreDetailsActivity.this)
                                                                                                .setText("Your review has been submitted. Thanks for the feedback!")
                                                                                                .setTextAppearance(R.style.AlertText)
                                                                                                .setBackgroundColorRes(R.color.successColor)
                                                                                                .setIcon(R.drawable.ic_dialog_okay)
                                                                                                .setDuration(3000)
                                                                                                .enableIconPulse(true)
                                                                                                .enableVibration(true)
                                                                                                .disableOutsideTouch()
                                                                                                .enableProgress(true)
                                                                                                .setProgressColorInt(getColor(android.R.color.white))
                                                                                                .show();
                                                                                    }).addOnFailureListener(e -> {
                                                                                rateProgressBar.setVisibility(View.GONE);
                                                                                saveBtn.setEnabled(true);
                                                                                saveBtnContainer.setVisibility(View.VISIBLE);

                                                                                Alerter.create(StoreDetailsActivity.this)
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
                                                                        } else {
                                                                            rateProgressBar.setVisibility(View.GONE);
                                                                            saveBtn.setEnabled(true);
                                                                            saveBtnContainer.setVisibility(View.VISIBLE);

                                                                            Alerter.create(StoreDetailsActivity.this)
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
                                                                    }
                                                                })).addOnFailureListener(e -> {
                                            rateProgressBar.setVisibility(View.GONE);
                                            saveBtn.setEnabled(true);
                                            saveBtnContainer.setVisibility(View.VISIBLE);

                                            Alerter.create(StoreDetailsActivity.this)
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
                                    }
                                }
                            });
                        } else {
                            double rating = queryDocumentSnapshots1.getDocuments().get(0).getDouble("rating");
                            String comment = queryDocumentSnapshots1.getDocuments().get(0).getString("comment");

                            ratingBar.setRating((float) rating);
                            ratingComment.getEditText().setText(comment);

                            KeyboardVisibilityEvent.setEventListener(StoreDetailsActivity.this, isOpen -> {
                                if (!isOpen) {
                                    ratingComment.clearFocus();
                                }
                            });

                            saveBtn.setOnClickListener(v -> {
                                if (ratingBar.getRating() == 0) {
                                    YoYo.with(Techniques.Shake).duration(700).repeat(1).playOn(ratingBar);
                                } else {
                                    if (!isConnectedToInternet(StoreDetailsActivity.this)) {
                                        showConnectToInternetDialog();
                                        return;
                                    } else {
                                        String[] split = preferenceManager.getString(Constants.KEY_USER_ID).split("-", 2);
                                        String review_id = "#" + split[1];

                                        saveBtnContainer.setVisibility(View.INVISIBLE);
                                        saveBtn.setEnabled(false);
                                        rateProgressBar.setVisibility(View.VISIBLE);

                                        storeRef.document(preferenceManager.getString(Constants.KEY_STORE))
                                                .collection(Constants.KEY_COLLECTION_REVIEWS)
                                                .document(review_id)
                                                .update("rating", Math.round(((double) ratingBar.getRating()) * 100.0) / 100.0,
                                                        "comment", ratingComment.getEditText().getText().toString().trim())
                                                .addOnSuccessListener(aVoid ->
                                                        storeRef.document(preferenceManager.getString(Constants.KEY_STORE))
                                                                .collection(Constants.KEY_COLLECTION_REVIEWS)
                                                                .addSnapshotListener((queryDocumentSnapshots2, error1) -> {
                                                                    if (error1 != null) {
                                                                        rateProgressBar.setVisibility(View.GONE);
                                                                        saveBtn.setEnabled(true);
                                                                        saveBtnContainer.setVisibility(View.VISIBLE);

                                                                        Alerter.create(StoreDetailsActivity.this)
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
                                                                    } else {
                                                                        if (queryDocumentSnapshots2.size() != 0 && !queryDocumentSnapshots2.isEmpty()) {
                                                                            int count = queryDocumentSnapshots2.size();
                                                                            double total_rating = 0;

                                                                            for (int i = 0; i < queryDocumentSnapshots2.size(); i++) {
                                                                                total_rating += Math.round(queryDocumentSnapshots2.getDocuments().get(i).getDouble("rating") * 100.0) / 100.0;
                                                                            }

                                                                            double average_rating = Math.round((total_rating / count) * 100.0) / 100.0;

                                                                            storeRef.document(preferenceManager.getString(Constants.KEY_STORE))
                                                                                    .update(Constants.KEY_STORE_AVERAGE_RATING, average_rating)
                                                                                    .addOnSuccessListener(aVoid1 -> {
                                                                                        rateProgressBar.setVisibility(View.GONE);
                                                                                        saveBtn.setEnabled(true);
                                                                                        saveBtnContainer.setVisibility(View.VISIBLE);

                                                                                        Alerter.create(StoreDetailsActivity.this)
                                                                                                .setText("Your review has been submitted. Thanks for the feedback!")
                                                                                                .setTextAppearance(R.style.AlertText)
                                                                                                .setBackgroundColorRes(R.color.successColor)
                                                                                                .setIcon(R.drawable.ic_dialog_okay)
                                                                                                .setDuration(3000)
                                                                                                .enableIconPulse(true)
                                                                                                .enableVibration(true)
                                                                                                .disableOutsideTouch()
                                                                                                .enableProgress(true)
                                                                                                .setProgressColorInt(getColor(android.R.color.white))
                                                                                                .show();
                                                                                    }).addOnFailureListener(e -> {
                                                                                rateProgressBar.setVisibility(View.GONE);
                                                                                saveBtn.setEnabled(true);
                                                                                saveBtnContainer.setVisibility(View.VISIBLE);

                                                                                Alerter.create(StoreDetailsActivity.this)
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
                                                                        } else {
                                                                            rateProgressBar.setVisibility(View.GONE);
                                                                            saveBtn.setEnabled(true);
                                                                            saveBtnContainer.setVisibility(View.VISIBLE);

                                                                            Alerter.create(StoreDetailsActivity.this)
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
                                                                    }
                                                                })).addOnFailureListener(e -> {
                                            rateProgressBar.setVisibility(View.GONE);
                                            saveBtn.setEnabled(true);
                                            saveBtnContainer.setVisibility(View.VISIBLE);

                                            Alerter.create(StoreDetailsActivity.this)
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
                                    }
                                }
                            });
                        }
                    }
                });

        ////////////////////////////////////////////////////////////////////////////////////////////

        seeAllReviewsBtn.setOnClickListener(v -> {
            startActivity(new Intent(StoreDetailsActivity.this, StoreReviewsActivity.class));
            CustomIntent.customType(StoreDetailsActivity.this, "bottom-to-up");
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        cartRef.whereEqualTo(Constants.KEY_CART_ITEM_LOCATION, cart_location).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.size() == 0) {
                cartIndicator.setVisibility(View.GONE);
            } else {
                cartIndicator.setVisibility(View.VISIBLE);
            }
        }).addOnFailureListener(e -> {
            Alerter.create(StoreDetailsActivity.this)
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
        });

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
            CustomIntent.customType(StoreDetailsActivity.this, "bottom-to-up");
        });
    }

    //////////////////////////////////// Load Store Details ////////////////////////////////////////

    private void loadStoreDetails() {
        final DocumentReference productDocumentRef = storeRef.document(preferenceManager.getString(Constants.KEY_STORE));
        productDocumentRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
                progressBar.setVisibility(View.GONE);
                refreshLayout.setRefreshing(false);
                Alerter.create(StoreDetailsActivity.this)
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
            } else {
                progressBar.setVisibility(View.GONE);
                refreshLayout.setRefreshing(false);
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    Uri store_img = Uri.parse(documentSnapshot.getString(Constants.KEY_STORE_IMAGE));
                    boolean store_status = documentSnapshot.getBoolean(Constants.KEY_STORE_STATUS);
                    String store_name = documentSnapshot.getString(Constants.KEY_STORE_NAME);
                    String store_id = documentSnapshot.getString(Constants.KEY_STORE_ID);
                    String store_rating = String.valueOf(documentSnapshot.getDouble(Constants.KEY_STORE_AVERAGE_RATING));
                    double rating = documentSnapshot.getDouble(Constants.KEY_STORE_AVERAGE_RATING);
                    String store_owner = documentSnapshot.getString(Constants.KEY_STORE_OWNER);
                    String store_address = documentSnapshot.getString(Constants.KEY_STORE_ADDRESS);
                    String store_email = documentSnapshot.getString(Constants.KEY_STORE_EMAIL);
                    String store_mobile = documentSnapshot.getString(Constants.KEY_STORE_MOBILE);
                    String store_timing = documentSnapshot.getString(Constants.KEY_STORE_TIMING);
                    double min_order_amount = documentSnapshot.getDouble(Constants.KEY_STORE_MINIMUM_ORDER_VALUE);

                    ////////////////////////////////////////////////////////////////////////////////

                    Glide.with(storeImage.getContext()).load(store_img)
                            .placeholder(R.drawable.thumbnail).centerCrop().into(storeImage);

                    ////////////////////////////////////////////////////////////////////////////////

                    if (store_status) {
                        storeImage.clearColorFilter();

                        storeStatus1.setText("Open");
                        storeStatus2.setText("Store's Open");
                        storeStatus2.setTextColor(getColor(R.color.successColor));
                        storeStatusContainer.setCardBackgroundColor(getColor(R.color.successColor));
                    } else {
                        ColorMatrix matrix = new ColorMatrix();
                        matrix.setSaturation(0);

                        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
                        storeImage.setColorFilter(filter);

                        storeStatus1.setText("Closed");
                        storeStatus2.setText("Store's Closed");
                        storeStatus2.setTextColor(getColor(R.color.errorColor));
                        storeStatusContainer.setCardBackgroundColor(getColor(R.color.errorColor));
                    }

                    ////////////////////////////////////////////////////////////////////////////////

                    storeName.setText(store_name);
                    storeID.setText(store_id);

                    ////////////////////////////////////////////////////////////////////////////////

                    if (rating == 0) {
                        storeRating.setVisibility(View.GONE);
                        storeRatingBar.setVisibility(View.GONE);
                    } else {
                        storeRating.setVisibility(View.VISIBLE);
                        storeRating.setText(store_rating);
                        storeRatingBar.setVisibility(View.VISIBLE);
                        storeRatingBar.setRating((float) rating);
                    }

                    ////////////////////////////////////////////////////////////////////////////////

                    storeOwner.setText(store_owner);
                    storeAddress.setText(store_address);

                    storeEmail.setText(store_email);
                    emailBtn.setOnClickListener(v -> {
                        Intent email = new Intent(Intent.ACTION_SENDTO);
                        email.setData(Uri.parse("mailto:" + store_email));
                        startActivity(email);
                    });

                    storeMobile.setText(store_mobile);
                    callBtn.setOnClickListener(v -> {
                        MaterialDialog materialDialog = new MaterialDialog.Builder(StoreDetailsActivity.this)
                                .setTitle("Call " + store_name + "?")
                                .setMessage("Are you sure of making a call to " + store_name + "?")
                                .setCancelable(false)
                                .setPositiveButton("Yes", R.drawable.ic_dialog_call, (dialogInterface, which) -> {
                                    dialogInterface.dismiss();
                                    if (ContextCompat.checkSelfPermission(StoreDetailsActivity.this,
                                            Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                        String dial = "tel:" + store_mobile;
                                        startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
                                    } else {
                                        Dexter.withContext(StoreDetailsActivity.this)
                                                .withPermission(Manifest.permission.CALL_PHONE)
                                                .withListener(new PermissionListener() {
                                                    @Override
                                                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                                                        String dial = "tel:" + store_mobile;
                                                        startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
                                                    }

                                                    @Override
                                                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                                                        if (permissionDeniedResponse.isPermanentlyDenied()) {
                                                            MaterialDialog materialDialog = new MaterialDialog.Builder(StoreDetailsActivity.this)
                                                                    .setTitle("Permission Denied!")
                                                                    .setMessage("Permission to access this device's phone and manage calls has been permanently denied for the app. Open the app settings to manually grant the permission.")
                                                                    .setCancelable(false)
                                                                    .setPositiveButton("Open", R.drawable.ic_dialog_settings, (dialogInterface, which) -> {
                                                                        dialogInterface.dismiss();
                                                                        Intent intent = new Intent();
                                                                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                                        intent.setData(Uri.fromParts("package", getPackageName(), null));
                                                                        startActivity(intent);
                                                                    })
                                                                    .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                                                            materialDialog.show();
                                                        } else {
                                                            Snacky.builder()
                                                                    .setActivity(StoreDetailsActivity.this)
                                                                    .setText("Permission to call denied!")
                                                                    .setDuration(Snacky.LENGTH_SHORT)
                                                                    .error()
                                                                    .show();
                                                        }
                                                    }

                                                    @Override
                                                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                                                        permissionToken.continuePermissionRequest();
                                                    }
                                                }).check();
                                    }
                                })
                                .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                        materialDialog.show();
                    });

                    ////////////////////////////////////////////////////////////////////////////////

                    storeTiming.setText(store_timing);
                    storeMinimumOrderAmount.setText(String.format("??? %s", min_order_amount));
                } else {
                    Alerter.create(StoreDetailsActivity.this)
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

                    storeImage.setImageResource(R.drawable.thumbnail);
                    storeStatusContainer.setVisibility(View.GONE);
                    storeStatus2.setVisibility(View.GONE);
                    storeName.setText("");
                    storeID.setText("");
                    storeRating.setVisibility(View.GONE);
                    storeRatingBar.setVisibility(View.GONE);
                    storeOwner.setText("");
                    storeAddress.setText("");
                    storeEmail.setText("");
                    storeMobile.setText("");
                    storeTiming.setText("");

                    emailBtn.setEnabled(false);
                    callBtn.setEnabled(false);
                }
            }
        });
    }

    /////////////////////////////////////// Load Categories ////////////////////////////////////////

    private void loadCategories() {
        Query query = categoriesRef.orderBy(Constants.KEY_CATEGORY_NAME, Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<Category> options = new FirestoreRecyclerOptions.Builder<Category>()
                .setLifecycleOwner(StoreDetailsActivity.this)
                .setQuery(query, Category.class)
                .build();

        categoryAdapter = new FirestoreRecyclerAdapter<Category, CategoryViewHolder>(options) {

            @NonNull
            @Override
            public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_store_category, parent, false);
                return new CategoryViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull CategoryViewHolder holder, int position, @NonNull Category model) {
                holder.categoryName.setText(model.getCategoryName());

                String cat_fruits = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Ffruits.png?alt=media&token=e682c6a7-16fe-47f1-b607-89b9b888b5d3";
                String cat_vegetables = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fvegetables.png?alt=media&token=5794ffa7-f88e-4477-9068-cd1d9ab4b247";
                String cat_foodgrains = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Ffoodgrains.png?alt=media&token=213bfe18-5525-4b3b-b1e0-83f55de8709e";
                String cat_dairy = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fdairy.png?alt=media&token=3623ac1a-8117-40c6-a13e-ec4be5e2518a";
                String cat_bakery = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fbakery.png?alt=media&token=786111f9-605c-4275-b0b5-901b6df68ec1";
                String cat_beverages = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fbeverages.png?alt=media&token=834a8b60-43df-4ccd-9e2f-559448c895d2";
                String cat_dryfruits = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fdry_fruits.png?alt=media&token=0e8afbf9-6cb5-42d0-ae74-e20b406b9113";
                String cat_meat = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fmeat_bacon.png?alt=media&token=b644e1af-2155-45ae-9d8f-0fc73c610997";
                String cat_noodles = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fnoodles_pasta.png?alt=media&token=e0c37743-9869-4fe9-a2f6-cd3e0ac908ad";
                String cat_snacks = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fsnacks.png?alt=media&token=e3b909e6-68bc-4dd9-bb77-092b7ec2ef7b";
                String cat_oil = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Foil.png?alt=media&token=2d22afd3-5978-4908-b8fe-8d06f9983664";
                String cat_spices = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fspices.png?alt=media&token=00b11823-bb04-4949-999f-f6860496e415";
                String cat_sweets = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fsweets.png?alt=media&token=2e4f6b55-ba63-4e90-8e86-0d3076b3e076";
                String cat_babycare = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fbaby_care.png?alt=media&token=f594f05b-92eb-433f-9cfa-fec19cf80923";
                String cat_household = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fcleaning_household.png?alt=media&token=d89ec809-bd96-42ee-920b-f8c25eefdf74";
                String cat_personalcare = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fpersonal_care.png?alt=media&token=3aa267d3-7764-47cb-ae68-f0694a4982ca";
                String cat_petcare = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fpet_care.png?alt=media&token=b5ce1d43-f34f-4008-88eb-5c81690fdc1f";
                String cat_stationary = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fstationary.png?alt=media&token=dac5dbd6-3235-42c2-9ec5-1615b4b63b44";
                String cat_hardware = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fhardware.png?alt=media&token=2a69540d-c6da-43b2-9358-564cb68a5431";
                String cat_medical = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fmedical.png?alt=media&token=83120872-249b-459a-ae93-8d29dad6bb98";
                String cat_sports = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fsports.png?alt=media&token=cd124d9c-2421-4e93-8e6a-d6e13b4933de";

                if (model.getCategoryName().equals("Fruits")) {
                    Glide.with(holder.categoryImage).load(Uri.parse(cat_fruits)).placeholder(R.drawable.thumbnail)
                            .centerCrop().into(holder.categoryImage);
                } else if (model.getCategoryName().equals("Vegetables")) {
                    Glide.with(holder.categoryImage).load(Uri.parse(cat_vegetables)).placeholder(R.drawable.thumbnail)
                            .centerCrop().into(holder.categoryImage);
                } else if (model.getCategoryName().equals("Food Grains")) {
                    Glide.with(holder.categoryImage).load(Uri.parse(cat_foodgrains)).placeholder(R.drawable.thumbnail)
                            .centerCrop().into(holder.categoryImage);
                } else if (model.getCategoryName().equals("Dairy Items")) {
                    Glide.with(holder.categoryImage).load(Uri.parse(cat_dairy)).placeholder(R.drawable.thumbnail)
                            .centerCrop().into(holder.categoryImage);
                } else if (model.getCategoryName().equals("Bakery Items")) {
                    Glide.with(holder.categoryImage).load(Uri.parse(cat_bakery)).placeholder(R.drawable.thumbnail)
                            .centerCrop().into(holder.categoryImage);
                } else if (model.getCategoryName().equals("Beverages")) {
                    Glide.with(holder.categoryImage).load(Uri.parse(cat_beverages)).placeholder(R.drawable.thumbnail)
                            .centerCrop().into(holder.categoryImage);
                } else if (model.getCategoryName().equals("Dry Fruits")) {
                    Glide.with(holder.categoryImage).load(Uri.parse(cat_dryfruits)).placeholder(R.drawable.thumbnail)
                            .centerCrop().into(holder.categoryImage);
                } else if (model.getCategoryName().equals("Meat & Bacon")) {
                    Glide.with(holder.categoryImage).load(Uri.parse(cat_meat)).placeholder(R.drawable.thumbnail)
                            .centerCrop().into(holder.categoryImage);
                } else if (model.getCategoryName().equals("Noodles & Pasta")) {
                    Glide.with(holder.categoryImage).load(Uri.parse(cat_noodles)).placeholder(R.drawable.thumbnail)
                            .centerCrop().into(holder.categoryImage);
                } else if (model.getCategoryName().equals("Snacks")) {
                    Glide.with(holder.categoryImage).load(Uri.parse(cat_snacks)).placeholder(R.drawable.thumbnail)
                            .centerCrop().into(holder.categoryImage);
                } else if (model.getCategoryName().equals("Kitchen Oil")) {
                    Glide.with(holder.categoryImage).load(Uri.parse(cat_oil)).placeholder(R.drawable.thumbnail)
                            .centerCrop().into(holder.categoryImage);
                } else if (model.getCategoryName().equals("Spices")) {
                    Glide.with(holder.categoryImage).load(Uri.parse(cat_spices)).placeholder(R.drawable.thumbnail)
                            .centerCrop().into(holder.categoryImage);
                } else if (model.getCategoryName().equals("Sweets")) {
                    Glide.with(holder.categoryImage).load(Uri.parse(cat_sweets)).placeholder(R.drawable.thumbnail)
                            .centerCrop().into(holder.categoryImage);
                } else if (model.getCategoryName().equals("Baby Care")) {
                    Glide.with(holder.categoryImage).load(Uri.parse(cat_babycare)).placeholder(R.drawable.thumbnail)
                            .centerCrop().into(holder.categoryImage);
                } else if (model.getCategoryName().equals("Household")) {
                    Glide.with(holder.categoryImage).load(Uri.parse(cat_household)).placeholder(R.drawable.thumbnail)
                            .centerCrop().into(holder.categoryImage);
                } else if (model.getCategoryName().equals("Personal Care")) {
                    Glide.with(holder.categoryImage).load(Uri.parse(cat_personalcare)).placeholder(R.drawable.thumbnail)
                            .centerCrop().into(holder.categoryImage);
                } else if (model.getCategoryName().equals("Pet Care")) {
                    Glide.with(holder.categoryImage).load(Uri.parse(cat_petcare)).placeholder(R.drawable.thumbnail)
                            .centerCrop().into(holder.categoryImage);
                } else if (model.getCategoryName().equals("Stationary")) {
                    Glide.with(holder.categoryImage).load(Uri.parse(cat_stationary)).placeholder(R.drawable.thumbnail)
                            .centerCrop().into(holder.categoryImage);
                } else if (model.getCategoryName().equals("Hardware")) {
                    Glide.with(holder.categoryImage).load(Uri.parse(cat_hardware)).placeholder(R.drawable.thumbnail)
                            .centerCrop().into(holder.categoryImage);
                } else if (model.getCategoryName().equals("Medical")) {
                    Glide.with(holder.categoryImage).load(Uri.parse(cat_medical)).placeholder(R.drawable.thumbnail)
                            .centerCrop().into(holder.categoryImage);
                } else if (model.getCategoryName().equals("Sports")) {
                    Glide.with(holder.categoryImage).load(Uri.parse(cat_sports)).placeholder(R.drawable.thumbnail)
                            .centerCrop().into(holder.categoryImage);
                }

                holder.clickListener.setOnClickListener(v -> {
                    preferenceManager.putString(Constants.KEY_STORE_CATEGORY, model.getCategoryName());
                    startActivity(new Intent(StoreDetailsActivity.this, StoreProductsListActivity.class));
                    CustomIntent.customType(StoreDetailsActivity.this, "left-to-right");
                });
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();

                refreshLayout.setRefreshing(false);

                if (getItemCount() == 0) {
                    recyclerCategories.setAdapter(null);
                    categoryAdapter.notifyDataSetChanged();
                    text.setText("Snap! Not selling any product yet");
                } else {
                    categoryAdapter.notifyDataSetChanged();
                    recyclerCategories.setAdapter(categoryAdapter);
                    recyclerCategories.getAdapter().notifyDataSetChanged();
                    text.setText("Shop by Category");
                }
            }

            @Override
            public void onError(@NonNull FirebaseFirestoreException e) {
                super.onError(e);
                refreshLayout.setRefreshing(false);
                Alerter.create(StoreDetailsActivity.this)
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

        categoryAdapter.notifyDataSetChanged();

        recyclerCategories.setLayoutManager(new GridLayoutManager(StoreDetailsActivity.this, 3));
        recyclerCategories.getLayoutManager().setAutoMeasureEnabled(true);
        recyclerCategories.setNestedScrollingEnabled(false);
        recyclerCategories.setHasFixedSize(false);
        recyclerCategories.setAdapter(categoryAdapter);
    }

    ///////////////////////////////////// CategoryViewHolder ///////////////////////////////////////

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout clickListener;
        ImageView categoryImage;
        TextView categoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);

            clickListener = itemView.findViewById(R.id.click_listener);
            categoryImage = itemView.findViewById(R.id.category_img);
            categoryName = itemView.findViewById(R.id.category_title);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(StoreDetailsActivity storeDetailsActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) storeDetailsActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(StoreDetailsActivity.this)
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

    public static void setWindowFlag(StoreDetailsActivity storeDetailsActivity, final int bits, boolean on) {
        Window window = storeDetailsActivity.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();

        if (on) {
            layoutParams.flags |= bits;
        } else {
            layoutParams.flags &= ~bits;
        }
        window.setAttributes(layoutParams);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        CustomIntent.customType(StoreDetailsActivity.this, "up-to-bottom");
    }
}