package com.application.grocertaxi;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import maes.tech.intentanim.CustomIntent;

public class DeliveryAddressActivity extends AppCompatActivity {

    private ImageView closeBtn;
    private TextInputLayout addressLine1, addressLine2, landmark, pinCode, city, state;
    private CardView saveBtnContainer;
    private ConstraintLayout saveBtn;
    private TextView pinBtn;
    private ProgressBar progressBar;

    private PreferenceManager preferenceManager;

    private CollectionReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_address);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(DeliveryAddressActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(DeliveryAddressActivity.this, "fadein-to-fadeout");
            finish();
        } else if (preferenceManager.getString(Constants.KEY_USER_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_CITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).isEmpty()) {
            Intent intent = new Intent(DeliveryAddressActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(DeliveryAddressActivity.this, "fadein-to-fadeout");
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
        addressLine1 = findViewById(R.id.address_line1);
        addressLine2 = findViewById(R.id.address_line2);
        landmark = findViewById(R.id.landmark);
        pinCode = findViewById(R.id.pin_code);
        city = findViewById(R.id.city);
        state = findViewById(R.id.state);
        saveBtnContainer = findViewById(R.id.save_btn_container);
        saveBtn = findViewById(R.id.save_btn);
        progressBar = findViewById(R.id.progress_bar);
        pinBtn = findViewById(R.id.pin_btn);
    }

    private void initFirebase() {
        userRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS);
    }

    private void setActionOnViews() {
        closeBtn.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });

        KeyboardVisibilityEvent.setEventListener(DeliveryAddressActivity.this, isOpen -> {
            if (!isOpen) {
                addressLine1.clearFocus();
                addressLine2.clearFocus();
                landmark.clearFocus();
                pinCode.clearFocus();
                city.clearFocus();
                state.clearFocus();
            }
        });

        state.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveBtn.performClick();
                return true;
            }
            return false;
        });

        saveBtn.setOnClickListener(view -> {
            UIUtil.hideKeyboard(DeliveryAddressActivity.this);

            final String address_line1 = addressLine1.getEditText().getText().toString().trim();
            final String address_line2 = addressLine2.getEditText().getText().toString().trim();
            final String landmark_loc = landmark.getEditText().getText().toString().trim();
            final String pin_code = pinCode.getEditText().getText().toString().trim();
            final String city_name = city.getEditText().getText().toString().trim();
            final String state_name = state.getEditText().getText().toString().trim();

            final String delivery_address = (String.format("%s, %s, %s, %s, %s - %s", address_line1, address_line2, landmark_loc, city_name, state_name, pin_code));

            if (!validateAddressLine() | !validatePinCode() | !validateCity() | !validateState()) {
                return;
            } else {
                if (!isConnectedToInternet(DeliveryAddressActivity.this)) {
                    showConnectToInternetDialog();
                    return;
                } else {
                    saveBtnContainer.setVisibility(View.INVISIBLE);
                    saveBtn.setEnabled(false);
                    progressBar.setVisibility(View.VISIBLE);

                    userRef.document(preferenceManager.getString(Constants.KEY_USER_ID))
                            .update(Constants.KEY_USER_ADDRESS, delivery_address)
                            .addOnSuccessListener(aVoid -> {
                                progressBar.setVisibility(View.GONE);
                                saveBtnContainer.setVisibility(View.VISIBLE);
                                saveBtn.setEnabled(true);

                                preferenceManager.putString(Constants.KEY_USER_ADDRESS, delivery_address);

                                onBackPressed();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                saveBtnContainer.setVisibility(View.VISIBLE);
                                saveBtn.setEnabled(true);

                                Alerter.create(DeliveryAddressActivity.this)
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

        pinBtn.setOnClickListener(v -> {
            startActivity(new Intent(DeliveryAddressActivity.this, LocationPermissionActivity.class));
            CustomIntent.customType(DeliveryAddressActivity.this, "left-to-right");
            finish();
        });
    }

    private boolean validateAddressLine() {
        String address_line = addressLine1.getEditText().getText().toString().trim();

        if (address_line.isEmpty()) {
            addressLine1.setError("Enter a delivery address!");
            addressLine1.requestFocus();
            return false;
        } else {
            addressLine1.setError(null);
            addressLine1.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validatePinCode() {
        String pin_code = pinCode.getEditText().getText().toString().trim();

        if (pin_code.isEmpty()) {
            pinCode.setError("Enter a Pin Code!");
            pinCode.requestFocus();
            return false;
        } else if (pin_code.length() != 6) {
            pinCode.setError("Invalid Pin Code!");
            pinCode.requestFocus();
            return false;
        } else {
            pinCode.setError(null);
            pinCode.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateCity() {
        String city_name = city.getEditText().getText().toString().trim();

        if (city_name.isEmpty()) {
            city.setError("Enter a city!");
            city.requestFocus();
            return false;
        } else {
            city.setError(null);
            city.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateState() {
        String state_name = state.getEditText().getText().toString().trim();

        if (state_name.isEmpty()) {
            state.setError("Enter a state!");
            state.requestFocus();
            return false;
        } else {
            state.setError(null);
            state.setErrorEnabled(false);
            return true;
        }
    }

    private boolean isConnectedToInternet(DeliveryAddressActivity deliveryAddressActivity) {
        ConnectivityManager connectivityManager = (ConnectivityManager) deliveryAddressActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if ((wifiConn != null && wifiConn.isConnected()) || (mobileConn != null && mobileConn.isConnected())) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(DeliveryAddressActivity.this)
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
        KeyboardVisibilityEvent.setEventListener(DeliveryAddressActivity.this, isOpen -> {
            if (isOpen) {
                UIUtil.hideKeyboard(DeliveryAddressActivity.this);
            }
        });
        CustomIntent.customType(DeliveryAddressActivity.this, "up-to-bottom");
    }
}