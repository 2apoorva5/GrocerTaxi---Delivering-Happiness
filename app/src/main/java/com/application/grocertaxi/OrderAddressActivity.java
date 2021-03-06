package com.application.grocertaxi;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.application.grocertaxi.Helper.LoadingDialog;
import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.Random;

import dev.shreyaspatil.MaterialDialog.MaterialDialog;
import maes.tech.intentanim.CustomIntent;

public class OrderAddressActivity extends AppCompatActivity {

    private ImageView backBtn, knowMoreBtn;
    private TextInputLayout nameInput, mobileInput, deliveryAddress, instructions;
    private TextView changeAddressBtn, readPolicyBtn;
    private ChipGroup paymentMethodChipGroup;
    private ConstraintLayout layoutContent, layoutNoInternet, retryBtn, proceedBtn;

    private CollectionReference userRef, storeRef;

    private PreferenceManager preferenceManager;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_address);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(OrderAddressActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(OrderAddressActivity.this, "fadein-to-fadeout");
            finish();
        } else if (preferenceManager.getString(Constants.KEY_USER_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_CITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).isEmpty()) {
            Intent intent = new Intent(OrderAddressActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(OrderAddressActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        loadingDialog = new LoadingDialog(OrderAddressActivity.this);

        ////////////////////////////////////////////////////////////////////////////////////////////

        layoutContent = findViewById(R.id.layout_content);
        layoutNoInternet = findViewById(R.id.layout_no_internet);
        retryBtn = findViewById(R.id.retry_btn);

        backBtn = findViewById(R.id.back_btn);
        nameInput = findViewById(R.id.name);
        mobileInput = findViewById(R.id.mobile);
        deliveryAddress = findViewById(R.id.address);
        changeAddressBtn = findViewById(R.id.change_address_btn);
        knowMoreBtn = findViewById(R.id.know_more_btn);
        instructions = findViewById(R.id.instructions);
        paymentMethodChipGroup = findViewById(R.id.payment_method_chip_group);
        readPolicyBtn = findViewById(R.id.read_policy_btn);
        proceedBtn = findViewById(R.id.proceed_btn);
    }

    private void checkNetworkConnection() {
        if (!isConnectedToInternet(OrderAddressActivity.this)) {
            layoutContent.setVisibility(View.GONE);
            layoutNoInternet.setVisibility(View.VISIBLE);
            retryBtn.setOnClickListener(v -> checkNetworkConnection());
        } else {
            initFirebase();
            setActionOnViews();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkNetworkConnection();

        if (preferenceManager.getString(Constants.KEY_USER_ADDRESS).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_ADDRESS).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_ADDRESS).isEmpty() ||
                preferenceManager.getString(Constants.KEY_USER_ADDRESS) == null) {
            deliveryAddress.getEditText().setEnabled(false);
            deliveryAddress.setEndIconActivated(false);
            deliveryAddress.getEditText().setText("No address added yet");
            changeAddressBtn.setText("Add Address");
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_LOCATION, "");
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_ADDRESS, "");
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_LATITUDE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_LONGITUDE, String.valueOf(0));
        } else {
            deliveryAddress.getEditText().setEnabled(false);
            deliveryAddress.setEndIconActivated(false);
            deliveryAddress.getEditText().setText(preferenceManager.getString(Constants.KEY_USER_ADDRESS));
            changeAddressBtn.setText("Change Address");
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_LOCATION, preferenceManager.getString(Constants.KEY_USER_LOCATION));
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_ADDRESS, preferenceManager.getString(Constants.KEY_USER_ADDRESS));
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_LATITUDE, preferenceManager.getString(Constants.KEY_USER_LATITUDE));
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_LONGITUDE, preferenceManager.getString(Constants.KEY_USER_LONGITUDE));
        }
    }

    private void initFirebase() {
        userRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS);

        storeRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_LOCALITY))
                .collection(Constants.KEY_COLLECTION_STORES);
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

        KeyboardVisibilityEvent.setEventListener(OrderAddressActivity.this, isOpen -> {
            if (!isOpen) {
                nameInput.clearFocus();
                mobileInput.clearFocus();
                instructions.clearFocus();
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        nameInput.getEditText().setText(preferenceManager.getString(Constants.KEY_USER_NAME));
        mobileInput.getEditText().setText(preferenceManager.getString(Constants.KEY_USER_MOBILE).substring(3, 13));

        ////////////////////////////////////////////////////////////////////////////////////////////

        changeAddressBtn.setOnClickListener(v -> {
            preferenceManager.putString(Constants.KEY_SUBLOCALITY, "");
            preferenceManager.putString(Constants.KEY_LOCALITY, "");
            preferenceManager.putString(Constants.KEY_COUNTRY, "");
            preferenceManager.putString(Constants.KEY_PINCODE, "");
            preferenceManager.putString(Constants.KEY_LATITUDE, "");
            preferenceManager.putString(Constants.KEY_LONGITUDE, "");
            startActivity(new Intent(OrderAddressActivity.this, LocationPermissionActivity.class));
            CustomIntent.customType(OrderAddressActivity.this, "bottom-to-up");
        });

        knowMoreBtn.setOnClickListener(v -> {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(OrderAddressActivity.this);
            bottomSheetDialog.setContentView(R.layout.bottom_sheet_info5);
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

        proceedBtn.setOnClickListener(v -> {
            UIUtil.hideKeyboard(OrderAddressActivity.this);

            final String name = nameInput.getEditText().getText().toString().trim();
            final String mobile = mobileInput.getPrefixText().toString().trim() + mobileInput.getEditText().getText().toString().trim();
            final String instruction = instructions.getEditText().getText().toString().trim();

            if (!validateName() | !validateMobile()) {
                return;
            } else {
                if (preferenceManager.getString(Constants.KEY_ORDER_DELIVERY_ADDRESS).equals("") ||
                        preferenceManager.getString(Constants.KEY_ORDER_DELIVERY_ADDRESS).length() == 0 ||
                        preferenceManager.getString(Constants.KEY_ORDER_DELIVERY_ADDRESS).isEmpty() ||
                        preferenceManager.getString(Constants.KEY_ORDER_DELIVERY_ADDRESS) == null) {
                    YoYo.with(Techniques.Shake).duration(700).repeat(1).playOn(deliveryAddress);
                    Alerter.create(OrderAddressActivity.this)
                            .setText("Add a delivery address to process the order further!")
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
                    if (paymentMethodChipGroup.getCheckedChipId() == R.id.online_payment_chip) {
                        if (!isConnectedToInternet(OrderAddressActivity.this)) {
                            showConnectToInternetDialog();
                            return;
                        } else {
                            loadingDialog.startDialog();

                            storeRef.document(preferenceManager.getString(Constants.KEY_ORDER_FROM_STOREID))
                                    .get()
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            if (task.getResult().exists()) {
                                                double storeLatitude = task.getResult().getDouble(Constants.KEY_STORE_LATITUDE);
                                                double storeLongitude = task.getResult().getDouble(Constants.KEY_STORE_LONGITUDE);

                                                double userLatitude = Double.parseDouble(preferenceManager.getString(Constants.KEY_USER_LATITUDE));
                                                double userLongitude = Double.parseDouble(preferenceManager.getString(Constants.KEY_USER_LONGITUDE));

                                                float[] results = new float[1];
                                                Location.distanceBetween(storeLatitude, storeLongitude, userLatitude, userLongitude, results);
                                                float distance = results[0];

                                                double kilometre = Math.round((distance / 1000) * 100.0) / 100.0;

                                                if (kilometre <= 5) {
                                                    userRef.document(preferenceManager.getString(Constants.KEY_USER_ID)).get()
                                                            .addOnCompleteListener(task1 -> {
                                                                if (task1.isSuccessful()) {
                                                                    DocumentSnapshot documentSnapshot = task1.getResult();
                                                                    if (documentSnapshot.exists()) {
                                                                        boolean first_order = documentSnapshot.getBoolean(Constants.KEY_USER_FIRST_ORDER);

                                                                        if (first_order) {
                                                                            Random random = new Random();
                                                                            int number1 = random.nextInt(9000) + 1000;
                                                                            int number2 = random.nextInt(9000) + 1000;
                                                                            int number3 = random.nextInt(9000) + 1000;

                                                                            preferenceManager.putString(Constants.KEY_ORDER_ID, String.format("#%d%d%d", number1, number2, number3));
                                                                            preferenceManager.putString(Constants.KEY_ORDER_BY_USERID, preferenceManager.getString(Constants.KEY_USER_ID));
                                                                            preferenceManager.putString(Constants.KEY_ORDER_BY_USERNAME, preferenceManager.getString(Constants.KEY_USER_NAME));
                                                                            preferenceManager.putString(Constants.KEY_ORDER_CUSTOMER_NAME, name);
                                                                            preferenceManager.putString(Constants.KEY_ORDER_CUSTOMER_MOBILE, mobile);
                                                                            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_DISTANCE, String.valueOf(kilometre));
                                                                            preferenceManager.putString(Constants.KEY_ORDER_INSTRUCTIONS, instruction);
                                                                            preferenceManager.putString(Constants.KEY_ORDER_PAYMENT_MODE, "Online Payment");

                                                                            double total_payable = Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_SUB_TOTAL))
                                                                                    + Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TIP_AMOUNT));

                                                                            preferenceManager.putString(Constants.KEY_ORDER_CONVENIENCE_FEE, String.valueOf(0));
                                                                            preferenceManager.putString(Constants.KEY_ORDER_TOTAL_PAYABLE, String.valueOf(total_payable));

                                                                            loadingDialog.dismissDialog();

                                                                            startActivity(new Intent(OrderAddressActivity.this, OrderSummaryActivity.class));
                                                                            CustomIntent.customType(OrderAddressActivity.this, "left-to-right");
                                                                            finish();
                                                                        } else {
                                                                            Random random = new Random();
                                                                            int number1 = random.nextInt(9000) + 1000;
                                                                            int number2 = random.nextInt(9000) + 1000;
                                                                            int number3 = random.nextInt(9000) + 1000;

                                                                            preferenceManager.putString(Constants.KEY_ORDER_ID, String.format("#%d%d%d", number1, number2, number3));
                                                                            preferenceManager.putString(Constants.KEY_ORDER_BY_USERID, preferenceManager.getString(Constants.KEY_USER_ID));
                                                                            preferenceManager.putString(Constants.KEY_ORDER_BY_USERNAME, preferenceManager.getString(Constants.KEY_USER_NAME));
                                                                            preferenceManager.putString(Constants.KEY_ORDER_CUSTOMER_NAME, name);
                                                                            preferenceManager.putString(Constants.KEY_ORDER_CUSTOMER_MOBILE, mobile);
                                                                            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_DISTANCE, String.valueOf(kilometre));
                                                                            preferenceManager.putString(Constants.KEY_ORDER_INSTRUCTIONS, instruction);
                                                                            preferenceManager.putString(Constants.KEY_ORDER_PAYMENT_MODE, "Online Payment");

                                                                            double sub_total = Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_SUB_TOTAL))
                                                                                    + Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TIP_AMOUNT));

                                                                            double convenience_fee = Math.round((0.02 * sub_total) * 100.0) / 100.0;
                                                                            double total_payable = Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_SUB_TOTAL))
                                                                                    + Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TIP_AMOUNT))
                                                                                    + convenience_fee;

                                                                            preferenceManager.putString(Constants.KEY_ORDER_CONVENIENCE_FEE, String.valueOf(convenience_fee));
                                                                            preferenceManager.putString(Constants.KEY_ORDER_TOTAL_PAYABLE, String.valueOf(total_payable));

                                                                            loadingDialog.dismissDialog();

                                                                            startActivity(new Intent(OrderAddressActivity.this, OrderSummaryActivity.class));
                                                                            CustomIntent.customType(OrderAddressActivity.this, "left-to-right");
                                                                            finish();
                                                                        }
                                                                    } else {
                                                                        loadingDialog.dismissDialog();

                                                                        Alerter.create(OrderAddressActivity.this)
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

                                                                    Alerter.create(OrderAddressActivity.this)
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
                                                    loadingDialog.dismissDialog();
                                                    MaterialDialog materialDialog = new MaterialDialog.Builder(OrderAddressActivity.this)
                                                            .setTitle("Ahan! Can't proceed!")
                                                            .setMessage("The delivery address should be in a distance of 5 kilometres from the store.")
                                                            .setCancelable(false)
                                                            .setPositiveButton("Okay", R.drawable.ic_dialog_okay, (dialogInterface, which) -> {
                                                                dialogInterface.dismiss();
                                                            })
                                                            .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                                                    materialDialog.show();
                                                }
                                            } else {
                                                loadingDialog.dismissDialog();

                                                Alerter.create(OrderAddressActivity.this)
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

                                            Alerter.create(OrderAddressActivity.this)
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
                        }
                    } else if (paymentMethodChipGroup.getCheckedChipId() == R.id.cash_on_delivery_chip) {
                        if (!isConnectedToInternet(OrderAddressActivity.this)) {
                            showConnectToInternetDialog();
                            return;
                        } else {
                            loadingDialog.startDialog();

                            storeRef.document(preferenceManager.getString(Constants.KEY_ORDER_FROM_STOREID))
                                    .get()
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            if (task.getResult().exists()) {
                                                double storeLatitude = task.getResult().getDouble(Constants.KEY_STORE_LATITUDE);
                                                double storeLongitude = task.getResult().getDouble(Constants.KEY_STORE_LONGITUDE);

                                                double userLatitude = Double.parseDouble(preferenceManager.getString(Constants.KEY_USER_LATITUDE));
                                                double userLongitude = Double.parseDouble(preferenceManager.getString(Constants.KEY_USER_LONGITUDE));

                                                float[] results = new float[1];
                                                Location.distanceBetween(storeLatitude, storeLongitude, userLatitude, userLongitude, results);
                                                float distance = results[0];

                                                double kilometre = Math.round((distance / 1000) * 100.0) / 100.0;

                                                if (kilometre <= 5) {
                                                    userRef.document(preferenceManager.getString(Constants.KEY_USER_ID)).get()
                                                            .addOnCompleteListener(task1 -> {
                                                                if (task1.isSuccessful()) {
                                                                    DocumentSnapshot documentSnapshot = task1.getResult();
                                                                    if (documentSnapshot.exists()) {
                                                                        boolean first_order = documentSnapshot.getBoolean(Constants.KEY_USER_FIRST_ORDER);

                                                                        if (first_order) {
                                                                            Random random = new Random();
                                                                            int number1 = random.nextInt(9000) + 1000;
                                                                            int number2 = random.nextInt(9000) + 1000;
                                                                            int number3 = random.nextInt(9000) + 1000;

                                                                            preferenceManager.putString(Constants.KEY_ORDER_ID, String.format("#%d%d%d", number1, number2, number3));
                                                                            preferenceManager.putString(Constants.KEY_ORDER_BY_USERID, preferenceManager.getString(Constants.KEY_USER_ID));
                                                                            preferenceManager.putString(Constants.KEY_ORDER_BY_USERNAME, preferenceManager.getString(Constants.KEY_USER_NAME));
                                                                            preferenceManager.putString(Constants.KEY_ORDER_CUSTOMER_NAME, name);
                                                                            preferenceManager.putString(Constants.KEY_ORDER_CUSTOMER_MOBILE, mobile);
                                                                            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_DISTANCE, String.valueOf(kilometre));
                                                                            preferenceManager.putString(Constants.KEY_ORDER_INSTRUCTIONS, instruction);
                                                                            preferenceManager.putString(Constants.KEY_ORDER_PAYMENT_MODE, "Pay on Delivery");

                                                                            double total_payable = Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_SUB_TOTAL))
                                                                                    + Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TIP_AMOUNT));

                                                                            preferenceManager.putString(Constants.KEY_ORDER_CONVENIENCE_FEE, String.valueOf(0));
                                                                            preferenceManager.putString(Constants.KEY_ORDER_TOTAL_PAYABLE, String.valueOf(total_payable));

                                                                            loadingDialog.dismissDialog();

                                                                            startActivity(new Intent(OrderAddressActivity.this, OrderSummaryActivity.class));
                                                                            CustomIntent.customType(OrderAddressActivity.this, "left-to-right");
                                                                            finish();
                                                                        } else {
                                                                            Random random = new Random();
                                                                            int number1 = random.nextInt(9000) + 1000;
                                                                            int number2 = random.nextInt(9000) + 1000;
                                                                            int number3 = random.nextInt(9000) + 1000;

                                                                            preferenceManager.putString(Constants.KEY_ORDER_ID, String.format("#%d%d%d", number1, number2, number3));
                                                                            preferenceManager.putString(Constants.KEY_ORDER_BY_USERID, preferenceManager.getString(Constants.KEY_USER_ID));
                                                                            preferenceManager.putString(Constants.KEY_ORDER_BY_USERNAME, preferenceManager.getString(Constants.KEY_USER_NAME));
                                                                            preferenceManager.putString(Constants.KEY_ORDER_CUSTOMER_NAME, name);
                                                                            preferenceManager.putString(Constants.KEY_ORDER_CUSTOMER_MOBILE, mobile);
                                                                            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_DISTANCE, String.valueOf(kilometre));
                                                                            preferenceManager.putString(Constants.KEY_ORDER_INSTRUCTIONS, instruction);
                                                                            preferenceManager.putString(Constants.KEY_ORDER_PAYMENT_MODE, "Pay on Delivery");

                                                                            double total_payable = Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_SUB_TOTAL))
                                                                                    + Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TIP_AMOUNT));

                                                                            preferenceManager.putString(Constants.KEY_ORDER_CONVENIENCE_FEE, String.valueOf(0));
                                                                            preferenceManager.putString(Constants.KEY_ORDER_TOTAL_PAYABLE, String.valueOf(total_payable));

                                                                            loadingDialog.dismissDialog();

                                                                            startActivity(new Intent(OrderAddressActivity.this, OrderSummaryActivity.class));
                                                                            CustomIntent.customType(OrderAddressActivity.this, "left-to-right");
                                                                            finish();
                                                                        }
                                                                    } else {
                                                                        loadingDialog.dismissDialog();

                                                                        Alerter.create(OrderAddressActivity.this)
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

                                                                    Alerter.create(OrderAddressActivity.this)
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
                                                    loadingDialog.dismissDialog();
                                                    MaterialDialog materialDialog = new MaterialDialog.Builder(OrderAddressActivity.this)
                                                            .setTitle("Ahan! Can't proceed!")
                                                            .setMessage("The delivery address should be in a distance of 5 kilometres from the store.")
                                                            .setCancelable(false)
                                                            .setPositiveButton("Okay", R.drawable.ic_dialog_okay, (dialogInterface, which) -> {
                                                                dialogInterface.dismiss();
                                                            })
                                                            .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                                                    materialDialog.show();
                                                }
                                            } else {
                                                loadingDialog.dismissDialog();

                                                Alerter.create(OrderAddressActivity.this)
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

                                            Alerter.create(OrderAddressActivity.this)
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
                        }
                    } else if (!paymentMethodChipGroup.isSelected()) {
                        YoYo.with(Techniques.Shake).duration(700).repeat(1).playOn(paymentMethodChipGroup);
                        Alerter.create(OrderAddressActivity.this)
                                .setText("Select a payment method first!")
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

    private boolean validateName() {
        String name = nameInput.getEditText().getText().toString().trim();

        if (name.isEmpty()) {
            nameInput.setError("Enter receiver's name!");
            nameInput.requestFocus();
            return false;
        } else {
            nameInput.setError(null);
            nameInput.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateMobile() {
        String mobile = mobileInput.getEditText().getText().toString().trim();

        if (mobile.isEmpty()) {
            mobileInput.setError("Enter receiver's mobile!");
            mobileInput.requestFocus();
            return false;
        } else if (mobile.length() != 10) {
            mobileInput.setError("Invalid mobile. Try Again!");
            mobileInput.requestFocus();
            return false;
        } else {
            mobileInput.setError(null);
            mobileInput.setErrorEnabled(false);
            return true;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(OrderAddressActivity orderAddressActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) orderAddressActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(OrderAddressActivity.this)
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
        KeyboardVisibilityEvent.setEventListener(OrderAddressActivity.this, isOpen -> {
            if (isOpen) {
                UIUtil.hideKeyboard(OrderAddressActivity.this);
            }
        });
        CustomIntent.customType(OrderAddressActivity.this, "right-to-left");
    }
}