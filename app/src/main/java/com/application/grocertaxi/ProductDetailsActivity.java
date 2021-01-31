package com.application.grocertaxi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.baoyz.widget.PullRefreshLayout;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import dmax.dialog.SpotsDialog;
import maes.tech.intentanim.CustomIntent;

public class ProductDetailsActivity extends AppCompatActivity {

    private ImageView closeBtn, cartBtn, productImage, productTypeImage;
    private CardView cartIndicator, productOfferContainer, addToCartBtnContainer;
    private TextView productName, productID, productUnit, productPrice, productMRP, productOffer, productStatus,
            productCategory, productStoreName, productDescription, productBrand, productMFGDate, productExpiryDate;
    private ConstraintLayout addToCartBtn;
    private PullRefreshLayout pullRefreshLayout;

    private CollectionReference productsRef;

    private PreferenceManager preferenceManager;
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(ProductDetailsActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(ProductDetailsActivity.this, "fadein-to-fadeout");
            finish();
        } else if (preferenceManager.getString(Constants.KEY_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_CITY) == null ||
                preferenceManager.getString(Constants.KEY_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_LOCALITY).equals("") ||
                preferenceManager.getString(Constants.KEY_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_LOCALITY).isEmpty()) {
            Intent intent = new Intent(ProductDetailsActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(ProductDetailsActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        progressDialog = new SpotsDialog.Builder().setContext(ProductDetailsActivity.this)
                .setMessage("Adding item to cart...")
                .setCancelable(false)
                .setTheme(R.style.SpotsDialog)
                .build();

        initViews();
        initFirebase();
        setActionOnViews();
    }

    private void initViews() {
        closeBtn = findViewById(R.id.close_btn);
        cartBtn = findViewById(R.id.cart_btn);
        cartIndicator = findViewById(R.id.cart_indicator);
        productImage = findViewById(R.id.product_image);
        productTypeImage = findViewById(R.id.product_type_image);
        productName = findViewById(R.id.product_name);
        productID = findViewById(R.id.product_id);
        productUnit = findViewById(R.id.product_unit);
        productPrice = findViewById(R.id.product_price);
        productMRP = findViewById(R.id.product_mrp);
        productOffer = findViewById(R.id.product_offer);
        productOfferContainer = findViewById(R.id.product_offer_container);
        productStatus = findViewById(R.id.product_status);
        productCategory = findViewById(R.id.product_category);
        productStoreName = findViewById(R.id.product_store_name);
        productDescription = findViewById(R.id.product_description);
        productBrand = findViewById(R.id.product_brand);
        productMFGDate = findViewById(R.id.product_mfg_date);
        productExpiryDate = findViewById(R.id.product_best_before);
        addToCartBtn = findViewById(R.id.add_to_cart_btn);
        addToCartBtnContainer = findViewById(R.id.add_to_cart_btn_container);
        pullRefreshLayout = findViewById(R.id.pull_refresh_layout);
    }

    private void initFirebase() {
        productsRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                .collection(Constants.KEY_COLLECTION_PRODUCTS);
    }

    private void setActionOnViews() {
        closeBtn.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });

        cartBtn.setOnClickListener(v -> {

        });
    }

