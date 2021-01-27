package com.application.grocertaxi;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

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

public class UpdateAddressActivity extends AppCompatActivity {

    private ImageView closeBtn;
    private TextInputLayout addressLine1, addressLine2, landmark, pinCode, city, state;
    private CardView saveBtnContainer;
    private ConstraintLayout saveBtn;
    private ProgressBar updateAddressProgressBar;

    private PreferenceManager preferenceManager;

    private CollectionReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_address);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(UpdateAddressActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(UpdateAddressActivity.this, "fadein-to-fadeout");
            finish();
        } else if(preferenceManager.getString(Constants.KEY_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_CITY) == null ||
                preferenceManager.getString(Constants.KEY_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_LOCALITY).equals("") ||
                preferenceManager.getString(Constants.KEY_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_LOCALITY).isEmpty()) {
            Intent intent = new Intent(UpdateAddressActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(UpdateAddressActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        initViews();
        initFirebase();
        setActionOnViews();
    }

    private void initViews(){
        closeBtn = findViewById(R.id.close_btn);
        addressLine1 = findViewById(R.id.address_line1);
        addressLine2 = findViewById(R.id.address_line2);
        landmark = findViewById(R.id.landmark);
        pinCode = findViewById(R.id.pin_code);
        city = findViewById(R.id.city);
        state = findViewById(R.id.state);
        saveBtnContainer = findViewById(R.id.save_btn_container);
        saveBtn = findViewById(R.id.save_btn);
        updateAddressProgressBar = findViewById(R.id.update_address_progress_bar);
    }

    private void initFirebase(){
        userRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS);
    }

    private void setActionOnViews(){
        closeBtn.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });

        KeyboardVisibilityEvent.setEventListener(UpdateAddressActivity.this, isOpen -> {
            if (!isOpen) {
                addressLine1.clearFocus();
                addressLine2.clearFocus();
                landmark.clearFocus();
                pinCode.clearFocus();
                city.clearFocus();
                state.clearFocus();
            }
        });

        addressLine1.getEditText().setText(preferenceManager.getString(Constants.KEY_ADDRESS_LINE1));
        addressLine2.getEditText().setText(preferenceManager.getString(Constants.KEY_ADDRESS_LINE2));
        landmark.getEditText().setText(preferenceManager.getString(Constants.KEY_LANDMARK));
        pinCode.getEditText().setText(preferenceManager.getString(Constants.KEY_PINCODE));
        city.getEditText().setText(preferenceManager.getString(Constants.KEY_CITY_NAME));
        state.getEditText().setText(preferenceManager.getString(Constants.KEY_STATE_NAME));

        saveBtn.setOnClickListener(view -> {
            UIUtil.hideKeyboard(UpdateAddressActivity.this);

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
                if (!isConnectedToInternet(UpdateAddressActivity.this)) {
                    showConnectToInternetDialog();
                    return;
                } else {
                    saveBtnContainer.setVisibility(View.INVISIBLE);
                    saveBtn.setEnabled(false);
                    updateAddressProgressBar.setVisibility(View.VISIBLE);

                    userRef.document(preferenceManager.getString(Constants.KEY_USER_ID))
                            .update(Constants.KEY_USER_ADDRESS, delivery_address)
                            .addOnSuccessListener(aVoid -> {
                                updateAddressProgressBar.setVisibility(View.GONE);
                                saveBtnContainer.setVisibility(View.VISIBLE);
                                saveBtn.setEnabled(true);

                                preferenceManager.putString(Constants.KEY_USER_ADDRESS, delivery_address);
                                preferenceManager.putString(Constants.KEY_ADDRESS_LINE1, address_line1);
                                preferenceManager.putString(Constants.KEY_ADDRESS_LINE2, address_line2);
                                preferenceManager.putString(Constants.KEY_LANDMARK, landmark_loc);
                                preferenceManager.putString(Constants.KEY_PINCODE, pin_code);
                                preferenceManager.putString(Constants.KEY_CITY_NAME, city_name);
                                preferenceManager.putString(Constants.KEY_STATE_NAME, state_name);

                                onBackPressed();
                            })
                            .addOnFailureListener(e -> {
                                updateAddressProgressBar.setVisibility(View.GONE);
                                saveBtnContainer.setVisibility(View.VISIBLE);
                                saveBtn.setEnabled(true);

                                Alerter.create(UpdateAddressActivity.this)
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
                            });
                }
            }
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
        } else if(pin_code.length() != 6) {
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

    private boolean isConnectedToInternet(UpdateAddressActivity updateAddressActivity) {
        ConnectivityManager connectivityManager = (ConnectivityManager) updateAddressActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if ((wifiConn != null && wifiConn.isConnected()) || (mobileConn != null && mobileConn.isConnected())) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(UpdateAddressActivity.this)
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
        CustomIntent.customType(UpdateAddressActivity.this, "up-to-bottom");
    }
}