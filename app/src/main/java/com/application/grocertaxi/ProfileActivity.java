package com.application.grocertaxi;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.application.grocertaxi.Helper.LoadingDialog;
import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.bumptech.glide.Glide;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.tapadoo.alerter.Alerter;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import dev.shreyaspatil.MaterialDialog.MaterialDialog;
import maes.tech.intentanim.CustomIntent;

public class ProfileActivity extends AppCompatActivity {

    private CircleImageView userProfilePic, choosePhoto;
    private TextView userName, userEmail, userMobile, deliveryAddress;
    private ConstraintLayout address, cart, orders, writeToUs, rateUs, inviteFriends,
            aboutUs, privacyPolicy, termsOfService, refundPolicy, appSettings, logout;
    private BottomNavigationView bottomBar;
    private FloatingActionButton cartBtn;
    private CardView cartIndicator;

    private PreferenceManager preferenceManager;
    private Uri profilePicUri = null;

    private FirebaseAuth firebaseAuth;
    private CollectionReference userRef, cartRef;
    private StorageReference storageReference;

    private String cart_location;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(ProfileActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(ProfileActivity.this, "fadein-to-fadeout");
            finish();
        } else if (preferenceManager.getString(Constants.KEY_USER_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_CITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).isEmpty()) {
            Intent intent = new Intent(ProfileActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(ProfileActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        loadingDialog = new LoadingDialog(ProfileActivity.this);

        cart_location = String.format("%s, %s", preferenceManager.getString(Constants.KEY_USER_LOCALITY), preferenceManager.getString(Constants.KEY_USER_CITY));

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
            deliveryAddress.setText("No address added yet. Add one!");
        } else {
            deliveryAddress.setText(preferenceManager.getString(Constants.KEY_USER_ADDRESS));
        }

        cartRef.whereEqualTo(Constants.KEY_CART_ITEM_LOCATION, cart_location).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.size() == 0) {
                cartIndicator.setVisibility(View.GONE);
            } else {
                cartIndicator.setVisibility(View.VISIBLE);
            }
        }).addOnFailureListener(e -> {
            Alerter.create(ProfileActivity.this)
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

    private void initViews() {
        userProfilePic = findViewById(R.id.user_profile_pic);
        choosePhoto = findViewById(R.id.choose_photo);
        userName = findViewById(R.id.user_name);
        userEmail = findViewById(R.id.user_email);
        userMobile = findViewById(R.id.user_mobile);

        address = findViewById(R.id.address);
        deliveryAddress = findViewById(R.id.delivery_address);
        cart = findViewById(R.id.cart);
        cartIndicator = findViewById(R.id.cart_indicator);
        orders = findViewById(R.id.orders);
        writeToUs = findViewById(R.id.write_us);
        rateUs = findViewById(R.id.rate_us);
        inviteFriends = findViewById(R.id.invite_friend);
        aboutUs = findViewById(R.id.about_us);
        privacyPolicy = findViewById(R.id.privacy_policy);
        termsOfService = findViewById(R.id.terms_of_service);
        refundPolicy = findViewById(R.id.refund_policy);
        appSettings = findViewById(R.id.app_settings);
        logout = findViewById(R.id.log_out);

        bottomBar = findViewById(R.id.bottom_bar);
        cartBtn = findViewById(R.id.cart_btn);
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        userRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS);
        storageReference = FirebaseStorage.getInstance().getReference("UserProfilePics/");
        cartRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .collection(Constants.KEY_COLLECTION_CART);
    }

    private void setActionOnViews() {
        if (preferenceManager.getString(Constants.KEY_USER_IMAGE).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_IMAGE).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_IMAGE).isEmpty() ||
                preferenceManager.getString(Constants.KEY_USER_IMAGE) == null) {
            userProfilePic.setImageResource(R.drawable.illustration_user_avatar);
        } else {
            Glide.with(ProfileActivity.this).load(preferenceManager.getString(Constants.KEY_USER_IMAGE)).centerCrop().into(userProfilePic);
            userProfilePic.setBorderWidth(0);
        }

        choosePhoto.setOnClickListener(v -> {
            if (!isConnectedToInternet(ProfileActivity.this)) {
                showConnectToInternetDialog();
                return;
            } else {
                MaterialDialog dialog = new MaterialDialog.Builder(ProfileActivity.this)
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

                            loadingDialog.startDialog();

                            userRef.document(preferenceManager.getString(Constants.KEY_USER_ID))
                                    .update(Constants.KEY_USER_IMAGE, "")
                                    .addOnSuccessListener(aVoid -> {
                                        loadingDialog.dismissDialog();

                                        preferenceManager.putString(Constants.KEY_USER_IMAGE, "");
                                        Alerter.create(ProfileActivity.this)
                                                .setText("Success! Your profile picture is removed.")
                                                .setTextAppearance(R.style.AlertText)
                                                .setBackgroundColorRes(R.color.successColor)
                                                .setIcon(R.drawable.ic_dialog_okay)
                                                .setDuration(3000)
                                                .enableIconPulse(true)
                                                .enableVibration(true)
                                                .disableOutsideTouch()
                                                .enableProgress(true)
                                                .setProgressColorInt(getColor(android.R.color.white))
                                                .show();
                                    })
                                    .addOnFailureListener(e -> {
                                        loadingDialog.dismissDialog();
                                        Alerter.create(ProfileActivity.this)
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
                        }).build();
                dialog.show();
            }
        });

        userName.setText(preferenceManager.getString(Constants.KEY_USER_NAME));
        userEmail.setText(preferenceManager.getString(Constants.KEY_USER_EMAIL));
        userMobile.setText(preferenceManager.getString(Constants.KEY_USER_MOBILE));

        ////////////////////////////////////////////////////////////////////////////////////////////

        address.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_SUBLOCALITY, "");
            preferenceManager.putString(Constants.KEY_LOCALITY, "");
            preferenceManager.putString(Constants.KEY_COUNTRY, "");
            preferenceManager.putString(Constants.KEY_PINCODE, "");
            preferenceManager.putString(Constants.KEY_LATITUDE, "");
            preferenceManager.putString(Constants.KEY_LONGITUDE, "");
            startActivity(new Intent(ProfileActivity.this, LocationPermissionActivity.class));
            CustomIntent.customType(ProfileActivity.this, "bottom-to-up");
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        cart.setOnClickListener(v -> {
            preferenceManager.putString(Constants.KEY_COUPON, "");
            preferenceManager.putString(Constants.KEY_COUPON_DISCOUNT_PERCENT, String.valueOf(0));

            preferenceManager.putString(Constants.KEY_ORDER_ID, "");
            preferenceManager.putString(Constants.KEY_ORDER_BY_USERID, "");
            preferenceManager.putString(Constants.KEY_ORDER_BY_USERNAME, "");
            preferenceManager.putString(Constants.KEY_ORDER_FROM_STOREID, "");
            preferenceManager.putString(Constants.KEY_ORDER_FROM_STORENAME, "");
            preferenceManager.putString(Constants.KEY_ORDER_CUSTOMER_NAME, "");
            preferenceManager.putString(Constants.KEY_ORDER_CUSTOMER_MOBILE, "");
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_LOCATION, "");
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_ADDRESS, "");
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_LATITUDE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_LONGITUDE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_DISTANCE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_NO_OF_ITEMS, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_TOTAL_MRP, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_TOTAL_RETAIL_PRICE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_COUPON_APPLIED, "");
            preferenceManager.putString(Constants.KEY_ORDER_COUPON_DISCOUNT, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_TOTAL_DISCOUNT, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_CHARGES, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_TIP_AMOUNT, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_SUB_TOTAL, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_PAYMENT_MODE, "");
            preferenceManager.putString(Constants.KEY_ORDER_CONVENIENCE_FEE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_TOTAL_PAYABLE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_INSTRUCTIONS, "");
            preferenceManager.putString(Constants.KEY_ORDER_STATUS, "");
            preferenceManager.putString(Constants.KEY_ORDER_PLACED_TIME, "");
            preferenceManager.putString(Constants.KEY_ORDER_COMPLETION_TIME, "");
            preferenceManager.putString(Constants.KEY_ORDER_CANCELLATION_TIME, "");
            preferenceManager.putString(Constants.KEY_ORDER_TIMESTAMP, "");

            startActivity(new Intent(getApplicationContext(), CartActivity.class));
            CustomIntent.customType(ProfileActivity.this, "bottom-to-up");
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        orders.setOnClickListener(view -> {
            startActivity(new Intent(ProfileActivity.this, OrdersHistoryActivity.class));
            CustomIntent.customType(ProfileActivity.this, "left-to-right");
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        writeToUs.setOnClickListener(v -> {
            Intent email = new Intent(Intent.ACTION_SENDTO);
            email.setData(Uri.parse("mailto:grocer.taxi@gmail.com"));
            startActivity(email);
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        rateUs.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + ProfileActivity.this.getPackageName())));
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=" + ProfileActivity.this.getPackageName())));
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        inviteFriends.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Grocer Taxi - Delivering Happiness!");
            String app_url = "https://play.google.com/store/apps/details?id=" + ProfileActivity.this.getPackageName();
            shareIntent.putExtra(Intent.EXTRA_TEXT, app_url);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        aboutUs.setOnClickListener(v -> {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(ProfileActivity.this);
            bottomSheetDialog.setContentView(R.layout.bottom_sheet_about);
            bottomSheetDialog.setCanceledOnTouchOutside(false);

            ImageView closeSheetBtn = bottomSheetDialog.findViewById(R.id.close_bottom_sheet_btn);
            closeSheetBtn.setOnClickListener(v12 -> bottomSheetDialog.dismiss());

            bottomSheetDialog.show();
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        privacyPolicy.setOnClickListener(view -> {
            String privacyPolicyUrl = "https://grocertaxi.wixsite.com/privacy-policy";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl));
            startActivity(browserIntent);
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        termsOfService.setOnClickListener(view -> {
            String privacyPolicyUrl = "https://grocertaxi.wixsite.com/terms-and-conditions";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl));
            startActivity(browserIntent);
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        refundPolicy.setOnClickListener(view -> {
            String privacyPolicyUrl = "https://grocertaxi.wixsite.com/refund-policy";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl));
            startActivity(browserIntent);
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        appSettings.setOnClickListener(v -> {
            Intent appInfoIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            appInfoIntent.addCategory(Intent.CATEGORY_DEFAULT);
            appInfoIntent.setData(Uri.parse("package:" + ProfileActivity.this.getPackageName()));
            startActivity(appInfoIntent);
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        logout.setOnClickListener(view -> {
            if (!isConnectedToInternet(ProfileActivity.this)) {
                showConnectToInternetDialog();
                return;
            } else {
                signOut();
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        bottomBar.setSelectedItemId(R.id.menu_profile);
        bottomBar.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_home:
                    startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                    CustomIntent.customType(ProfileActivity.this, "fadein-to-fadeout");
                    finish();
                    break;
                case R.id.menu_categories:
                    startActivity(new Intent(ProfileActivity.this, CategoriesActivity.class));
                    CustomIntent.customType(ProfileActivity.this, "fadein-to-fadeout");
                    break;
                case R.id.menu_stores:
                    startActivity(new Intent(ProfileActivity.this, StoresListActivity.class));
                    CustomIntent.customType(ProfileActivity.this, "fadein-to-fadeout");
                    break;
                case R.id.menu_profile:
                    break;
            }
            return true;
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        cartBtn.setOnClickListener(v -> {
            preferenceManager.putString(Constants.KEY_COUPON, "");
            preferenceManager.putString(Constants.KEY_COUPON_DISCOUNT_PERCENT, String.valueOf(0));

            preferenceManager.putString(Constants.KEY_ORDER_ID, "");
            preferenceManager.putString(Constants.KEY_ORDER_BY_USERID, "");
            preferenceManager.putString(Constants.KEY_ORDER_BY_USERNAME, "");
            preferenceManager.putString(Constants.KEY_ORDER_FROM_STOREID, "");
            preferenceManager.putString(Constants.KEY_ORDER_FROM_STORENAME, "");
            preferenceManager.putString(Constants.KEY_ORDER_CUSTOMER_NAME, "");
            preferenceManager.putString(Constants.KEY_ORDER_CUSTOMER_MOBILE, "");
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_LOCATION, "");
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_ADDRESS, "");
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_LATITUDE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_LONGITUDE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_DISTANCE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_NO_OF_ITEMS, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_TOTAL_MRP, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_TOTAL_RETAIL_PRICE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_COUPON_APPLIED, "");
            preferenceManager.putString(Constants.KEY_ORDER_COUPON_DISCOUNT, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_TOTAL_DISCOUNT, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_CHARGES, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_TIP_AMOUNT, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_SUB_TOTAL, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_PAYMENT_MODE, "");
            preferenceManager.putString(Constants.KEY_ORDER_CONVENIENCE_FEE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_TOTAL_PAYABLE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_INSTRUCTIONS, "");
            preferenceManager.putString(Constants.KEY_ORDER_STATUS, "");
            preferenceManager.putString(Constants.KEY_ORDER_PLACED_TIME, "");
            preferenceManager.putString(Constants.KEY_ORDER_COMPLETION_TIME, "");
            preferenceManager.putString(Constants.KEY_ORDER_CANCELLATION_TIME, "");
            preferenceManager.putString(Constants.KEY_ORDER_TIMESTAMP, "");

            startActivity(new Intent(getApplicationContext(), CartActivity.class));
            CustomIntent.customType(ProfileActivity.this, "bottom-to-up");
        });
    }

    private void selectImage() {
        ImagePicker.Companion.with(ProfileActivity.this)
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
            Glide.with(ProfileActivity.this).load(profilePicUri).centerCrop().into(userProfilePic);

            loadingDialog.startDialog();

            if (profilePicUri != null) {
                final StorageReference fileRef = storageReference.child(preferenceManager.getString(Constants.KEY_USER_ID) + ".img");

                fileRef.putFile(profilePicUri)
                        .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    final String imageValue = uri.toString();

                                    userRef.document(preferenceManager.getString(Constants.KEY_USER_ID))
                                            .update(Constants.KEY_USER_IMAGE, imageValue)
                                            .addOnSuccessListener(aVoid -> {
                                                loadingDialog.dismissDialog();

                                                preferenceManager.putString(Constants.KEY_USER_IMAGE, imageValue);
                                                Alerter.create(ProfileActivity.this)
                                                        .setText("Success! Your profile picture just got updated.")
                                                        .setTextAppearance(R.style.AlertText)
                                                        .setBackgroundColorRes(R.color.successColor)
                                                        .setIcon(R.drawable.ic_dialog_okay)
                                                        .setDuration(3000)
                                                        .enableIconPulse(true)
                                                        .enableVibration(true)
                                                        .disableOutsideTouch()
                                                        .enableProgress(true)
                                                        .setProgressColorInt(getColor(android.R.color.white))
                                                        .show();
                                            })
                                            .addOnFailureListener(e -> {
                                                loadingDialog.dismissDialog();
                                                Alerter.create(ProfileActivity.this)
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
                                })
                                .addOnFailureListener(e -> {
                                    loadingDialog.dismissDialog();
                                    Alerter.create(ProfileActivity.this)
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
                                }))
                        .addOnFailureListener(e -> {
                            loadingDialog.dismissDialog();
                            Alerter.create(ProfileActivity.this)
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
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Alerter.create(ProfileActivity.this)
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

    private void signOut() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(ProfileActivity.this)
                .setTitle("Log out of Grocer Taxi?")
                .setMessage("Are you sure of logging out of Grocer Taxi?")
                .setCancelable(false)
                .setPositiveButton("Yes", R.drawable.ic_dialog_okay, (dialogInterface, which) -> {
                    dialogInterface.dismiss();
                    loadingDialog.startDialog();
                    DocumentReference documentReference = userRef.document(preferenceManager.getString(Constants.KEY_USER_ID));

                    HashMap<String, Object> updates = new HashMap<>();
                    updates.put(Constants.KEY_USER_TOKEN, FieldValue.delete());
                    documentReference.update(updates)
                            .addOnSuccessListener(aVoid -> {
                                loadingDialog.dismissDialog();
                                firebaseAuth.signOut();
                                preferenceManager.clearPreferences();
                                Toast.makeText(ProfileActivity.this, "Logged Out!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(getApplicationContext(), WelcomeActivity.class));
                                CustomIntent.customType(ProfileActivity.this, "fadein-to-fadeout");
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                loadingDialog.dismissDialog();

                                Alerter.create(ProfileActivity.this)
                                        .setText("Whoa! Unable to log out. Try Again!")
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
                })
                .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
        materialDialog.show();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(ProfileActivity profileActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) profileActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(ProfileActivity.this)
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
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        CustomIntent.customType(ProfileActivity.this, "fadein-to-fadeout");
    }
}