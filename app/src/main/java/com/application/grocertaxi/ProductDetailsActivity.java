package com.application.grocertaxi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.application.grocertaxi.Model.Product;
import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.baoyz.widget.PullRefreshLayout;
import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import java.util.HashMap;

import dmax.dialog.SpotsDialog;
import maes.tech.intentanim.CustomIntent;

public class ProductDetailsActivity extends AppCompatActivity {

    private ImageView closeBtn, cartBtn, productImage, productTypeImage, imgOffer1,
            knowMoreBtn1, knowMoreBtn2, knowMoreBtn3, knowMoreBtn4, knowMoreBtn5;
    private CardView cartIndicator, productOfferContainer, addToCartBtnContainer;
    private TextView productName, productUnit, productPrice, productMRP, productStatus, productUnitInStock,
            productCategory, productStoreName, productDescription, productBrand, productMFGDate, productExpiryDate,
            textOffer1, textOffer2, textOffer3, textAddToCart;
    private ShimmerTextView productOffer;
    private ConstraintLayout layoutContent, layoutNoInternet, retryBtn, layoutSimilarItems, addToCartBtn;
    private RecyclerView recyclerSimilarItems;
    private ProgressBar progressBar;
    private PullRefreshLayout pullRefreshLayout;

    private CollectionReference productsRef, userRef, storeRef, cartRef;
    private FirestoreRecyclerAdapter<Product, SimilarItemsViewHolder> similarItemAdapter;

