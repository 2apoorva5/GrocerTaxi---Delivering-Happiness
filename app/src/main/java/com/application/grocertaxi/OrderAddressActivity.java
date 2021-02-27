package com.application.grocertaxi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

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
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.Random;

import dmax.dialog.SpotsDialog;
import maes.tech.intentanim.CustomIntent;

public class OrderAddressActivity extends AppCompatActivity {

    private ImageView backBtn, knowMoreBtn;
    private TextInputLayout nameInput, mobileInput, deliveryAddress, instructions;
    private TextView changeAddressBtn;
    private ChipGroup paymentMethodChipGroup;
    private ConstraintLayout proceedBtn;

    private CollectionReference userRef;

    private PreferenceManager preferenceManager;
    private AlertDialog progressDialog;

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

        progressDialog = new SpotsDialog.Builder().setContext(OrderAddressActivity.this)
                .setMessage("Hold on..")
                .setCancelable(false)
                .setTheme(R.style.SpotsDialog)
                .build();

        initViews();
        initFirebase();
        setActionOnViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (preferenceManager.getString(Constants.KEY_USER_ADDRESS).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_ADDRESS).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_ADDRESS).isEmpty() ||
                preferenceManager.getString(Constants.KEY_USER_ADDRESS) == null) {
            deliveryAddress.getEditText().setEnabled(false);
            deliveryAddress.setEndIconActivated(false);
            deliveryAddress.getEditText().setText("No address added yet");
            changeAddressBtn.setText("Add Address");
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_ADDRESS, "");
        } else {
            deliveryAddress.getEditText().setEnabled(false);
            deliveryAddress.setEndIconActivated(false);
            deliveryAddress.getEditText().setText(preferenceManager.getString(Constants.KEY_USER_ADDRESS));
            changeAddressBtn.setText("Change Address");
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_ADDRESS, preferenceManager.getString(Constants.KEY_USER_ADDRESS));
        }
    }

    private void initViews() {
        backBtn = findViewById(R.id.back_btn);
        nameInput = findViewById(R.id.name);
        mobileInput = findViewById(R.id.mobile);
        deliveryAddress = findViewById(R.id.address);
        changeAddressBtn = findViewById(R.id.change_address_btn);
        knowMoreBtn = findViewById(R.id.know_more_btn);
        instructions = findViewById(R.id.instructions);
        paymentMethodChipGroup = findViewById(R.id.payment_method_chip_group);
        proceedBtn = findViewById(R.id.proceed_btn);
    }

    private void initFirebase() {
        userRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS);
    }

    private void setActionOnViews() {
        backBtn.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });

        KeyboardVisibilityEvent.setEventListener(OrderAddressActivity.this, isOpen -> {
            if (!isOpen) {
                nameInput.clearFocus();
                mobileInput.clearFocus();
                instructions.clearFocus();
            }
        });

        nameInput.getEditText().setText(preferenceManager.getString(Constants.KEY_USER_NAME));
        mobileInput.getEditText().setText(preferenceManager.getString(Constants.KEY_USER_MOBILE).substring(3, 13));

        changeAddressBtn.setOnClickListener(v -> {
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
                            progressDialog.show();

                            userRef.document(preferenceManager.getString(Constants.KEY_USER_ID)).get()
                                    .addOnCompleteListener(task -> {
                                        if(task.isSuccessful()) {
                                            DocumentSnapshot documentSnapshot = task.getResult();
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
                                                    preferenceManager.putString(Constants.KEY_ORDER_INSTRUCTIONS, instruction);
                                                    preferenceManager.putString(Constants.KEY_ORDER_PAYMENT_MODE, "Online Payment");

                                                    double total_payable = Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_SUB_TOTAL))
                                                            + Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TIP_AMOUNT));

                                                    preferenceManager.putString(Constants.KEY_ORDER_CONVENIENCE_FEE, String.valueOf(0));
                                                    preferenceManager.putString(Constants.KEY_ORDER_TOTAL_PAYABLE, String.valueOf(total_payable));

                                                    progressDialog.dismiss();

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

                                                    progressDialog.dismiss();

                                                    startActivity(new Intent(OrderAddressActivity.this, OrderSummaryActivity.class));
                                                    CustomIntent.customType(OrderAddressActivity.this, "left-to-right");
                                                    finish();
                                                }
                                            } else {
                                                progressDialog.dismiss();

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
                                            progressDialog.dismiss();

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
                            progressDialog.show();

                            userRef.document(preferenceManager.getString(Constants.KEY_USER_ID)).get()
                                    .addOnCompleteListener(task -> {
                                        if(task.isSuccessful()) {
                                            DocumentSnapshot documentSnapshot = task.getResult();
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
                                                    preferenceManager.putString(Constants.KEY_ORDER_INSTRUCTIONS, instruction);
                                                    preferenceManager.putString(Constants.KEY_ORDER_PAYMENT_MODE, "Pay on Delivery");

                                                    double total_payable = Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_SUB_TOTAL))
                                                            + Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TIP_AMOUNT));

                                                    preferenceManager.putString(Constants.KEY_ORDER_CONVENIENCE_FEE, String.valueOf(0));
                                                    preferenceManager.putString(Constants.KEY_ORDER_TOTAL_PAYABLE, String.valueOf(total_payable));

                                                    progressDialog.dismiss();

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
                                                    preferenceManager.putString(Constants.KEY_ORDER_INSTRUCTIONS, instruction);
                                                    preferenceManager.putString(Constants.KEY_ORDER_PAYMENT_MODE, "Pay on Delivery");

                                                    double total_payable = Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_SUB_TOTAL))
                                                            + Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TIP_AMOUNT));

                                                    preferenceManager.putString(Constants.KEY_ORDER_CONVENIENCE_FEE, String.valueOf(0));
                                                    preferenceManager.putString(Constants.KEY_ORDER_TOTAL_PAYABLE, String.valueOf(total_payable));

                                                    progressDialog.dismiss();

                                                    startActivity(new Intent(OrderAddressActivity.this, OrderSummaryActivity.class));
                                                    CustomIntent.customType(OrderAddressActivity.this, "left-to-right");
                                                    finish();
                                                }
                                            } else {
                                                progressDialog.dismiss();

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
                                            progressDialog.dismiss();

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

    private boolean isConnectedToInternet(OrderAddressActivity orderAddressActivity) {
        ConnectivityManager connectivityManager = (ConnectivityManager) orderAddressActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if ((wifiConn != null && wifiConn.isConnected()) || (mobileConn != null && mobileConn.isConnected())) {
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