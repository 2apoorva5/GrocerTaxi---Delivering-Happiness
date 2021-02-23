package com.application.grocertaxi;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.shreyaspatil.MaterialDialog.MaterialDialog;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import de.mateware.snacky.Snacky;
import maes.tech.intentanim.CustomIntent;

public class LocationPermissionActivity extends AppCompatActivity {

    private ImageView backBtn;
    private ConstraintLayout grantBtn;

    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_permission);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(LocationPermissionActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(LocationPermissionActivity.this, "fadein-to-fadeout");
            finish();
        } else if (preferenceManager.getString(Constants.KEY_USER_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_CITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).isEmpty()) {
            Intent intent = new Intent(LocationPermissionActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(LocationPermissionActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        initViews();
        setActionOnViews();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startActivity(new Intent(getApplicationContext(), PinOnMapActivity.class));
            CustomIntent.customType(LocationPermissionActivity.this, "left-to-right");
            finish();
        } else {
            return;
        }
    }

    private void initViews() {
        backBtn = findViewById(R.id.back_btn);
        grantBtn = findViewById(R.id.grant_btn);
    }

    private void setActionOnViews() {
        backBtn.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });

        grantBtn.setOnClickListener(v -> {
            if (!isConnectedToInternet(LocationPermissionActivity.this)) {
                showConnectToInternetDialog();
                return;
            } else {
                Dexter.withContext(getApplicationContext())
                        .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        .withListener(new PermissionListener() {
                            @Override
                            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                                startActivity(new Intent(getApplicationContext(), PinOnMapActivity.class));
                                CustomIntent.customType(LocationPermissionActivity.this, "left-to-right");
                                finish();
                            }

                            @Override
                            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                                if (permissionDeniedResponse.isPermanentlyDenied()) {
                                    MaterialDialog materialDialog = new MaterialDialog.Builder(LocationPermissionActivity.this)
                                            .setTitle("Permission Denied!")
                                            .setMessage("Permission to access this device's location has been permanently denied for the app. Open the app settings to manually grant the permission.")
                                            .setCancelable(false)
                                            .setPositiveButton("Open", R.drawable.ic_dialog_settings, (dialogInterface, which) -> {
                                                dialogInterface.dismiss();
                                                Intent intent = new Intent();
                                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                intent.setData(Uri.fromParts("package", getPackageName(), null));
                                                startActivity(intent);
                                            })
                                            .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                                    materialDialog.show();
                                } else {
                                    Snacky.builder()
                                            .setActivity(LocationPermissionActivity.this)
                                            .setText("Permission denied!")
                                            .setDuration(Snacky.LENGTH_SHORT)
                                            .error()
                                            .show();
                                }
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                                permissionToken.continuePermissionRequest();
                            }
                        }).check();
            }
        });
    }

    private boolean isConnectedToInternet(LocationPermissionActivity locationPermissionActivity) {
        ConnectivityManager connectivityManager = (ConnectivityManager) locationPermissionActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if ((wifiConn != null && wifiConn.isConnected()) || (mobileConn != null && mobileConn.isConnected())) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(LocationPermissionActivity.this)
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
        KeyboardVisibilityEvent.setEventListener(LocationPermissionActivity.this, isOpen -> {
            if (isOpen) {
                UIUtil.hideKeyboard(LocationPermissionActivity.this);
            }
        });
        CustomIntent.customType(LocationPermissionActivity.this, "right-to-left");
    }
}