    private String cart_location;
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
        } else if (preferenceManager.getString(Constants.KEY_USER_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_CITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).isEmpty()) {
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
                .setMessage("Adding item to cart..")
                .setCancelable(false)
                .setTheme(R.style.SpotsDialog)
                .build();

        ////////////////////////////////////////////////////////////////////////////////////////////

        cart_location = String.format("%s, %s", preferenceManager.getString(Constants.KEY_USER_LOCALITY), preferenceManager.getString(Constants.KEY_USER_CITY));

        ////////////////////////////////////////////////////////////////////////////////////////////

        layoutContent = findViewById(R.id.layout_content);
        layoutNoInternet = findViewById(R.id.layout_no_internet);
        retryBtn = findViewById(R.id.retry_btn);

        closeBtn = findViewById(R.id.close_btn);
        cartBtn = findViewById(R.id.cart_btn);
        cartIndicator = findViewById(R.id.cart_indicator);

        productImage = findViewById(R.id.product_image);
        productTypeImage = findViewById(R.id.product_type);
        productName = findViewById(R.id.product_name);
        productUnit = findViewById(R.id.product_unit);
        productPrice = findViewById(R.id.product_price);
        productMRP = findViewById(R.id.product_mrp);
        productOffer = findViewById(R.id.product_offer);
        productOfferContainer = findViewById(R.id.product_offer_container);
        productStatus = findViewById(R.id.product_status);
        productUnitInStock = findViewById(R.id.product_unit_in_stock);
        productCategory = findViewById(R.id.product_category);
        productStoreName = findViewById(R.id.product_store_name);
        productDescription = findViewById(R.id.product_description);
        productBrand = findViewById(R.id.product_brand);
        productMFGDate = findViewById(R.id.product_mfg_date);
        productExpiryDate = findViewById(R.id.product_best_before);
        progressBar = findViewById(R.id.progress_bar);

        imgOffer1 = findViewById(R.id.img_offer1);
        textOffer1 = findViewById(R.id.text_offer1);
        textOffer2 = findViewById(R.id.text_offer2);
        textOffer3 = findViewById(R.id.text_offer3);
        knowMoreBtn1 = findViewById(R.id.know_more_btn1);
        knowMoreBtn2 = findViewById(R.id.know_more_btn2);
        knowMoreBtn3 = findViewById(R.id.know_more_btn3);
        knowMoreBtn4 = findViewById(R.id.know_more_btn4);
        knowMoreBtn5 = findViewById(R.id.know_more_btn5);

        layoutSimilarItems = findViewById(R.id.layout_similar_items);
        recyclerSimilarItems = findViewById(R.id.recycler_similar_items);

        addToCartBtn = findViewById(R.id.add_to_cart_btn);
        addToCartBtnContainer = findViewById(R.id.add_to_cart_btn_container);
        textAddToCart = findViewById(R.id.text_add_to_cart);
        pullRefreshLayout = findViewById(R.id.pull_refresh_layout);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkNetworkConnection();
    }

    private void checkNetworkConnection() {
        if (!isConnectedToInternet(ProductDetailsActivity.this)) {
            layoutContent.setVisibility(View.GONE);
            layoutNoInternet.setVisibility(View.VISIBLE);
            retryBtn.setOnClickListener(v -> checkNetworkConnection());
        } else {
            initFirebase();
            setActionOnViews();
        }
    }

    private void initFirebase() {
        productsRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_LOCALITY))
                .collection(Constants.KEY_COLLECTION_PRODUCTS);

        userRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS);

        storeRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_LOCALITY))
                .collection(Constants.KEY_COLLECTION_STORES);

        cartRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .collection(Constants.KEY_COLLECTION_CART);
    }

    private void setActionOnViews() {
        layoutNoInternet.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);

        pullRefreshLayout.setRefreshing(false);

        ////////////////////////////////////////////////////////////////////////////////////////////

        pullRefreshLayout.setColor(getColor(R.color.colorIconLight));
        pullRefreshLayout.setBackgroundColor(getColor(R.color.colorAccent));
        pullRefreshLayout.setOnRefreshListener(this::checkNetworkConnection);

        ////////////////////////////////////////////////////////////////////////////////////////////

        closeBtn.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        progressBar.setVisibility(View.VISIBLE);
        loadProductDetails();

        ////////////////////////////////////////////////////////////////////////////////////////////

        cartRef.whereEqualTo(Constants.KEY_CART_ITEM_LOCATION, cart_location).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.size() == 0) {
                cartIndicator.setVisibility(View.GONE);
            } else {
                cartIndicator.setVisibility(View.VISIBLE);
            }
        }).addOnFailureListener(e -> {
            Alerter.create(ProductDetailsActivity.this)
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
            return;
        });

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
            CustomIntent.customType(ProductDetailsActivity.this, "bottom-to-up");
        });
    }

    //////////////////////////////////// Load Product Details //////////////////////////////////////

    private void loadProductDetails() {
        final DocumentReference productDocumentRef = productsRef.document(preferenceManager.getString(Constants.KEY_PRODUCT));
        productDocumentRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
                progressBar.setVisibility(View.GONE);
                pullRefreshLayout.setRefreshing(false);
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
            } else {
                progressBar.setVisibility(View.GONE);
                pullRefreshLayout.setRefreshing(false);
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    Uri product_img = Uri.parse(documentSnapshot.getString(Constants.KEY_PRODUCT_IMAGE));
                    boolean product_type = documentSnapshot.getBoolean(Constants.KEY_PRODUCT_IS_VEG);
                    String product_name = documentSnapshot.getString(Constants.KEY_PRODUCT_NAME);
                    String product_id = documentSnapshot.getString(Constants.KEY_PRODUCT_ID);
                    String product_unit = documentSnapshot.getString(Constants.KEY_PRODUCT_UNIT);
                    double product_price = documentSnapshot.getDouble(Constants.KEY_PRODUCT_RETAIL_PRICE);
                    double product_mrp = documentSnapshot.getDouble(Constants.KEY_PRODUCT_MRP);
                    long product_offer = documentSnapshot.getLong(Constants.KEY_PRODUCT_OFFER);
                    boolean product_in_stock = documentSnapshot.getBoolean(Constants.KEY_PRODUCT_IN_STOCK);
                    long product_unit_in_stock = documentSnapshot.getLong(Constants.KEY_PRODUCT_UNITS_IN_STOCK);
                    String product_category = documentSnapshot.getString(Constants.KEY_PRODUCT_CATEGORY);
                    String product_store_name = documentSnapshot.getString(Constants.KEY_PRODUCT_STORE_NAME);
                    String product_store_id = documentSnapshot.getString(Constants.KEY_PRODUCT_STORE_ID);
                    String product_desc = documentSnapshot.getString(Constants.KEY_PRODUCT_DESCRIPTION);
                    String product_brand = documentSnapshot.getString(Constants.KEY_PRODUCT_BRAND);
                    String product_mfg_date = documentSnapshot.getString(Constants.KEY_PRODUCT_MFG_DATE);
                    String product_expiry_date = documentSnapshot.getString(Constants.KEY_PRODUCT_EXPIRY_TIME);

                    ////////////////////////////////////////////////////////////////////////////////

                    Glide.with(productImage.getContext()).load(product_img)
                            .placeholder(R.drawable.thumbnail).centerCrop().into(productImage);

                    ////////////////////////////////////////////////////////////////////////////////

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

                    ////////////////////////////////////////////////////////////////////////////////

                    productName.setText(product_name);
                    productUnit.setText(product_unit);

                    ////////////////////////////////////////////////////////////////////////////////

                    productPrice.setText(String.format("₹ %s", product_price));

                    if (product_offer == 0) {
                        productMRP.setVisibility(View.GONE);
                        productOfferContainer.setVisibility(View.GONE);
                    } else {
                        productMRP.setVisibility(View.VISIBLE);
                        productMRP.setText(String.format("₹ %s", product_mrp));
                        productMRP.setPaintFlags(productMRP.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        productOfferContainer.setVisibility(View.VISIBLE);
                        Shimmer shimmer = new Shimmer();
                        productOffer.setText(String.format("%d%% OFF", product_offer));
                        shimmer.start(productOffer);
                    }

                    ////////////////////////////////////////////////////////////////////////////////

                    productCategory.setText(product_category);
                    productStoreName.setText(product_store_name);
                    productStoreName.setEnabled(true);
                    productStoreName.setOnClickListener(v -> {
                        preferenceManager.putString(Constants.KEY_STORE, product_store_id);
                        startActivity(new Intent(ProductDetailsActivity.this, StoreDetailsActivity.class));
                        CustomIntent.customType(ProductDetailsActivity.this, "bottom-to-up");
                    });

                    ////////////////////////////////////////////////////////////////////////////////

                    if (product_desc.isEmpty()) {
                        productDescription.setText("No Information");
                    } else {
                        productDescription.setText(product_desc);
                    }

                    if (product_brand.isEmpty()) {
                        productBrand.setText("No Information");
                    } else {
                        productBrand.setText(product_brand);
                    }

                    if (product_mfg_date.isEmpty()) {
                        productMFGDate.setText("No Information");
                    } else {
                        productMFGDate.setText(product_mfg_date);
                    }

                    if (product_expiry_date.isEmpty()) {
                        productExpiryDate.setText("No Information");
                    } else {
                        productExpiryDate.setText(product_expiry_date);
                    }

                    ////////////////////////////////////////////////////////////////////////////////

                    userRef.document(preferenceManager.getString(Constants.KEY_USER_ID))
                            .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            pullRefreshLayout.setRefreshing(false);
                            DocumentSnapshot documentSnapshot1 = task.getResult();
                            if (documentSnapshot1.exists()) {
                                boolean first_order = documentSnapshot1.getBoolean(Constants.KEY_USER_FIRST_ORDER);

                                if (first_order) {
                                    imgOffer1.setVisibility(View.VISIBLE);
                                    textOffer1.setVisibility(View.VISIBLE);
                                    knowMoreBtn1.setVisibility(View.VISIBLE);
                                    textOffer2.setText("Yay! FREE Delivery for you.");
                                    textOffer3.setText("Yay! No convenience fee for you.");

                                    knowMoreBtn1.setOnClickListener(v -> {
                                        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(ProductDetailsActivity.this);
                                        bottomSheetDialog.setContentView(R.layout.bottom_sheet_info1);
                                        bottomSheetDialog.setCanceledOnTouchOutside(false);

                                        ImageView closeSheetBtn = bottomSheetDialog.findViewById(R.id.close_bottom_sheet_btn);
                                        closeSheetBtn.setOnClickListener(v12 -> bottomSheetDialog.dismiss());

                                        bottomSheetDialog.show();
                                    });

                                    knowMoreBtn2.setOnClickListener(v -> {
                                        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(ProductDetailsActivity.this);
                                        bottomSheetDialog.setContentView(R.layout.bottom_sheet_info2);
                                        bottomSheetDialog.setCanceledOnTouchOutside(false);

                                        ImageView closeSheetBtn = bottomSheetDialog.findViewById(R.id.close_bottom_sheet_btn);
                                        closeSheetBtn.setOnClickListener(v12 -> bottomSheetDialog.dismiss());

                                        bottomSheetDialog.show();
                                    });

                                    knowMoreBtn3.setOnClickListener(v -> {
                                        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(ProductDetailsActivity.this);
                                        bottomSheetDialog.setContentView(R.layout.bottom_sheet_info3);
                                        bottomSheetDialog.setCanceledOnTouchOutside(false);

                                        ImageView closeSheetBtn = bottomSheetDialog.findViewById(R.id.close_bottom_sheet_btn);
                                        closeSheetBtn.setOnClickListener(v12 -> bottomSheetDialog.dismiss());

                                        bottomSheetDialog.show();
                                    });
                                } else {
                                    imgOffer1.setVisibility(View.GONE);
                                    textOffer1.setVisibility(View.GONE);
                                    knowMoreBtn1.setVisibility(View.GONE);

                                    textOffer2.setText("Delivery charges may apply during checkout.");
                                    textOffer3.setText("A convenience fee will be added during checkout (only applicable for online payments though).");

                                    knowMoreBtn2.setOnClickListener(v -> {
                                        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(ProductDetailsActivity.this);
                                        bottomSheetDialog.setContentView(R.layout.bottom_sheet_info2);
                                        bottomSheetDialog.setCanceledOnTouchOutside(false);

                                        ImageView closeSheetBtn = bottomSheetDialog.findViewById(R.id.close_bottom_sheet_btn);
                                        closeSheetBtn.setOnClickListener(v12 -> bottomSheetDialog.dismiss());

                                        bottomSheetDialog.show();
                                    });

                                    knowMoreBtn3.setOnClickListener(v -> {
                                        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(ProductDetailsActivity.this);
                                        bottomSheetDialog.setContentView(R.layout.bottom_sheet_info3);
                                        bottomSheetDialog.setCanceledOnTouchOutside(false);

                                        ImageView closeSheetBtn = bottomSheetDialog.findViewById(R.id.close_bottom_sheet_btn);
                                        closeSheetBtn.setOnClickListener(v12 -> bottomSheetDialog.dismiss());

                                        bottomSheetDialog.show();
                                    });
                                }
                            } else {
                                pullRefreshLayout.setRefreshing(false);
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
                            }
                        } else {
                            pullRefreshLayout.setRefreshing(false);
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
                        }
                    });

                    ////////////////////////////////////////////////////////////////////////////////

                    knowMoreBtn4.setOnClickListener(v -> {
                        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(ProductDetailsActivity.this);
                        bottomSheetDialog.setContentView(R.layout.bottom_sheet_info4);
                        bottomSheetDialog.setCanceledOnTouchOutside(false);

                        ImageView closeSheetBtn = bottomSheetDialog.findViewById(R.id.close_bottom_sheet_btn);
                        closeSheetBtn.setOnClickListener(v12 -> bottomSheetDialog.dismiss());

                        bottomSheetDialog.show();
                    });

                    ////////////////////////////////////////////////////////////////////////////////

                    knowMoreBtn5.setOnClickListener(v -> {
                        String privacyPolicyUrl = "https://grocertaxi.wixsite.com/refund-policy";
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl));
                        startActivity(browserIntent);
                    });

                    ////////////////////////////////////////////////////////////////////////////////

                    if (product_in_stock) {
                        productImage.clearColorFilter();
                        productOfferContainer.setVisibility(View.VISIBLE);

                        productStatus.setText("In Stock");
                        productStatus.setTextColor(getColor(R.color.successColor));

                        productUnitInStock.setVisibility(View.VISIBLE);
                        if (product_unit_in_stock >= 1 && product_unit_in_stock <= 5) {
                            productUnitInStock.setText(String.format("(Hurry! Only %d left)", product_unit_in_stock));
                        } else if (product_unit_in_stock > 5) {
                            productUnitInStock.setText(String.format("(%d units left)", product_unit_in_stock));
                        }

                        addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorPrimary));
                        textAddToCart.setText("Add to Cart");
                        textAddToCart.setTextColor(getColor(R.color.colorTextDark));
                        addToCartBtn.setEnabled(true);
                        addToCartBtn.setOnClickListener(v -> {
                            if (!isConnectedToInternet(ProductDetailsActivity.this)) {
                                showConnectToInternetDialog();
                                return;
                            } else {
                                progressDialog.show();

                                String[] split = product_id.split("-", 2);
                                String cart_id = "ITEM-" + split[1];

                                HashMap<String, Object> newCartItem = new HashMap<>();
                                newCartItem.put(Constants.KEY_CART_ITEM_ID, cart_id);
                                newCartItem.put(Constants.KEY_CART_ITEM_TIMESTAMP, FieldValue.serverTimestamp());
                                newCartItem.put(Constants.KEY_CART_ITEM_LOCATION, cart_location);
                                newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_ID, product_id);
                                newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_STORE_ID, product_store_id);
                                newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_STORE_NAME, product_store_name);
                                newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_CATEGORY, product_category);
                                newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_IMAGE, String.valueOf(product_img));
                                newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_NAME, product_name);
                                newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_UNIT, product_unit);
                                newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_MRP, product_mrp);
                                newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_RETAIL_PRICE, product_price);
                                newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_OFFER, product_offer);
                                newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY, 1);

                                cartRef.get().addOnSuccessListener(queryDocumentSnapshots1 -> {
                                    if (queryDocumentSnapshots1.getDocuments().size() == 0) {
                                        cartRef.document(cart_id).set(newCartItem)
                                                .addOnSuccessListener(aVoid -> {
                                                    progressDialog.dismiss();
                                                    cartIndicator.setVisibility(View.VISIBLE);
                                                    Alerter.create(ProductDetailsActivity.this)
                                                            .setText("Success! Your cart just got updated.")
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
                                                    addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.successColor));
                                                    textAddToCart.setText("Go to Cart");
                                                    textAddToCart.setTextColor(getColor(R.color.colorTextLight));
                                                    addToCartBtn.setEnabled(true);
                                                    addToCartBtn.setOnClickListener(v1 -> {
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
                                                        CustomIntent.customType(ProductDetailsActivity.this, "bottom-to-up");
                                                    });
                                                })
                                                .addOnFailureListener(e -> {
                                                    progressDialog.dismiss();
                                                    Alerter.create(ProductDetailsActivity.this)
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
                                    } else {
                                        cartRef.whereEqualTo(Constants.KEY_CART_ITEM_PRODUCT_STORE_ID, product_store_id)
                                                .get().addOnSuccessListener(queryDocumentSnapshots2 -> {
                                            if (queryDocumentSnapshots2.getDocuments().size() == 0) {
                                                progressDialog.dismiss();
                                                MaterialDialog materialDialog = new MaterialDialog.Builder(ProductDetailsActivity.this)
                                                        .setTitle("Item cannot be added to your cart!")
                                                        .setMessage("Your cart has already been setup for another store this item does not belong to. You must clear your cart first before proceeding with this item.")
                                                        .setCancelable(false)
                                                        .setPositiveButton("Go to Cart", R.drawable.ic_dialog_cart, (dialogInterface, which) -> {
                                                            dialogInterface.dismiss();
                                                            startActivity(new Intent(getApplicationContext(), CartActivity.class));
                                                            CustomIntent.customType(ProductDetailsActivity.this, "bottom-to-up");
                                                        })
                                                        .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                                                materialDialog.show();
                                            } else {
                                                cartRef.document(cart_id).get()
                                                        .addOnCompleteListener(task -> {
                                                            if (task.isSuccessful()) {
                                                                if (task.getResult().exists()) {
                                                                    long count = task.getResult().getLong(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY);
                                                                    if (count <= product_unit_in_stock) {
                                                                        cartRef.document(cart_id)
                                                                                .update(Constants.KEY_CART_ITEM_TIMESTAMP, FieldValue.serverTimestamp(),
                                                                                        Constants.KEY_CART_ITEM_PRODUCT_QUANTITY, task.getResult().getLong(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY) + 1)
                                                                                .addOnSuccessListener(aVoid -> {
                                                                                    progressDialog.dismiss();
                                                                                    cartIndicator.setVisibility(View.VISIBLE);
                                                                                    Alerter.create(ProductDetailsActivity.this)
                                                                                            .setText("Success! Your cart just got updated.")
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
                                                                                    addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.successColor));
                                                                                    textAddToCart.setText("Go to Cart");
                                                                                    textAddToCart.setTextColor(getColor(R.color.colorTextLight));
                                                                                    addToCartBtn.setEnabled(true);
                                                                                    addToCartBtn.setOnClickListener(v1 -> {
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
                                                                                        CustomIntent.customType(ProductDetailsActivity.this, "bottom-to-up");
                                                                                    });
                                                                                }).addOnFailureListener(e -> {
                                                                            progressDialog.dismiss();
                                                                            Alerter.create(ProductDetailsActivity.this)
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
                                                                    } else {
                                                                        progressDialog.dismiss();
                                                                        MaterialDialog materialDialog = new MaterialDialog.Builder(ProductDetailsActivity.this)
                                                                                .setTitle("Item can't be further added to your cart!")
                                                                                .setMessage("The store doesn't have more of this product than the quantity you already have in your cart.")
                                                                                .setCancelable(false)
                                                                                .setPositiveButton("Okay", R.drawable.ic_dialog_okay, (dialogInterface, which) -> dialogInterface.dismiss())
                                                                                .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                                                                        materialDialog.show();
                                                                    }
                                                                } else {
                                                                    cartRef.document(cart_id).set(newCartItem)
                                                                            .addOnSuccessListener(aVoid -> {
                                                                                progressDialog.dismiss();
                                                                                cartIndicator.setVisibility(View.VISIBLE);
                                                                                Alerter.create(ProductDetailsActivity.this)
                                                                                        .setText("Success! Your cart just got updated.")
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
                                                                                addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.successColor));
                                                                                textAddToCart.setText("Go to Cart");
                                                                                textAddToCart.setTextColor(getColor(R.color.colorTextLight));
                                                                                addToCartBtn.setEnabled(true);
                                                                                addToCartBtn.setOnClickListener(v1 -> {
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
                                                                                    CustomIntent.customType(ProductDetailsActivity.this, "bottom-to-up");
                                                                                });
                                                                            })
                                                                            .addOnFailureListener(e -> {
                                                                                progressDialog.dismiss();
                                                                                Alerter.create(ProductDetailsActivity.this)
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
                                                            } else {
                                                                progressDialog.dismiss();
                                                                Alerter.create(ProductDetailsActivity.this)
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
                                                            }
                                                        }).addOnFailureListener(e -> {
                                                    progressDialog.dismiss();
                                                    Alerter.create(ProductDetailsActivity.this)
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
                                        }).addOnFailureListener(e -> {
                                            progressDialog.dismiss();
                                            Alerter.create(ProductDetailsActivity.this)
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
                                }).addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Alerter.create(ProductDetailsActivity.this)
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
                        });
                    } else {
                        ColorMatrix matrix = new ColorMatrix();
                        matrix.setSaturation(0);

                        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
                        productImage.setColorFilter(filter);

                        productOfferContainer.setVisibility(View.GONE);

                        productStatus.setText("Out of Stock");
                        productStatus.setTextColor(getColor(R.color.errorColor));
                        productUnitInStock.setVisibility(View.GONE);

                        addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorInactive));
                        textAddToCart.setText("Add to Cart");
                        textAddToCart.setTextColor(getColor(R.color.colorTextLight));
                        addToCartBtn.setEnabled(false);
                    }


                    /////////////////////////////// LoadSimilarItems ///////////////////////////////

                    Query query = storeRef.document(product_store_id).collection(Constants.KEY_COLLECTION_CATEGORIES)
                            .document(product_category).collection(Constants.KEY_COLLECTION_PRODUCTS)
                            .whereNotEqualTo(Constants.KEY_PRODUCT_ID, product_id).limit(5);

                    FirestoreRecyclerOptions<Product> options = new FirestoreRecyclerOptions.Builder<Product>()
                            .setLifecycleOwner(ProductDetailsActivity.this)
                            .setQuery(query, Product.class)
                            .build();

                    similarItemAdapter = new FirestoreRecyclerAdapter<Product, SimilarItemsViewHolder>(options) {

                        @NonNull
                        @Override
                        public SimilarItemsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_horizontal_product, parent, false);
                            return new SimilarItemsViewHolder(view);
                        }

                        @Override
                        protected void onBindViewHolder(@NonNull SimilarItemsViewHolder holder, int position, @NonNull Product model) {
                            Glide.with(holder.productImage.getContext()).load(model.getProductImage())
                                    .placeholder(R.drawable.thumbnail).centerCrop().into(holder.productImage);

                            ////////////////////////////////////////////////////////////////////////

                            if (model.getProductCategory().equals("Baby Care") || model.getProductCategory().equals("Household") ||
                                    model.getProductCategory().equals("Personal Care") || model.getProductCategory().equals("Stationary") ||
                                    model.getProductCategory().equals("Hardware") || model.getProductCategory().equals("Medical") || model.getProductCategory().equals("Sports")) {
                                holder.productTypeImage.setVisibility(View.GONE);
                            } else {
                                holder.productTypeImage.setVisibility(View.VISIBLE);
                                if (model.isProductIsVeg()) {
                                    holder.productTypeImage.setImageResource(R.drawable.ic_veg);
                                } else {
                                    holder.productTypeImage.setImageResource(R.drawable.ic_nonveg);
                                }
                            }

                            ////////////////////////////////////////////////////////////////////////

                            holder.productName.setText(model.getProductName());
                            holder.productUnit.setText(model.getProductUnit());
                            holder.productPrice.setText(String.format("₹ %s", model.getProductRetailPrice()));

                            ////////////////////////////////////////////////////////////////////////

                            if (model.getProductOffer() == 0) {
                                holder.productOffer.setVisibility(View.GONE);
                            } else {
                                Shimmer shimmer = new Shimmer();
                                holder.productOffer.setVisibility(View.VISIBLE);
                                holder.productOffer.setText(String.format("%d%% OFF", model.getProductOffer()));
                                shimmer.start(holder.productOffer);
                            }

                            ////////////////////////////////////////////////////////////////////////

                            holder.clickListenerFruit.setOnClickListener(v -> {
                                preferenceManager.putString(Constants.KEY_PRODUCT, model.getProductID());
                                startActivity(new Intent(ProductDetailsActivity.this, ProductDetailsActivity.class));
                                CustomIntent.customType(ProductDetailsActivity.this, "bottom-to-up");
                            });

                            ////////////////////////////////////////////////////////////////////////

                            if (model.isProductInStock()) {
                                holder.productImage.clearColorFilter();
                                holder.productOffer.setVisibility(View.VISIBLE);

                                holder.addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorAccent));
                                holder.addToCartBtn.setEnabled(true);
                                holder.addToCartBtn.setOnClickListener(v -> {
                                    if (!isConnectedToInternet(ProductDetailsActivity.this)) {
                                        showConnectToInternetDialog();
                                        return;
                                    } else {
                                        progressDialog.show();

                                        String[] split = model.getProductID().split("-", 2);
                                        String cart_id = "ITEM-" + split[1];

                                        HashMap<String, Object> newCartItem = new HashMap<>();
                                        newCartItem.put(Constants.KEY_CART_ITEM_ID, cart_id);
                                        newCartItem.put(Constants.KEY_CART_ITEM_TIMESTAMP, FieldValue.serverTimestamp());
                                        newCartItem.put(Constants.KEY_CART_ITEM_LOCATION, cart_location);
                                        newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_ID, model.getProductID());
                                        newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_STORE_ID, model.getProductStoreID());
                                        newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_STORE_NAME, model.getProductStoreName());
                                        newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_CATEGORY, model.getProductCategory());
                                        newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_IMAGE, model.getProductImage());
                                        newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_NAME, model.getProductName());
                                        newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_UNIT, model.getProductUnit());
                                        newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_MRP, model.getProductMRP());
                                        newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_RETAIL_PRICE, model.getProductRetailPrice());
                                        newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_OFFER, model.getProductOffer());
                                        newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY, 1);

                                        cartRef.get().addOnSuccessListener(queryDocumentSnapshots1 -> {
                                            if (queryDocumentSnapshots1.getDocuments().size() == 0) {
                                                cartRef.document(cart_id).set(newCartItem)
                                                        .addOnSuccessListener(aVoid -> {
                                                            progressDialog.dismiss();
                                                            cartIndicator.setVisibility(View.VISIBLE);
                                                            Alerter.create(ProductDetailsActivity.this)
                                                                    .setText("Success! Your cart just got updated.")
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
                                                            progressDialog.dismiss();
                                                            Alerter.create(ProductDetailsActivity.this)
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
                                            } else {
                                                cartRef.whereEqualTo(Constants.KEY_CART_ITEM_PRODUCT_STORE_ID, model.getProductStoreID())
                                                        .get().addOnSuccessListener(queryDocumentSnapshots2 -> {
                                                    if (queryDocumentSnapshots2.getDocuments().size() == 0) {
                                                        progressDialog.dismiss();
                                                        MaterialDialog materialDialog = new MaterialDialog.Builder(ProductDetailsActivity.this)
                                                                .setTitle("Item cannot be added to your cart!")
                                                                .setMessage("Your cart has already been setup for another store this item does not belong to. You must clear your cart first before proceeding with this item.")
                                                                .setCancelable(false)
                                                                .setPositiveButton("Go to Cart", R.drawable.ic_dialog_cart, (dialogInterface, which) -> {
                                                                    dialogInterface.dismiss();
                                                                    startActivity(new Intent(getApplicationContext(), CartActivity.class));
                                                                    CustomIntent.customType(ProductDetailsActivity.this, "bottom-to-up");
                                                                })
                                                                .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                                                        materialDialog.show();
                                                    } else {
                                                        cartRef.document(cart_id).get()
                                                                .addOnCompleteListener(task -> {
                                                                    if (task.isSuccessful()) {
                                                                        if (task.getResult().exists()) {
                                                                            long count = task.getResult().getLong(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY);
                                                                            if (count <= model.getProductUnitsInStock()) {
                                                                                cartRef.document(cart_id)
                                                                                        .update(Constants.KEY_CART_ITEM_TIMESTAMP, FieldValue.serverTimestamp(),
                                                                                                Constants.KEY_CART_ITEM_PRODUCT_QUANTITY, task.getResult().getLong(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY) + 1)
                                                                                        .addOnSuccessListener(aVoid -> {
                                                                                            progressDialog.dismiss();
                                                                                            Alerter.create(ProductDetailsActivity.this)
                                                                                                    .setText("Success! Your cart just got updated.")
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
                                                                                        }).addOnFailureListener(e -> {
                                                                                    progressDialog.dismiss();
                                                                                    Alerter.create(ProductDetailsActivity.this)
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
                                                                            } else {
                                                                                progressDialog.dismiss();
                                                                                MaterialDialog materialDialog = new MaterialDialog.Builder(ProductDetailsActivity.this)
                                                                                        .setTitle("Item can't be further added to your cart!")
                                                                                        .setMessage("The store doesn't have more of this product than the quantity you already have in your cart.")
                                                                                        .setCancelable(false)
                                                                                        .setPositiveButton("Okay", R.drawable.ic_dialog_okay, (dialogInterface, which) -> dialogInterface.dismiss())
                                                                                        .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                                                                                materialDialog.show();
                                                                            }
                                                                        } else {
                                                                            cartRef.document(cart_id).set(newCartItem)
                                                                                    .addOnSuccessListener(aVoid -> {
                                                                                        progressDialog.dismiss();
                                                                                        cartIndicator.setVisibility(View.VISIBLE);
                                                                                        Alerter.create(ProductDetailsActivity.this)
                                                                                                .setText("Success! Your cart just got updated.")
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
                                                                                        progressDialog.dismiss();
                                                                                        Alerter.create(ProductDetailsActivity.this)
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
                                                                    } else {
                                                                        progressDialog.dismiss();
                                                                        Alerter.create(ProductDetailsActivity.this)
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
                                                                    }
                                                                }).addOnFailureListener(e -> {
                                                            progressDialog.dismiss();
                                                            Alerter.create(ProductDetailsActivity.this)
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
                                                }).addOnFailureListener(e -> {
                                                    progressDialog.dismiss();
                                                    Alerter.create(ProductDetailsActivity.this)
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
                                        }).addOnFailureListener(e -> {
                                            progressDialog.dismiss();
                                            Alerter.create(ProductDetailsActivity.this)
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
                                });
                            } else {
                                ColorMatrix matrix = new ColorMatrix();
                                matrix.setSaturation(0);

                                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
                                holder.productImage.setColorFilter(filter);

                                holder.productOffer.setVisibility(View.GONE);

                                holder.addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorInactive));
                                holder.addToCartBtn.setEnabled(false);
                            }
                        }

                        @Override
                        public void onDataChanged() {
                            super.onDataChanged();

                            pullRefreshLayout.setRefreshing(false);

                            if (getItemCount() == 0) {
                                layoutSimilarItems.setVisibility(View.GONE);
                            } else {
                                layoutSimilarItems.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onError(@NonNull FirebaseFirestoreException e) {
                            super.onError(e);

                            pullRefreshLayout.setRefreshing(false);
                            Alerter.create(ProductDetailsActivity.this)
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
                        }
                    };

                    similarItemAdapter.notifyDataSetChanged();

                    recyclerSimilarItems.setHasFixedSize(true);
                    recyclerSimilarItems.setLayoutManager(new LinearLayoutManager(ProductDetailsActivity.this, LinearLayoutManager.HORIZONTAL, false));
                    recyclerSimilarItems.setAdapter(similarItemAdapter);
                } else {
                    progressBar.setVisibility(View.GONE);
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

                    productImage.setImageResource(R.drawable.thumbnail);
                    productTypeImage.setVisibility(View.GONE);
                    productName.setText("");
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

    ////////////////////////////////// SimilarItemsViewHolder //////////////////////////////////////

    public static class SimilarItemsViewHolder extends RecyclerView.ViewHolder {

        CardView addToCartBtnContainer;
        ConstraintLayout clickListenerFruit, addToCartBtn;
        ImageView productImage, productTypeImage;
        TextView productName, productPrice, productUnit;
        ShimmerTextView productOffer;

        public SimilarItemsViewHolder(@NonNull View itemView) {
            super(itemView);

            clickListenerFruit = itemView.findViewById(R.id.click_listener);
            productImage = itemView.findViewById(R.id.product_image);
            productTypeImage = itemView.findViewById(R.id.product_type);
            productOffer = itemView.findViewById(R.id.product_offer);
            productName = itemView.findViewById(R.id.product_name);
            productUnit = itemView.findViewById(R.id.product_unit);
            productPrice = itemView.findViewById(R.id.product_price);
            addToCartBtnContainer = itemView.findViewById(R.id.add_to_cart_btn_container);
            addToCartBtn = itemView.findViewById(R.id.add_to_cart_btn);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(ProductDetailsActivity productDetailsActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) productDetailsActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
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