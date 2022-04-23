package com.application.grocertaxi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.application.grocertaxi.Helper.LoadingDialog;
import com.application.grocertaxi.Model.Coupon;
import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.tapadoo.alerter.Alerter;

import dev.shreyaspatil.MaterialDialog.MaterialDialog;
import maes.tech.intentanim.CustomIntent;

public class CouponsListActivity extends AppCompatActivity {

    private ImageView closeBtn;
    private RecyclerView recyclerCoupons;
    private ConstraintLayout layoutContent, layoutEmpty, layoutNoInternet, retryBtn;
    private SwipeRefreshLayout refreshLayout;
    private ShimmerFrameLayout shimmerLayout;

    private CollectionReference couponsRef;
    private FirestoreRecyclerAdapter<Coupon, CouponViewHolder> couponAdapter;

    private PreferenceManager preferenceManager;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coupons_list);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(CouponsListActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(CouponsListActivity.this, "fadein-to-fadeout");
            finish();
        } else if (preferenceManager.getString(Constants.KEY_USER_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_CITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).isEmpty()) {
            Intent intent = new Intent(CouponsListActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(CouponsListActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        loadingDialog = new LoadingDialog(CouponsListActivity.this);

        ////////////////////////////////////////////////////////////////////////////////////////////

        closeBtn = findViewById(R.id.close_btn);
        recyclerCoupons = findViewById(R.id.recycler_coupons);
        refreshLayout = findViewById(R.id.refresh_layout);
        shimmerLayout = findViewById(R.id.shimmer_layout);
        layoutContent = findViewById(R.id.layout_content);
        layoutEmpty = findViewById(R.id.layout_empty);
        layoutNoInternet = findViewById(R.id.layout_no_internet);
        retryBtn = findViewById(R.id.retry_btn);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkNetworkConnection();
    }

    private void checkNetworkConnection() {
        if (!isConnectedToInternet(CouponsListActivity.this)) {
            layoutContent.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
            layoutNoInternet.setVisibility(View.VISIBLE);
            retryBtn.setOnClickListener(v -> checkNetworkConnection());
        } else {
            initFirebase();
            setActionOnViews();
        }
    }

    private void initFirebase() {
        couponsRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_COUPONS);
    }

    private void setActionOnViews() {
        layoutNoInternet.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);

        refreshLayout.setRefreshing(false);
        refreshLayout.setOnRefreshListener(this::checkNetworkConnection);

        ////////////////////////////////////////////////////////////////////////////////////////////

        closeBtn.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        loadCoupons();
    }

    //////////////////////////////////////// Load Coupons /////////////////////////////////////////

    @SuppressLint("NotifyDataSetChanged")
    private void loadCoupons() {
        shimmerLayout.setVisibility(View.VISIBLE);
        shimmerLayout.startShimmer();

        Query query = couponsRef;
        FirestoreRecyclerOptions<Coupon> options = new FirestoreRecyclerOptions.Builder<Coupon>()
                .setLifecycleOwner(CouponsListActivity.this)
                .setQuery(query, Coupon.class)
                .build();

        couponAdapter = new FirestoreRecyclerAdapter<Coupon, CouponViewHolder>(options) {
            @NonNull
            @Override
            public CouponViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_coupon, parent, false);
                return new CouponViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull CouponViewHolder holder, int position, @NonNull Coupon model) {
                holder.code.setText(model.getCode());
                holder.description.setText(model.getDescription());

                holder.clickListener.setOnClickListener(v -> {
                    if (!isConnectedToInternet(CouponsListActivity.this)) {
                        showConnectToInternetDialog();
                        return;
                    } else {
                        if (model.getCode().equals("GTNEW10")) {
                            loadingDialog.startDialog();

                            FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                                    .document(preferenceManager.getString(Constants.KEY_USER_ID))
                                    .get().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot documentSnapshot = task.getResult();
                                    if (documentSnapshot.exists()) {
                                        boolean first_order = documentSnapshot.getBoolean(Constants.KEY_USER_FIRST_ORDER);

                                        if (first_order) {
                                            loadingDialog.dismissDialog();

                                            preferenceManager.putString(Constants.KEY_COUPON, model.getCode());
                                            preferenceManager.putString(Constants.KEY_COUPON_DISCOUNT_PERCENT, String.valueOf(model.getDiscountPercent()));

                                            onBackPressed();
                                        } else {
                                            loadingDialog.dismissDialog();
                                            MaterialDialog materialDialog = new MaterialDialog.Builder(CouponsListActivity.this)
                                                    .setTitle("You can't use this coupon!")
                                                    .setMessage("This coupon is only valid for your first order. Since this is not your first order, we're sorry you can't use this.")
                                                    .setCancelable(false)
                                                    .setPositiveButton("Okay", R.drawable.ic_dialog_okay, (dialogInterface, which) -> dialogInterface.dismiss())
                                                    .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                                            materialDialog.show();
                                        }
                                    } else {
                                        loadingDialog.dismissDialog();

                                        Alerter.create(CouponsListActivity.this)
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
                                } else {
                                    loadingDialog.dismissDialog();

                                    Alerter.create(CouponsListActivity.this)
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
                        } else {
                            preferenceManager.putString(Constants.KEY_COUPON, model.getCode());
                            preferenceManager.putString(Constants.KEY_COUPON_DISCOUNT_PERCENT, String.valueOf(model.getDiscountPercent()));

                            onBackPressed();
                        }
                    }
                });
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();

                refreshLayout.setRefreshing(false);
                shimmerLayout.stopShimmer();
                shimmerLayout.setVisibility(View.GONE);

                if (getItemCount() == 0) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(@NonNull FirebaseFirestoreException e) {
                super.onError(e);

                refreshLayout.setRefreshing(false);
                shimmerLayout.stopShimmer();
                shimmerLayout.setVisibility(View.GONE);
                Alerter.create(CouponsListActivity.this)
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

        couponAdapter.notifyDataSetChanged();

        recyclerCoupons.setHasFixedSize(true);
        recyclerCoupons.setLayoutManager(new LinearLayoutManager(CouponsListActivity.this));
        recyclerCoupons.setAdapter(couponAdapter);
    }

    ////////////////////////////////////// CouponViewHolder ////////////////////////////////////////

    public static class CouponViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout clickListener;
        TextView code, description;

        public CouponViewHolder(@NonNull View itemView) {
            super(itemView);

            clickListener = itemView.findViewById(R.id.click_listener);
            code = itemView.findViewById(R.id.code);
            description = itemView.findViewById(R.id.description);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(CouponsListActivity couponsListActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) couponsListActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(CouponsListActivity.this)
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
    public void onBackPressed() {
        super.onBackPressed();
        CustomIntent.customType(CouponsListActivity.this, "up-to-bottom");
    }
}