    private void loadProductDetails() {
        final DocumentReference productDocumentRef = productsRef.document(preferenceManager.getString(Constants.KEY_PRODUCT));
        productDocumentRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
                Alerter.create(ProductDetailsActivity.this)
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
            } else {
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    Uri product_img = Uri.parse(documentSnapshot.getString(Constants.KEY_PRODUCT_IMAGE));
                    boolean product_type = documentSnapshot.getBoolean(Constants.KEY_PRODUCT_IS_VEG);
                    String product_name = documentSnapshot.getString(Constants.KEY_PRODUCT_NAME);
                    String product_id = documentSnapshot.getString(Constants.KEY_PRODUCT_ID);
                    String product_unit = documentSnapshot.getString(Constants.KEY_PRODUCT_UNIT);
                    String product_price = String.valueOf(documentSnapshot.getDouble(Constants.KEY_PRODUCT_RETAIL_PRICE));
                    String product_mrp = String.valueOf(documentSnapshot.getDouble(Constants.KEY_PRODUCT_MRP));
                    boolean product_in_stock = documentSnapshot.getBoolean(Constants.KEY_PRODUCT_IN_STOCK);
                    String product_category = documentSnapshot.getString(Constants.KEY_PRODUCT_CATEGORY);
                    String product_store_name = documentSnapshot.getString(Constants.KEY_PRODUCT_STORE_NAME);
                    String product_store_id = documentSnapshot.getString(Constants.KEY_PRODUCT_STORE_ID);
                    String product_desc = documentSnapshot.getString(Constants.KEY_PRODUCT_DESCRIPTION);
                    String product_brand = documentSnapshot.getString(Constants.KEY_PRODUCT_BRAND);
                    String product_mfg_date = documentSnapshot.getString(Constants.KEY_PRODUCT_MFG_DATE);
                    String product_expiry_date = documentSnapshot.getString(Constants.KEY_PRODUCT_EXPIRY_TIME);

                    //ProductImage
                    Glide.with(productImage.getContext()).load(product_img)
                            .placeholder(R.drawable.thumbnail).centerCrop().into(productImage);

                    //ProductTypeImage
                    if (product_category.equals("Baby Care") || product_category.equals("Household") ||
                            product_category.equals("Personal Care") || product_category.equals("Stationary") ||
                            product_category.equals("Hardware") || product_category.equals("Medical") ||
                            product_category.equals("Sports")) {
                        productTypeImage.setVisibility(View.GONE);
                    } else {
                        productTypeImage.setVisibility(View.VISIBLE);
                        if (product_type) {
                            productTypeImage.setImageResource(R.drawable.ic_veg);
                        } else {
                            productTypeImage.setImageResource(R.drawable.ic_nonveg);
                        }
                    }

                    //ProductName
                    productName.setText(product_name);
                    //ProductID
                    productID.setText(product_id);
                    //ProductUnit
                    productUnit.setText(product_unit);
                    //ProductPrice
                    productPrice.setText(String.format("₹ %s", product_price));

                    //ProductMRP and ProductOffer
                    if (documentSnapshot.getDouble(Constants.KEY_PRODUCT_RETAIL_PRICE) ==
                            documentSnapshot.getDouble(Constants.KEY_PRODUCT_MRP)) {
                        productMRP.setVisibility(View.GONE);
                        productOfferContainer.setVisibility(View.GONE);
                    } else {
                        productMRP.setText(String.format("₹ %s", product_mrp));
                        productMRP.setPaintFlags(productMRP.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        float offer = (float) (((documentSnapshot.getDouble(Constants.KEY_PRODUCT_MRP) - documentSnapshot.getDouble(Constants.KEY_PRODUCT_RETAIL_PRICE)) / documentSnapshot.getDouble(Constants.KEY_PRODUCT_MRP)) * 100);
                        String offer_value = ((int) offer) + "% off";
                        productOffer.setText(offer_value);
                        productOfferContainer.setVisibility(View.VISIBLE);
                    }

                    //ProductStatus and AddToCartBtn
                    if (product_in_stock) {
                        productStatus.setText("In Stock");
                        productStatus.setTextColor(getColor(R.color.successColor));
                        addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorPrimary));
                        addToCartBtn.setEnabled(true);
                        addToCartBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                            }
                        });
                    } else {
                        productStatus.setText("Out of Stock");
                        productStatus.setTextColor(getColor(R.color.errorColor));
                        addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorInactive));
                        addToCartBtn.setEnabled(false);
                    }

                    //ProductCategory
                    productCategory.setText(product_category);
                    //ProductStore
                    productStoreName.setText(product_store_name);
                    productStoreName.setEnabled(true);
                    productStoreName.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            preferenceManager.putString(Constants.KEY_STORE, product_store_id);
                        }
                    });

                    //ProductDescription
                    if (product_desc.isEmpty()) {
                        productDescription.setText("No Information");
                    } else {
                        productDescription.setText(product_desc);
                    }
                    //ProductBrand
                    if (product_brand.isEmpty()) {
                        productBrand.setText("No Information");
                    } else {
                        productBrand.setText(product_brand);
                    }
                    //ProductMFGDate
                    if (product_mfg_date.isEmpty()) {
                        productMFGDate.setText("No Information");
                    } else {
                        productMFGDate.setText(product_mfg_date);
                    }
                    //ProductExpiryDate
                    if (product_expiry_date.isEmpty()) {
                        productExpiryDate.setText("No Information");
                    } else {
                        productExpiryDate.setText(product_expiry_date);
                    }
                } else {
                    productImage.setImageResource(R.drawable.thumbnail);
                    productTypeImage.setVisibility(View.GONE);
                    productName.setText("");
                    productID.setText("");
                    productUnit.setText("");
                    productPrice.setText("");
                    productMRP.setText("");
                    productOfferContainer.setVisibility(View.GONE);
                    productStatus.setText("");
                    productCategory.setText("");
                    productStoreName.setText("");
                    productStoreName.setEnabled(false);
                    productDescription.setText("");
                    productBrand.setText("");
                    productMFGDate.setText("");
                    productExpiryDate.setText("");

                    addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorInactive));
                    addToCartBtn.setEnabled(false);
                }
            }
        });
    }

    private boolean isConnectedToInternet(ProductDetailsActivity productDetailsActivity) {
        ConnectivityManager connectivityManager = (ConnectivityManager) productDetailsActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if ((wifiConn != null && wifiConn.isConnected()) || (mobileConn != null && mobileConn.isConnected())) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(ProductDetailsActivity.this)
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
    protected void onStart() {
        super.onStart();

        loadProductDetails();

        pullRefreshLayout.setColor(getColor(R.color.colorAccent));
        pullRefreshLayout.setOnRefreshListener(() -> {
            if (!isConnectedToInternet(ProductDetailsActivity.this)) {
                pullRefreshLayout.setRefreshing(false);
                showConnectToInternetDialog();
                return;
            } else {
                loadProductDetails();
            }
        });
    }

    public static void setWindowFlag(ProductDetailsActivity productDetailsActivity, final int bits, boolean on) {
        Window window = productDetailsActivity.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();

        if (on) {
            layoutParams.flags |= bits;
        } else {
            layoutParams.flags &= ~bits;
        }
        window.setAttributes(layoutParams);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        CustomIntent.customType(ProductDetailsActivity.this, "up-to-bottom");
    }
}