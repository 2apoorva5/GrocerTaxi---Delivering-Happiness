package com.application.grocertaxi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import dmax.dialog.SpotsDialog;
import maes.tech.intentanim.CustomIntent;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ImageView closeBtn;
    private TextInputLayout emailField;
    private ConstraintLayout sendLinkBtn;

    private FirebaseAuth firebaseAuth;

    private PreferenceManager preferenceManager;
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(ForgotPasswordActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(ForgotPasswordActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        progressDialog = new SpotsDialog.Builder().setContext(ForgotPasswordActivity.this)
                .setMessage("Hold on..")
                .setCancelable(false)
                .setTheme(R.style.SpotsDialog)
                .build();

        initViews();
        initFirebase();
        setActionOnViews();
    }

    private void initViews() {
        closeBtn = findViewById(R.id.close_btn);
        emailField = findViewById(R.id.email_field);
        sendLinkBtn = findViewById(R.id.send_link_btn);
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    private void setActionOnViews() {
        closeBtn.setOnClickListener(v -> onBackPressed());

        KeyboardVisibilityEvent.setEventListener(ForgotPasswordActivity.this, isOpen -> {
            if (!isOpen) {
                emailField.clearFocus();
            }
        });

        sendLinkBtn.setOnClickListener(v -> {
            UIUtil.hideKeyboard(ForgotPasswordActivity.this);

            final String email = emailField.getEditText().getText().toString().toLowerCase().trim();

            if (!validateEmail()) {
                return;
            } else {
                if (!isConnectedToInternet(ForgotPasswordActivity.this)) {
                    showConnectToInternetDialog();
                    return;
                } else {
                    progressDialog.show();

                    firebaseAuth.fetchSignInMethodsForEmail(email)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    if (!task.getResult().getSignInMethods().isEmpty() &&
                                            task.getResult().getSignInMethods().size() != 0) {
                                        firebaseAuth.sendPasswordResetEmail(email)
                                                .addOnSuccessListener(aVoid -> {
                                                    progressDialog.dismiss();

                                                    Toast.makeText(ForgotPasswordActivity.this, "Check your email for password reset link", Toast.LENGTH_LONG).show();

                                                    onBackPressed();
                                                    finish();
                                                }).addOnFailureListener(e -> {
                                            progressDialog.dismiss();

                                            Alerter.create(ForgotPasswordActivity.this)
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
                                        progressDialog.dismiss();

                                        YoYo.with(Techniques.Shake).duration(700).repeat(0).playOn(emailField);
                                        Alerter.create(ForgotPasswordActivity.this)
                                                .setText("You don't have any account with that email!")
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

                                    Alerter.create(ForgotPasswordActivity.this)
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
            }
        });
    }

    private boolean validateEmail() {
        String email = emailField.getEditText().getText().toString().toLowerCase().trim();

        if (email.isEmpty()) {
            emailField.setError("Enter an email!");
            emailField.requestFocus();
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.setError("Invalid email. Try again!");
            emailField.requestFocus();
            return false;
        } else {
            emailField.setError(null);
            emailField.setErrorEnabled(false);
            return true;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(ForgotPasswordActivity forgotPasswordActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) forgotPasswordActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(ForgotPasswordActivity.this)
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
        KeyboardVisibilityEvent.setEventListener(ForgotPasswordActivity.this, isOpen -> {
            if (isOpen) {
                UIUtil.hideKeyboard(ForgotPasswordActivity.this);
            }
        });
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        CustomIntent.customType(ForgotPasswordActivity.this, "up-to-bottom");
    }
}