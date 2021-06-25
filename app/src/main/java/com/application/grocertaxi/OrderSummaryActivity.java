package com.application.grocertaxi;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;
import com.tomergoldst.tooltips.ToolTip;
import com.tomergoldst.tooltips.ToolTipsManager;

import org.json.JSONException;
import org.json.JSONObject;

import maes.tech.intentanim.CustomIntent;

public class OrderSummaryActivity extends AppCompatActivity implements PaymentResultListener {

    private ImageView backBtn, knowMoreBtn1, knowMoreBtn2, knowMoreBtn3, knowMoreBtn4;
    private TextView name, address, mobile, orderID, storeName, itemsCount, totalMRP, discountAmount, couponDiscount,
            deliveryCharges, textTipAdded, tipAmount, convenienceFee, totalPayable, readPolicyBtn;
    private ConstraintLayout layoutContent, layoutNoInternet, retryBtn, priceDetailsContainer, placeOrderBtn;

    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_summary);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(OrderSummaryActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(OrderSummaryActivity.this, "fadein-to-fadeout");
            finish();
        } else if (preferenceManager.getString(Constants.KEY_USER_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_CITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).isEmpty()) {
            Intent intent = new Intent(OrderSummaryActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(OrderSummaryActivity.this, "fadein-to-fadeout");
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
        knowMoreBtn1 = findViewById(R.id.know_more_btn1);
        knowMoreBtn2 = findViewById(R.id.know_more_btn2);
        knowMoreBtn3 = findViewById(R.id.know_more_btn3);
        knowMoreBtn4 = findViewById(R.id.know_more_btn4);
        name = findViewById(R.id.name);
        address = findViewById(R.id.address);
        mobile = findViewById(R.id.mobile);
        orderID = findViewById(R.id.order_id);
        storeName = findViewById(R.id.store_name);
        itemsCount = findViewById(R.id.items_count);
        totalMRP = findViewById(R.id.total_mrp);
        discountAmount = findViewById(R.id.discount_amount);
        couponDiscount = findViewById(R.id.coupon_discount);
        deliveryCharges = findViewById(R.id.delivery_charges);
        textTipAdded = findViewById(R.id.text_tip_added);
        tipAmount = findViewById(R.id.tip_amount);
        convenienceFee = findViewById(R.id.convenience_fee);
        totalPayable = findViewById(R.id.total_payable);
        priceDetailsContainer = findViewById(R.id.price_details_container);

        readPolicyBtn = findViewById(R.id.read_policy_btn);

        placeOrderBtn = findViewById(R.id.place_order_btn);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkNetworkConnection();
    }

    private void checkNetworkConnection() {
        if (!isConnectedToInternet(OrderSummaryActivity.this)) {
            layoutContent.setVisibility(View.GONE);
            layoutNoInternet.setVisibility(View.VISIBLE);
            retryBtn.setOnClickListener(v -> checkNetworkConnection());
        } else {
            setActionOnViews();
        }
    }

    private void setActionOnViews() {
        layoutNoInternet.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);

        ////////////////////////////////////////////////////////////////////////////////////////////

        backBtn.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        name.setText(preferenceManager.getString(Constants.KEY_ORDER_CUSTOMER_NAME));
        address.setText(preferenceManager.getString(Constants.KEY_ORDER_DELIVERY_ADDRESS));
        mobile.setText(preferenceManager.getString(Constants.KEY_ORDER_CUSTOMER_MOBILE));

        ////////////////////////////////////////////////////////////////////////////////////////////

        orderID.setText(preferenceManager.getString(Constants.KEY_ORDER_ID));
        storeName.setText(String.format("Ordering from %s", preferenceManager.getString(Constants.KEY_ORDER_FROM_STORENAME)));

        ////////////////////////////////////////////////////////////////////////////////////////////

        int no_of_items = Integer.parseInt(preferenceManager.getString(Constants.KEY_ORDER_NO_OF_ITEMS));
        double total_mrp = Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TOTAL_MRP));
        double discount_amount = Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TOTAL_DISCOUNT));
        double coupon_amount = Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_COUPON_DISCOUNT));
        double delivery_charges = Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_DELIVERY_CHARGES));
        double tip_amount = Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TIP_AMOUNT));
        double convenience_fee = Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_CONVENIENCE_FEE));
        double total_payable = Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TOTAL_PAYABLE));

        if (no_of_items == 1) {
            itemsCount.setText(String.format("(%d Item)", no_of_items));
        } else {
            itemsCount.setText(String.format("(%d Items)", no_of_items));
        }
        totalMRP.setText(String.format("₹ %s", total_mrp));
        discountAmount.setText(String.format("₹ %s", discount_amount));

        if (coupon_amount == 0) {
            couponDiscount.setText("No coupon applied");
            couponDiscount.setTextColor(getColor(R.color.colorTextDark));
        } else {
            couponDiscount.setText(String.format("₹ %s", coupon_amount));
            couponDiscount.setTextColor(getColor(R.color.successColor));
        }

        if (delivery_charges == 0) {
            deliveryCharges.setText("FREE");
            deliveryCharges.setTextColor(getColor(R.color.successColor));
        } else {
            deliveryCharges.setText(String.format("₹ %s", delivery_charges));
            deliveryCharges.setTextColor(getColor(R.color.errorColor));
        }

        if (tip_amount == 0) {
            textTipAdded.setVisibility(View.GONE);
            knowMoreBtn3.setVisibility(View.GONE);
            tipAmount.setVisibility(View.GONE);
        } else {
            textTipAdded.setVisibility(View.VISIBLE);
            knowMoreBtn3.setVisibility(View.VISIBLE);
            tipAmount.setVisibility(View.VISIBLE);
            tipAmount.setText(String.format("+ ₹ %s", tip_amount));
        }

        if (convenience_fee == 0) {
            convenienceFee.setText("FREE");
            convenienceFee.setTextColor(getColor(R.color.successColor));
        } else {
            convenienceFee.setText(String.format("+ ₹ %s", convenience_fee));
            convenienceFee.setTextColor(getColor(R.color.errorColor));
        }

        totalPayable.setText(String.format("₹ %s", total_payable));

        ////////////////////////////////////////////////////////////////////////////////////////////

        knowMoreBtn1.setOnClickListener(v -> {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(OrderSummaryActivity.this);
            bottomSheetDialog.setContentView(R.layout.bottom_sheet_info5);
            bottomSheetDialog.setCanceledOnTouchOutside(false);

            ImageView closeSheetBtn = bottomSheetDialog.findViewById(R.id.close_bottom_sheet_btn);
            closeSheetBtn.setOnClickListener(v12 -> bottomSheetDialog.dismiss());

            bottomSheetDialog.show();
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        knowMoreBtn2.setOnClickListener(v -> {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(OrderSummaryActivity.this);
            bottomSheetDialog.setContentView(R.layout.bottom_sheet_info2);
            bottomSheetDialog.setCanceledOnTouchOutside(false);

            ImageView closeSheetBtn = bottomSheetDialog.findViewById(R.id.close_bottom_sheet_btn);
            closeSheetBtn.setOnClickListener(v12 -> bottomSheetDialog.dismiss());

            bottomSheetDialog.show();
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        ToolTipsManager toolTipsManager = new ToolTipsManager();
        knowMoreBtn3.setOnClickListener(v -> {
            ToolTip.Builder builder = new ToolTip.Builder(
                    priceDetailsContainer.getContext(), knowMoreBtn3, priceDetailsContainer, "This amount goes into your\ndelivery superhero salary", ToolTip.POSITION_RIGHT_TO
            );
            builder.setBackgroundColor(Color.parseColor("#B3000000"));
            builder.setTextAppearance(R.style.TooltipTextAppearance);

            toolTipsManager.show(builder.build());
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        knowMoreBtn4.setOnClickListener(v -> {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(OrderSummaryActivity.this);
            bottomSheetDialog.setContentView(R.layout.bottom_sheet_info3);
            bottomSheetDialog.setCanceledOnTouchOutside(false);

            ImageView closeSheetBtn = bottomSheetDialog.findViewById(R.id.close_bottom_sheet_btn);
            closeSheetBtn.setOnClickListener(v12 -> bottomSheetDialog.dismiss());

            bottomSheetDialog.show();
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        readPolicyBtn.setOnClickListener(v -> {
                    String privacyPolicyUrl = "https://grocertaxi.wixsite.com/refund-policy";
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl));
                    startActivity(browserIntent);
                });

        ////////////////////////////////////////////////////////////////////////////////////////////

        placeOrderBtn.setOnClickListener(v -> {
            if (!isConnectedToInternet(OrderSummaryActivity.this)) {
                showConnectToInternetDialog();
                return;
            } else {
                if (preferenceManager.getString(Constants.KEY_ORDER_PAYMENT_MODE).equals("Pay on Delivery")) {
                    startActivity(new Intent(OrderSummaryActivity.this, OrderConfirmationActivity.class));
                    CustomIntent.customType(OrderSummaryActivity.this, "left-to-right");
                    finish();
                } else if (preferenceManager.getString(Constants.KEY_ORDER_PAYMENT_MODE).equals("Online Payment")) {
                    int payable = Math.round(Float.parseFloat(preferenceManager.getString(Constants.KEY_ORDER_TOTAL_PAYABLE)) * 100);

                    Checkout checkout = new Checkout();
                    checkout.setKeyID("rzp_live_TKJ8qCyCDVnT6Z");
                    checkout.setImage(R.mipmap.app_logo);
                    JSONObject jsonObject = new JSONObject();

                    try {
                        jsonObject.put("name", "Grocer Taxi");
                        jsonObject.put("description", "Order " + preferenceManager.getString(Constants.KEY_ORDER_ID));
                        jsonObject.put("theme.color", "#F9AF00");
                        jsonObject.put("theme.hide_topbar", "false");
                        jsonObject.put("currency", "INR");
                        jsonObject.put("amount", payable);
                        jsonObject.put("prefill.contact", preferenceManager.getString(Constants.KEY_USER_MOBILE));
                        jsonObject.put("prefill.email", preferenceManager.getString(Constants.KEY_USER_EMAIL));

                        JSONObject retryObj = new JSONObject();
                        retryObj.put("enabled", true);
                        retryObj.put("max_count", 4);
                        jsonObject.put("retry", retryObj);

                        checkout.open(OrderSummaryActivity.this, jsonObject);
                    } catch (JSONException e) {
                        Alerter.create(OrderSummaryActivity.this)
                                .setText(e.getMessage())
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
            }
        });
    }

    @Override
    public void onPaymentSuccess(String s) {
        try {
            startActivity(new Intent(OrderSummaryActivity.this, OrderConfirmationActivity.class));
            CustomIntent.customType(OrderSummaryActivity.this, "left-to-right");
            finish();
        } catch (Exception e) {
            Alerter.create(OrderSummaryActivity.this)
                    .setText(e.getMessage())
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

    @Override
    public void onPaymentError(int i, String s) {
        if (i == Checkout.NETWORK_ERROR) {
            Toast.makeText(OrderSummaryActivity.this, "NETWORK ERROR!", Toast.LENGTH_SHORT).show();
        } else if (i == Checkout.INVALID_OPTIONS) {
            Toast.makeText(OrderSummaryActivity.this, "Invalid details in checkout form!", Toast.LENGTH_SHORT).show();
        } else if (i == Checkout.PAYMENT_CANCELED) {
            Toast.makeText(OrderSummaryActivity.this, "Payment Cancelled!", Toast.LENGTH_SHORT).show();
        } else if (i == Checkout.TLS_ERROR) {
            Toast.makeText(OrderSummaryActivity.this, "The device does not support TLS v1.1 or TLS v1.2!", Toast.LENGTH_SHORT).show();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(OrderSummaryActivity orderSummaryActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) orderSummaryActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(OrderSummaryActivity.this)
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
        CustomIntent.customType(OrderSummaryActivity.this, "right-to-left");
    }
}