package com.application.grocertaxi;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.List;

import maes.tech.intentanim.CustomIntent;

public class SignInActivity extends AppCompatActivity {

    private ImageView closeBtn;
    private TextInputLayout emailOrMobileField, passwordField;
    private TextView forgotPassword, signUpBtn, privacyPolicy;
    private MaterialCheckBox privacyPolicyCheckBox;
    private ConstraintLayout signInBtn;
    private CardView signInBtnContainer;
    private ProgressBar signInProgressBar;

    private FirebaseAuth firebaseAuth;
    private CollectionReference userRef;

    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(SignInActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(SignInActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        initViews();
        initFirebase();
        setActionOnViews();
    }

    private void initViews() {
        closeBtn = findViewById(R.id.close_btn);
        emailOrMobileField = findViewById(R.id.sign_in_email_or_mobile_field);
        passwordField = findViewById(R.id.sign_in_password_field);
        forgotPassword = findViewById(R.id.forgot_password);
        privacyPolicyCheckBox = findViewById(R.id.privacy_policy_check_box);
        privacyPolicy = findViewById(R.id.privacy_policy);
        signInBtnContainer = findViewById(R.id.sign_in_btn_container);
        signInBtn = findViewById(R.id.sign_in_btn);
        signInProgressBar = findViewById(R.id.sign_in_progress_bar);
        signUpBtn = findViewById(R.id.sign_up_btn);
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        userRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS);
    }

