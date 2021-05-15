package com.application.grocertaxi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.bumptech.glide.Glide;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.Random;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;
import maes.tech.intentanim.CustomIntent;

public class SignUpActivity extends AppCompatActivity {

    private ImageView closeBtn;
    private CircleImageView userProfilePic, choosePhoto;
    private TextInputLayout nameField, emailField, mobileField, createPasswordField, confirmPasswordField;
    private TextView signInBtn, privacyPolicy;
    private MaterialCheckBox privacyPolicyCheckBox;
    private ConstraintLayout sendOtpBtn;
    private CardView sendOtpBtnContainer;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;
    private CollectionReference userRef;

    private Uri profilePicUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());

        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(SignUpActivity.this, "fadein-to-fadeout");
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
        userProfilePic = findViewById(R.id.user_profile_pic);
        choosePhoto = findViewById(R.id.choose_photo);
        nameField = findViewById(R.id.sign_up_name_field);
        emailField = findViewById(R.id.sign_up_email_field);
        mobileField = findViewById(R.id.sign_up_mobile_field);
        createPasswordField = findViewById(R.id.sign_up_create_password_field);
        confirmPasswordField = findViewById(R.id.sign_up_confirm_password_field);
        privacyPolicyCheckBox = findViewById(R.id.privacy_policy_check_box);
        privacyPolicy = findViewById(R.id.privacy_policy);
        sendOtpBtnContainer = findViewById(R.id.send_otp_btn_container);
        sendOtpBtn = findViewById(R.id.send_otp_btn);
        progressBar = findViewById(R.id.progress_bar);
        signInBtn = findViewById(R.id.sign_in_btn);
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        userRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS);
    }

    private void setActionOnViews() {
        closeBtn.setOnClickListener(view -> onBackPressed());

        KeyboardVisibilityEvent.setEventListener(SignUpActivity.this, isOpen -> {
            if (!isOpen) {
                nameField.clearFocus();
                emailField.clearFocus();
                mobileField.clearFocus();
                createPasswordField.clearFocus();
                confirmPasswordField.clearFocus();
            }
        });

        choosePhoto.setOnClickListener(v -> {
            MaterialDialog dialog = new MaterialDialog.Builder(SignUpActivity.this)
                    .setTitle("Edit Profile Photo")
                    .setMessage("Choose an action to continue!")
                    .setCancelable(false)
                    .setAnimation(R.raw.choose_photo)
                    .setPositiveButton("Edit", R.drawable.ic_dialog_camera, (dialogInterface, which) -> {
                        dialogInterface.dismiss();
                        selectImage();
                    })
                    .setNegativeButton("Remove", R.drawable.ic_dialog_remove, (dialogInterface, which) -> {
                        dialogInterface.dismiss();
                        profilePicUri = null;
                        userProfilePic.setImageResource(R.drawable.illustration_user_avatar);
                    }).build();
            dialog.show();
        });

        privacyPolicy.setOnClickListener(view -> {
            progressBar.setVisibility(View.GONE);
            sendOtpBtnContainer.setVisibility(View.VISIBLE);
            sendOtpBtn.setEnabled(true);

            String privacyPolicyUrl = "https://grocertaxi.wixsite.com/privacy-policy";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl));
            startActivity(browserIntent);
        });

        sendOtpBtn.setOnClickListener(view -> {
            UIUtil.hideKeyboard(SignUpActivity.this);

            final String email = emailField.getEditText().getText().toString().toLowerCase().trim();
            final String mobile = mobileField.getPrefixText().toString().trim() + mobileField.getEditText().getText().toString().trim();

            if (!validateName() | !validateEmail() | !validateMobile() | !validatePasswords()) {
                return;
            } else {
                if (!privacyPolicyCheckBox.isChecked()) {
                    YoYo.with(Techniques.Shake).duration(700).repeat(0).playOn(privacyPolicyCheckBox);
                    YoYo.with(Techniques.Shake).duration(700).repeat(0).playOn(privacyPolicy);
                    Alerter.create(SignUpActivity.this)
                            .setText("Review and accept the privacy policy first!")
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
                } else {
                    if (!isConnectedToInternet(SignUpActivity.this)) {
                        showConnectToInternetDialog();
                        return;
                    } else {
                        sendOtpBtnContainer.setVisibility(View.INVISIBLE);
                        sendOtpBtn.setEnabled(false);
                        progressBar.setVisibility(View.VISIBLE);

                        firebaseAuth.fetchSignInMethodsForEmail(email)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        if (task.getResult().getSignInMethods().isEmpty() &&
                                                task.getResult().getSignInMethods().size() == 0) {
                                            userRef.whereEqualTo(Constants.KEY_USER_MOBILE, mobile)
                                                    .get().addOnCompleteListener(task1 -> {
                                                if (task1.isSuccessful()) {
                                                    if (task1.getResult().getDocuments().isEmpty() &&
                                                            task1.getResult().getDocuments().size() == 0) {
                                                        openVerifyOTP();
                                                    } else {
                                                        progressBar.setVisibility(View.GONE);
                                                        sendOtpBtnContainer.setVisibility(View.VISIBLE);
                                                        sendOtpBtn.setEnabled(true);

                                                        YoYo.with(Techniques.Shake).duration(700).repeat(0).playOn(mobileField);
                                                        Alerter.create(SignUpActivity.this)
                                                                .setText("That mobile has already been registered. Try another!")
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
                                                    progressBar.setVisibility(View.GONE);
                                                    sendOtpBtnContainer.setVisibility(View.VISIBLE);
                                                    sendOtpBtn.setEnabled(true);

                                                    Alerter.create(SignUpActivity.this)
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
                                            }).addOnFailureListener(e -> {
                                                progressBar.setVisibility(View.GONE);
                                                sendOtpBtnContainer.setVisibility(View.VISIBLE);
                                                sendOtpBtn.setEnabled(true);

                                                Alerter.create(SignUpActivity.this)
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
                                            progressBar.setVisibility(View.GONE);
                                            sendOtpBtnContainer.setVisibility(View.VISIBLE);
                                            sendOtpBtn.setEnabled(true);

                                            YoYo.with(Techniques.Shake).duration(700).repeat(0).playOn(emailField);
                                            Alerter.create(SignUpActivity.this)
                                                    .setText("That email has already been registered. Try another!")
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
                                        progressBar.setVisibility(View.GONE);
                                        sendOtpBtnContainer.setVisibility(View.VISIBLE);
                                        sendOtpBtn.setEnabled(true);

                                        Alerter.create(SignUpActivity.this)
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
                                }).addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            sendOtpBtnContainer.setVisibility(View.VISIBLE);
                            sendOtpBtn.setEnabled(true);

                            Alerter.create(SignUpActivity.this)
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
            }
        });

        signInBtn.setOnClickListener(view -> {
            progressBar.setVisibility(View.GONE);
            sendOtpBtnContainer.setVisibility(View.VISIBLE);
            sendOtpBtn.setEnabled(true);

            startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
            CustomIntent.customType(SignUpActivity.this, "bottom-to-up");
            finish();
        });
    }

    private boolean validateName() {
        String name = nameField.getEditText().getText().toString().trim();

        if (name.isEmpty()) {
            nameField.setError("Enter your name!");
            nameField.requestFocus();
            return false;
        } else {
            nameField.setError(null);
            nameField.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateEmail() {
        String email = emailField.getEditText().getText().toString().toLowerCase().trim();

        if (email.isEmpty()) {
            emailField.setError("Enter an email to set up account!");
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

    private boolean validateMobile() {
        String mobile = mobileField.getEditText().getText().toString().trim();

        if (mobile.isEmpty()) {
            mobileField.setError("Enter a mobile to verify account!");
            mobileField.requestFocus();
            return false;
        } else if (mobile.length() != 10) {
            mobileField.setError("Invalid mobile. Try Again!");
            mobileField.requestFocus();
            return false;
        } else {
            mobileField.setError(null);
            mobileField.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validatePasswords() {
        String createPassword = createPasswordField.getEditText().getText().toString().trim();
        String confirmPassword = confirmPasswordField.getEditText().getText().toString().trim();

        Pattern PASSWORD_PATTERN = Pattern.compile("^" +
                "(?=.*[0-9])" +                 //at least 1 digit
                "(?=.*[a-z])" +                 //at least 1 lowercase letter
                "(?=.*[A-Z])" +                 //at least 1 uppercase letter
                "(?=.*[!@#$%^&*+=_])" +         //at least 1 special character
                "(?=\\S+$)" +                   //no white spaces
                ".{6,}" +                       //at least 6-character long
                "$");

        if (createPassword.isEmpty()) {
            createPasswordField.setError("Create a password for the account!");
            createPasswordField.requestFocus();
            return false;
        } else if (confirmPassword.isEmpty()) {
            confirmPasswordField.setError("Confirm your created password!");
            confirmPasswordField.requestFocus();
            return false;
        } else if (!PASSWORD_PATTERN.matcher(createPassword).matches()) {
            createPasswordField.setError("Invalid! Requires a minimum of 6 characters with no white spaces, at least 1 digit, 1 lowercase letter, 1 uppercase letter and 1 special character.");
            createPasswordField.requestFocus();
            return false;
        } else if (!confirmPassword.equals(createPassword)) {
            createPasswordField.setError("Ahan! Those passwords didn't match.");
            confirmPasswordField.setError("Ahan! Those passwords didn't match.");
            confirmPasswordField.requestFocus();
            return false;
        } else {
            createPasswordField.setError(null);
            createPasswordField.setErrorEnabled(false);
            confirmPasswordField.setError(null);
            confirmPasswordField.setErrorEnabled(false);
            return true;
        }
    }

    private void selectImage() {
        ImagePicker.Companion.with(SignUpActivity.this)
                .cropSquare()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            profilePicUri = data.getData();
            Glide.with(SignUpActivity.this).load(profilePicUri).centerCrop().into(userProfilePic);
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Alerter.create(SignUpActivity.this)
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
            return;
        }
    }

    private void openVerifyOTP() {
        final String name = nameField.getEditText().getText().toString().trim();
        final String email = emailField.getEditText().getText().toString().toLowerCase().trim();
        final String mobile = mobileField.getPrefixText().toString().trim() + mobileField.getEditText().getText().toString().trim();
        final String createPassword = createPasswordField.getEditText().getText().toString().trim();

        Random random = new Random();
        int number1 = random.nextInt(9000) + 1000;
        int number2 = random.nextInt(9000) + 1000;
        int number3 = random.nextInt(9000) + 1000;

        if (profilePicUri != null) {
            progressBar.setVisibility(View.GONE);
            sendOtpBtnContainer.setVisibility(View.VISIBLE);
            sendOtpBtn.setEnabled(true);

            Intent intent = new Intent(SignUpActivity.this, VerifyOTPActivity.class);
            intent.putExtra("user_id", String.format("USER-%d%d%d", number1, number2, number3));
            intent.putExtra("user_name", name);
            intent.putExtra("user_email", email);
            intent.putExtra("user_mobile", mobile);
            intent.putExtra("user_password", createPassword);
            intent.putExtra("user_image", profilePicUri.toString());
            startActivity(intent);
            CustomIntent.customType(SignUpActivity.this, "left-to-right");
            finish();
        } else {
            progressBar.setVisibility(View.GONE);
            sendOtpBtnContainer.setVisibility(View.VISIBLE);
            sendOtpBtn.setEnabled(true);

            Intent intent = new Intent(SignUpActivity.this, VerifyOTPActivity.class);
            intent.putExtra("user_id", String.format("USER-%d%d%d", number1, number2, number3));
            intent.putExtra("user_name", name);
            intent.putExtra("user_email", email);
            intent.putExtra("user_mobile", mobile);
            intent.putExtra("user_password", createPassword);
            intent.putExtra("user_image", "");
            startActivity(intent);
            CustomIntent.customType(SignUpActivity.this, "left-to-right");
            finish();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(SignUpActivity signUpActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) signUpActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(SignUpActivity.this)
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
        KeyboardVisibilityEvent.setEventListener(SignUpActivity.this, isOpen -> {
            if (isOpen) {
                UIUtil.hideKeyboard(SignUpActivity.this);
            }
        });
        finishAffinity();
    }
}