package com.application.grocertaxi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import maes.tech.intentanim.CustomIntent;

public class OrderConfirmationActivity extends AppCompatActivity {

    private ImageView backBtn, imgView3;
    private View viewBetween2;
    private CardView view3;
    private TextView textPlaced, orderID;
    private ConstraintLayout layoutConfirmation, continueBtn, trackBtn;
    private ProgressBar progressBar;

    private CollectionReference userRef, storeRef, cartRef;

    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_confirmation);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(OrderConfirmationActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(OrderConfirmationActivity.this, "fadein-to-fadeout");
            finish();
        } else if (preferenceManager.getString(Constants.KEY_USER_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_CITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).isEmpty()) {
            Intent intent = new Intent(OrderConfirmationActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(OrderConfirmationActivity.this, "fadein-to-fadeout");
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
        imgView3 = findViewById(R.id.img_view3);
        viewBetween2 = findViewById(R.id.view_between2);
        view3 = findViewById(R.id.view3);
        textPlaced = findViewById(R.id.text_placed);
        orderID = findViewById(R.id.order_id);
        layoutConfirmation = findViewById(R.id.layout_confirmation);
        continueBtn = findViewById(R.id.continue_btn);
        trackBtn = findViewById(R.id.track_btn);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void initFirebase() {
        userRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS);

        storeRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_LOCALITY))
                .collection(Constants.KEY_COLLECTION_STORES);

        cartRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .collection(Constants.KEY_COLLECTION_CART);
    }

    @SuppressLint("ResourceAsColor")
    private void setActionOnViews() {
        backBtn.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });

        if (!isConnectedToInternet(OrderConfirmationActivity.this)) {
            showConnectToInternetDialog();
            return;
        } else {
            view3.setCardBackgroundColor(getColor(R.color.colorAccent));
            imgView3.setImageResource(R.drawable.ic_pending);
            textPlaced.setText("Placing\nOrder");
            textPlaced.setTextColor(getColor(R.color.colorTextDark));
            viewBetween2.setBackgroundColor(getColor(R.color.colorAccent));
            layoutConfirmation.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);

            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE , dd-MMM-yyyy hh:mm a");
            String currentTime = simpleDateFormat.format(calendar.getTime());

            HashMap<String, Object> newOrder = new HashMap<>();
            newOrder.put(Constants.KEY_ORDER_ID, preferenceManager.getString(Constants.KEY_ORDER_ID));
            newOrder.put(Constants.KEY_ORDER_BY_USERID, preferenceManager.getString(Constants.KEY_ORDER_BY_USERID));
            newOrder.put(Constants.KEY_ORDER_BY_USERNAME, preferenceManager.getString(Constants.KEY_ORDER_BY_USERNAME));
            newOrder.put(Constants.KEY_ORDER_FROM_STOREID, preferenceManager.getString(Constants.KEY_ORDER_FROM_STOREID));
            newOrder.put(Constants.KEY_ORDER_FROM_STORENAME, preferenceManager.getString(Constants.KEY_ORDER_FROM_STORENAME));
            newOrder.put(Constants.KEY_ORDER_CUSTOMER_NAME, preferenceManager.getString(Constants.KEY_ORDER_CUSTOMER_NAME));
            newOrder.put(Constants.KEY_ORDER_CUSTOMER_MOBILE, preferenceManager.getString(Constants.KEY_ORDER_CUSTOMER_MOBILE));
            newOrder.put(Constants.KEY_ORDER_DELIVERY_ADDRESS, preferenceManager.getString(Constants.KEY_ORDER_DELIVERY_ADDRESS));
            newOrder.put(Constants.KEY_ORDER_NO_OF_ITEMS, Integer.valueOf(preferenceManager.getString(Constants.KEY_ORDER_NO_OF_ITEMS)));
            newOrder.put(Constants.KEY_ORDER_TOTAL_MRP, Double.valueOf(preferenceManager.getString(Constants.KEY_ORDER_TOTAL_MRP)));
            newOrder.put(Constants.KEY_ORDER_TOTAL_RETAIL_PRICE, Double.valueOf(preferenceManager.getString(Constants.KEY_ORDER_TOTAL_RETAIL_PRICE)));
            newOrder.put(Constants.KEY_ORDER_TOTAL_DISCOUNT, Double.valueOf(preferenceManager.getString(Constants.KEY_ORDER_TOTAL_DISCOUNT)));
            newOrder.put(Constants.KEY_ORDER_DELIVERY_CHARGES, Double.valueOf(preferenceManager.getString(Constants.KEY_ORDER_DELIVERY_CHARGES)));
            newOrder.put(Constants.KEY_ORDER_TIP_AMOUNT, Double.valueOf(preferenceManager.getString(Constants.KEY_ORDER_TIP_AMOUNT)));
            newOrder.put(Constants.KEY_ORDER_SUB_TOTAL, Double.valueOf(preferenceManager.getString(Constants.KEY_ORDER_SUB_TOTAL)));
            newOrder.put(Constants.KEY_ORDER_PAYMENT_MODE, preferenceManager.getString(Constants.KEY_ORDER_PAYMENT_MODE));
            newOrder.put(Constants.KEY_ORDER_CONVENIENCE_FEE, Double.valueOf(preferenceManager.getString(Constants.KEY_ORDER_CONVENIENCE_FEE)));
            newOrder.put(Constants.KEY_ORDER_TOTAL_PAYABLE, Double.valueOf(preferenceManager.getString(Constants.KEY_ORDER_TOTAL_PAYABLE)));
            newOrder.put(Constants.KEY_ORDER_STATUS, "Placed");
            newOrder.put(Constants.KEY_ORDER_PLACED_TIME, currentTime);
            newOrder.put(Constants.KEY_ORDER_COMPLETION_TIME, "");
            newOrder.put(Constants.KEY_ORDER_CANCELLATION_TIME, "");
            newOrder.put(Constants.KEY_ORDER_TIMESTAMP, FieldValue.serverTimestamp());

            userRef.document(preferenceManager.getString(Constants.KEY_USER_ID))
                    .collection(Constants.KEY_COLLECTION_PENDING_ORDERS)
                    .document(preferenceManager.getString(Constants.KEY_ORDER_ID))
                    .set(newOrder).addOnSuccessListener(aVoid12 ->
                    cartRef.get().addOnSuccessListener(queryDocumentSnapshots1 -> {
                        for (QueryDocumentSnapshot cartDocumentSnapshot : queryDocumentSnapshots1) {
                            HashMap<String, Object> newOrderItem = new HashMap<>();
                            newOrderItem.put(Constants.KEY_ORDER_ITEM_ID, cartDocumentSnapshot.get(Constants.KEY_CART_ITEM_ID));
                            newOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_ID, cartDocumentSnapshot.get(Constants.KEY_CART_ITEM_PRODUCT_ID));
                            newOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_STORE_ID, cartDocumentSnapshot.get(Constants.KEY_CART_ITEM_PRODUCT_STORE_ID));
                            newOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_STORE_NAME, cartDocumentSnapshot.get(Constants.KEY_CART_ITEM_PRODUCT_STORE_NAME));
                            newOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_CATEGORY, cartDocumentSnapshot.get(Constants.KEY_CART_ITEM_PRODUCT_CATEGORY));
                            newOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_IMAGE, cartDocumentSnapshot.get(Constants.KEY_CART_ITEM_PRODUCT_IMAGE));
                            newOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_NAME, cartDocumentSnapshot.get(Constants.KEY_CART_ITEM_PRODUCT_NAME));
                            newOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_UNIT, cartDocumentSnapshot.get(Constants.KEY_CART_ITEM_PRODUCT_UNIT));
                            newOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_MRP, cartDocumentSnapshot.get(Constants.KEY_CART_ITEM_PRODUCT_MRP));
                            newOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_RETAIL_PRICE, cartDocumentSnapshot.get(Constants.KEY_CART_ITEM_PRODUCT_RETAIL_PRICE));
                            newOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_QUANTITY, cartDocumentSnapshot.get(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY));
                            newOrderItem.put(Constants.KEY_ORDER_ITEM_TIMESTAMP, FieldValue.serverTimestamp());

                            userRef.document(preferenceManager.getString(Constants.KEY_USER_ID))
                                    .collection(Constants.KEY_COLLECTION_PENDING_ORDERS)
                                    .document(preferenceManager.getString(Constants.KEY_ORDER_ID))
                                    .collection(Constants.KEY_COLLECTION_ORDER_ITEMS)
                                    .document(cartDocumentSnapshot.getId()).set(newOrderItem)
                                    .addOnSuccessListener(aVoid1 ->
                                            storeRef.document(preferenceManager.getString(Constants.KEY_ORDER_FROM_STOREID))
                                                    .collection(Constants.KEY_COLLECTION_PENDING_ORDERS)
                                                    .document(preferenceManager.getString(Constants.KEY_ORDER_ID))
                                                    .set(newOrder)
                                                    .addOnSuccessListener(aVoid11 ->
                                                            storeRef.document(preferenceManager.getString(Constants.KEY_ORDER_FROM_STOREID))
                                                                    .collection(Constants.KEY_COLLECTION_PENDING_ORDERS)
                                                                    .document(preferenceManager.getString(Constants.KEY_ORDER_ID))
                                                                    .collection(Constants.KEY_COLLECTION_ORDER_ITEMS)
                                                                    .document(cartDocumentSnapshot.getId()).set(newOrderItem)
                                                                    .addOnSuccessListener(aVoid111 ->
                                                                            cartRef.get().addOnSuccessListener(queryDocumentSnapshots2 -> {
                                                                                for (QueryDocumentSnapshot deleteDocumentSnapshot : queryDocumentSnapshots2) {
                                                                                    deleteDocumentSnapshot.getReference().delete()
                                                                                            .addOnSuccessListener(aVoid1111 -> {
                                                                                                userRef.document(preferenceManager.getString(Constants.KEY_USER_ID))
                                                                                                        .update(Constants.KEY_USER_FIRST_ORDER, false)
                                                                                                        .addOnSuccessListener(aVoid -> {
                                                                                                            view3.setCardBackgroundColor(getColor(R.color.successColor));
                                                                                                            imgView3.setImageResource(R.drawable.ic_dialog_okay);
                                                                                                            textPlaced.setText("Order\nPlaced");
                                                                                                            textPlaced.setTextColor(getColor(R.color.successColor));
                                                                                                            viewBetween2.setBackgroundColor(getColor(R.color.successColor));
                                                                                                            progressBar.setVisibility(View.GONE);
                                                                                                            layoutConfirmation.setVisibility(View.VISIBLE);

                                                                                                            preferenceManager.putBoolean(Constants.KEY_USER_FIRST_ORDER, false);

                                                                                                            String text_order_id = "You can track the status of your order " + preferenceManager.getString(Constants.KEY_ORDER_ID);
                                                                                                            SpannableString ss = new SpannableString(text_order_id);
                                                                                                            ForegroundColorSpan fcs = new ForegroundColorSpan(getColor(R.color.colorPrimary));
                                                                                                            ss.setSpan(fcs, 39, 52, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                                                                                                            orderID.setText(ss);

                                                                                                            continueBtn.setOnClickListener(v -> {
                                                                                                                startActivity(new Intent(OrderConfirmationActivity.this, MainActivity.class));
                                                                                                                CustomIntent.customType(OrderConfirmationActivity.this, "right-to-left");
                                                                                                                finish();
                                                                                                            });

                                                                                                            trackBtn.setOnClickListener(v -> {

                                                                                                            });
                                                                                                        }).addOnFailureListener(e -> {
                                                                                                    view3.setCardBackgroundColor(getColor(R.color.errorColor));
                                                                                                    imgView3.setImageResource(R.drawable.ic_error);
                                                                                                    textPlaced.setText("Error\nOccurred");
                                                                                                    textPlaced.setTextColor(getColor(R.color.errorColor));
                                                                                                    viewBetween2.setBackgroundColor(getColor(R.color.errorColor));
                                                                                                    layoutConfirmation.setVisibility(View.GONE);
                                                                                                    progressBar.setVisibility(View.GONE);
                                                                                                    Alerter.create(OrderConfirmationActivity.this)
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
                                                                                            }).addOnFailureListener(e -> {
                                                                                        view3.setCardBackgroundColor(getColor(R.color.errorColor));
                                                                                        imgView3.setImageResource(R.drawable.ic_error);
                                                                                        textPlaced.setText("Error\nOccurred");
                                                                                        textPlaced.setTextColor(getColor(R.color.errorColor));
                                                                                        viewBetween2.setBackgroundColor(getColor(R.color.errorColor));
                                                                                        layoutConfirmation.setVisibility(View.GONE);
                                                                                        progressBar.setVisibility(View.GONE);
                                                                                        Alerter.create(OrderConfirmationActivity.this)
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
                                                                                }
                                                                            }).addOnFailureListener(e -> {
                                                                                view3.setCardBackgroundColor(getColor(R.color.errorColor));
                                                                                imgView3.setImageResource(R.drawable.ic_error);
                                                                                textPlaced.setText("Error\nOccurred");
                                                                                textPlaced.setTextColor(getColor(R.color.errorColor));
                                                                                viewBetween2.setBackgroundColor(getColor(R.color.errorColor));
                                                                                layoutConfirmation.setVisibility(View.GONE);
                                                                                progressBar.setVisibility(View.GONE);
                                                                                Alerter.create(OrderConfirmationActivity.this)
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
                                                                            })).addOnFailureListener(e -> {
                                                                view3.setCardBackgroundColor(getColor(R.color.errorColor));
                                                                imgView3.setImageResource(R.drawable.ic_error);
                                                                textPlaced.setText("Error\nOccurred");
                                                                textPlaced.setTextColor(getColor(R.color.errorColor));
                                                                viewBetween2.setBackgroundColor(getColor(R.color.errorColor));
                                                                layoutConfirmation.setVisibility(View.GONE);
                                                                progressBar.setVisibility(View.GONE);
                                                                Alerter.create(OrderConfirmationActivity.this)
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
                                                            })).addOnFailureListener(e -> {
                                                view3.setCardBackgroundColor(getColor(R.color.errorColor));
                                                imgView3.setImageResource(R.drawable.ic_error);
                                                textPlaced.setText("Error\nOccurred");
                                                textPlaced.setTextColor(getColor(R.color.errorColor));
                                                viewBetween2.setBackgroundColor(getColor(R.color.errorColor));
                                                layoutConfirmation.setVisibility(View.GONE);
                                                progressBar.setVisibility(View.GONE);
                                                Alerter.create(OrderConfirmationActivity.this)
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
                                            })).addOnFailureListener(e -> {
                                view3.setCardBackgroundColor(getColor(R.color.errorColor));
                                imgView3.setImageResource(R.drawable.ic_error);
                                textPlaced.setText("Error\nOccurred");
                                textPlaced.setTextColor(getColor(R.color.errorColor));
                                viewBetween2.setBackgroundColor(getColor(R.color.errorColor));
                                layoutConfirmation.setVisibility(View.GONE);
                                progressBar.setVisibility(View.GONE);
                                Alerter.create(OrderConfirmationActivity.this)
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
                        }
                    }).addOnFailureListener(e -> {
                        view3.setCardBackgroundColor(getColor(R.color.errorColor));
                        imgView3.setImageResource(R.drawable.ic_error);
                        textPlaced.setText("Error\nOccurred");
                        textPlaced.setTextColor(getColor(R.color.errorColor));
                        viewBetween2.setBackgroundColor(getColor(R.color.errorColor));
                        layoutConfirmation.setVisibility(View.GONE);
                        progressBar.setVisibility(View.GONE);
                        Alerter.create(OrderConfirmationActivity.this)
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
                    })).addOnFailureListener(e -> {
                view3.setCardBackgroundColor(getColor(R.color.errorColor));
                imgView3.setImageResource(R.drawable.ic_error);
                textPlaced.setText("Error\nOccurred");
                textPlaced.setTextColor(getColor(R.color.errorColor));
                viewBetween2.setBackgroundColor(getColor(R.color.errorColor));
                layoutConfirmation.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                Alerter.create(OrderConfirmationActivity.this)
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
        }
    }

    private boolean isConnectedToInternet(OrderConfirmationActivity orderConfirmationActivity) {
        ConnectivityManager connectivityManager = (ConnectivityManager) orderConfirmationActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if ((wifiConn != null && wifiConn.isConnected()) || (mobileConn != null && mobileConn.isConnected())) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(OrderConfirmationActivity.this)
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
        CustomIntent.customType(OrderConfirmationActivity.this, "right-to-left");
    }

    @Override
    public void finish() {
        super.finish();

        preferenceManager.putString(Constants.KEY_ORDER_ID, "");
        preferenceManager.putString(Constants.KEY_ORDER_BY_USERID, "");
        preferenceManager.putString(Constants.KEY_ORDER_BY_USERNAME, "");
        preferenceManager.putString(Constants.KEY_ORDER_FROM_STOREID, "");
        preferenceManager.putString(Constants.KEY_ORDER_FROM_STORENAME, "");
        preferenceManager.putString(Constants.KEY_ORDER_CUSTOMER_NAME, "");
        preferenceManager.putString(Constants.KEY_ORDER_CUSTOMER_MOBILE, "");
        preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_ADDRESS, "");
        preferenceManager.putString(Constants.KEY_ORDER_NO_OF_ITEMS, String.valueOf(0));
        preferenceManager.putString(Constants.KEY_ORDER_TOTAL_MRP, String.valueOf(0));
        preferenceManager.putString(Constants.KEY_ORDER_TOTAL_RETAIL_PRICE, String.valueOf(0));
        preferenceManager.putString(Constants.KEY_ORDER_TOTAL_DISCOUNT, String.valueOf(0));
        preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_CHARGES, String.valueOf(0));
        preferenceManager.putString(Constants.KEY_ORDER_TIP_AMOUNT, String.valueOf(0));
        preferenceManager.putString(Constants.KEY_ORDER_SUB_TOTAL, String.valueOf(0));
        preferenceManager.putString(Constants.KEY_ORDER_PAYMENT_MODE, "");
        preferenceManager.putString(Constants.KEY_ORDER_CONVENIENCE_FEE, String.valueOf(0));
        preferenceManager.putString(Constants.KEY_ORDER_TOTAL_PAYABLE, String.valueOf(0));
        preferenceManager.putString(Constants.KEY_ORDER_STATUS, "");
        preferenceManager.putString(Constants.KEY_ORDER_PLACED_TIME, "");
        preferenceManager.putString(Constants.KEY_ORDER_COMPLETION_TIME, "");
        preferenceManager.putString(Constants.KEY_ORDER_CANCELLATION_TIME, "");
        preferenceManager.putString(Constants.KEY_ORDER_TIMESTAMP, "");
    }
}