    private void setActionOnViews() {
        closeBtn.setOnClickListener(view -> onBackPressed());

        KeyboardVisibilityEvent.setEventListener(SignInActivity.this, isOpen -> {
            if (!isOpen) {
                emailOrMobileField.clearFocus();
                passwordField.clearFocus();
            }
        });

        forgotPassword.setOnClickListener(view -> {
            signInProgressBar.setVisibility(View.GONE);
            signInBtnContainer.setVisibility(View.VISIBLE);
            signInBtn.setEnabled(true);
        });

        signInBtn.setOnClickListener(view -> {
            UIUtil.hideKeyboard(SignInActivity.this);

            final String emailOrMobile = emailOrMobileField.getEditText().getText().toString().toLowerCase().trim();

            if(!validateEmailOrMobile() | !validatePassword()) {
                return;
            } else {
                if (!privacyPolicyCheckBox.isChecked()) {
                    YoYo.with(Techniques.Shake).duration(700).repeat(0).playOn(privacyPolicyCheckBox);
                    YoYo.with(Techniques.Shake).duration(700).repeat(0).playOn(privacyPolicy);
                    Alerter.create(SignInActivity.this)
                            .setText("Review and accept that privacy policy first!")
                            .setTextAppearance(R.style.AlertText)
                            .setBackgroundColorRes(R.color.infoColor)
                            .setIcon(R.drawable.ic_info)
                            .setDuration(3000)
                            .enableIconPulse(true)
                            .enableVibration(true)
                            .disableOutsideTouch()
                            .enableProgress(true)
                            .setProgressColorInt(getColor(android.R.color.white))
                            .show();
                    return;
                } else {
                    if (Patterns.EMAIL_ADDRESS.matcher(emailOrMobile).matches()) {
                        emailOrMobileField.setError(null);
                        emailOrMobileField.setErrorEnabled(false);

                        if (!isConnectedToInternet(SignInActivity.this)) {
                            showConnectToInternetDialog();
                            return;
                        } else {
                            login(emailOrMobile);
                        }
                    } else if(emailOrMobile.matches("\\d{10}")) {
                        emailOrMobileField.setError(null);
                        emailOrMobileField.setErrorEnabled(false);

                        if (!isConnectedToInternet(SignInActivity.this)) {
                            showConnectToInternetDialog();
                            return;
                        } else {
                            signInBtnContainer.setVisibility(View.INVISIBLE);
                            signInBtn.setEnabled(false);
                            signInProgressBar.setVisibility(View.VISIBLE);

                            userRef.whereEqualTo(Constants.KEY_USER_MOBILE, "+91" + emailOrMobile)
                                    .get().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    List<DocumentSnapshot> documentSnapshots = task.getResult().getDocuments();
                                    if (documentSnapshots.isEmpty()) {
                                        signInProgressBar.setVisibility(View.GONE);
                                        signInBtnContainer.setVisibility(View.VISIBLE);
                                        signInBtn.setEnabled(true);

                                        YoYo.with(Techniques.Shake).duration(700).repeat(0).playOn(emailOrMobileField);
                                        Alerter.create(SignInActivity.this)
                                                .setText("We didn't find any account with that mobile!")
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
                                    } else {
                                        String email = documentSnapshots.get(0).getString(Constants.KEY_USER_EMAIL).trim();
                                        login(email);
                                    }
                                } else {
                                    signInProgressBar.setVisibility(View.GONE);
                                    signInBtnContainer.setVisibility(View.VISIBLE);
                                    signInBtn.setEnabled(true);

                                    Alerter.create(SignInActivity.this)
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
                            }).addOnFailureListener(e -> {
                                signInProgressBar.setVisibility(View.GONE);
                                signInBtnContainer.setVisibility(View.VISIBLE);
                                signInBtn.setEnabled(true);

                                Alerter.create(SignInActivity.this)
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
                    } else {
                        YoYo.with(Techniques.Shake).duration(700).repeat(0).playOn(emailOrMobileField);
                        emailOrMobileField.setError("Enter a valid email or mobile!");
                        emailOrMobileField.requestFocus();
                    }
                }
            }
        });

        signUpBtn.setOnClickListener(view -> {
            signInProgressBar.setVisibility(View.GONE);
            signInBtnContainer.setVisibility(View.VISIBLE);
            signInBtn.setEnabled(true);

            startActivity(new Intent(SignInActivity.this, SignUpActivity.class));
            CustomIntent.customType(SignInActivity.this, "bottom-to-up");
            finish();
        });
    }

    private boolean validateEmailOrMobile() {
        String emailOrMobile = emailOrMobileField.getEditText().getText().toString().toLowerCase().trim();

        if (emailOrMobile.isEmpty()) {
            emailOrMobileField.setError("Enter your email or mobile!");
            emailOrMobileField.requestFocus();
            return false;
        } else {
            emailOrMobileField.setError(null);
            emailOrMobileField.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validatePassword() {
        String password = passwordField.getEditText().getText().toString().trim();

        if (password.isEmpty()) {
            passwordField.setError("Without a password? Eh!");
            passwordField.requestFocus();
            return false;
        } else {
            passwordField.setError(null);
            passwordField.setErrorEnabled(false);
            return true;
        }
    }

    private void login(final String email) {
        signInBtnContainer.setVisibility(View.INVISIBLE);
        signInBtn.setEnabled(false);
        signInProgressBar.setVisibility(View.VISIBLE);

        firebaseAuth.signInWithEmailAndPassword(email, passwordField.getEditText().getText().toString().trim())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userRef.whereEqualTo(Constants.KEY_USER_EMAIL, email)
                                .get()
                                .addOnCompleteListener(task1 -> {
                                    if(task1.isSuccessful()) {
                                        signInProgressBar.setVisibility(View.GONE);
                                        signInBtnContainer.setVisibility(View.VISIBLE);
                                        signInBtn.setEnabled(true);

                                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                                        preferenceManager.putString(Constants.KEY_UID, task1.getResult().getDocuments().get(0).getString(Constants.KEY_UID));
                                        preferenceManager.putString(Constants.KEY_USER_ID, task1.getResult().getDocuments().get(0).getString(Constants.KEY_USER_ID));
                                        preferenceManager.putString(Constants.KEY_USER_NAME, task1.getResult().getDocuments().get(0).getString(Constants.KEY_USER_NAME));
                                        preferenceManager.putString(Constants.KEY_USER_EMAIL, task1.getResult().getDocuments().get(0).getString(Constants.KEY_USER_EMAIL));
                                        preferenceManager.putString(Constants.KEY_USER_MOBILE, task1.getResult().getDocuments().get(0).getString(Constants.KEY_USER_MOBILE));
                                        preferenceManager.putString(Constants.KEY_USER_IMAGE, task1.getResult().getDocuments().get(0).getString(Constants.KEY_USER_IMAGE));
                                        preferenceManager.putString(Constants.KEY_USER_ADDRESS, task1.getResult().getDocuments().get(0).getString(Constants.KEY_USER_ADDRESS));
                                        preferenceManager.putString(Constants.KEY_CITY, "");
                                        preferenceManager.putString(Constants.KEY_LOCALITY, "");
                                        preferenceManager.putString(Constants.KEY_ADDRESS_LINE1, "");
                                        preferenceManager.putString(Constants.KEY_ADDRESS_LINE2, "");
                                        preferenceManager.putString(Constants.KEY_LANDMARK, "");
                                        preferenceManager.putString(Constants.KEY_PINCODE, "");
                                        preferenceManager.putString(Constants.KEY_CITY_NAME, "");
                                        preferenceManager.putString(Constants.KEY_STATE_NAME, "");

                                        Intent intent = new Intent(getApplicationContext(), ChooseCityActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        CustomIntent.customType(SignInActivity.this, "fadein-to-fadeout");
                                        finish();
                                    } else {
                                        signInProgressBar.setVisibility(View.GONE);
                                        signInBtnContainer.setVisibility(View.VISIBLE);
                                        signInBtn.setEnabled(true);

                                        Alerter.create(SignInActivity.this)
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
                                }).addOnFailureListener(e -> {
                                    signInProgressBar.setVisibility(View.GONE);
                                    signInBtnContainer.setVisibility(View.VISIBLE);
                                    signInBtn.setEnabled(true);

                                    Alerter.create(SignInActivity.this)
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
                    } else {
                        signInProgressBar.setVisibility(View.GONE);
                        signInBtnContainer.setVisibility(View.VISIBLE);
                        signInBtn.setEnabled(true);

                        Alerter.create(SignInActivity.this)
                                .setText("Whoa! It seems you've got invalid credentials. Try again!")
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
                }).addOnFailureListener(e -> {
            signInProgressBar.setVisibility(View.GONE);
            signInBtnContainer.setVisibility(View.VISIBLE);
            signInBtn.setEnabled(true);

            Alerter.create(SignInActivity.this)
                    .setText("Whoa! It seems you've got invalid credentials. Try again!")
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

    private boolean isConnectedToInternet(SignInActivity signInActivity) {
        ConnectivityManager connectivityManager = (ConnectivityManager) signInActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if ((wifiConn != null && wifiConn.isConnected()) || (mobileConn != null && mobileConn.isConnected())) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(SignInActivity.this)
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
        finishAffinity();
    }
}