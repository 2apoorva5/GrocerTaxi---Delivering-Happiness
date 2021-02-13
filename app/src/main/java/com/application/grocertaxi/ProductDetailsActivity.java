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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import java.util.HashMap;

import dmax.dialog.SpotsDialog;
import maes.tech.intentanim.CustomIntent;

public class ProductDetailsActivity extends AppCompatActivity {

    private ImageView closeBtn, cartBtn, productImage, productTypeImage;
    private CardView cartIndicator, productOfferContainer, addToCartBtnContainer;
    private TextView productName, productID, productUnit, productPrice, productMRP, productStatus,
            productCategory, productStoreName, productDescription, productBrand, productMFGDate, productExpiryDate;
    private ShimmerTextView productOffer;
    private ConstraintLayout addToCartBtn;
    private PullRefreshLayout pullRefreshLayout;

    private CollectionReference productsRef, cartRef;

    private String cart_location;
    private Shimmer shimmer;
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
                .setMessage("Adding item to cart...")
                .setCancelable(false)
                .setTheme(R.style.SpotsDialog)
                .build();

        cart_location = String.format("%s, %s", preferenceManager.getString(Constants.KEY_USER_LOCALITY), preferenceManager.getString(Constants.KEY_USER_CITY));

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
                .document(preferenceManager.getString(Constants.KEY_USER_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_LOCALITY))
                .collection(Constants.KEY_COLLECTION_PRODUCTS);

        cartRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .collection(Constants.KEY_COLLECTION_CART);
    }

    private void setActionOnViews() {
        closeBtn.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });

        cartBtn.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), CartActivity.class));
            CustomIntent.customType(ProductDetailsActivity.this, "bottom-to-up");
        });
    }

    private void loadProductDetails() {
        final DocumentReference productDocumentRef = productsRef.document(preferenceManager.getString(Constants.KEY_PRODUCT));
        productDocumentRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
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
            } else {
                pullRefreshLayout.setRefreshing(false);
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    Uri product_img = Uri.parse(documentSnapshot.getString(Constants.KEY_PRODUCT_IMAGE));
                    boolean product_type = documentSnapshot.getBoolean(Constants.KEY_PRODUCT_IS_VEG);
                    String product_name = documentSnapshot.getString(Constants.KEY_PRODUCT_NAME);
                    String product_id = documentSnapshot.getString(Constants.KEY_PRODUCT_ID);
                    String product_unit = documentSnapshot.getString(Constants.KEY_PRODUCT_UNIT);
                    double product_price = documentSnapshot.getDouble(Constants.KEY_PRODUCT_RETAIL_PRICE);
                    double product_mrp = documentSnapshot.getDouble(Constants.KEY_PRODUCT_MRP);
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
                    if (product_price == product_mrp) {
                        productMRP.setVisibility(View.GONE);
                        productOfferContainer.setVisibility(View.GONE);
                    } else {
                        productMRP.setVisibility(View.VISIBLE);
                        productOfferContainer.setVisibility(View.VISIBLE);
                        shimmer = new Shimmer();
                        productMRP.setText(String.format("₹ %s", product_mrp));
                        productMRP.setPaintFlags(productMRP.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        float offer = (float) (((product_mrp - product_price) / product_mrp) * 100);
                        String offer_value = ((int) offer) + "% off";
                        productOffer.setText(offer_value);
                        shimmer.start(productOffer);
                    }

                    //ProductStatus and AddToCartBtn
                    if (product_in_stock) {
                        productStatus.setText("In Stock");
                        productStatus.setTextColor(getColor(R.color.successColor));
                        addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorPrimary));
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
                                newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_IMAGE, product_img);
                                newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_NAME, product_name);
                                newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_UNIT, product_unit);
                                newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_MRP, product_mrp);
                                newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_RETAIL_PRICE, product_price);
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
                                                    return;
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
                                                    return;
                                                });
                                    } else {
                                        cartRef.whereEqualTo(Constants.KEY_CART_ITEM_PRODUCT_STORE_ID, product_store_id)
                                                .get().addOnSuccessListener(queryDocumentSnapshots2 -> {
                                            if (queryDocumentSnapshots2.getDocuments().size() == 0) {
                                                progressDialog.dismiss();
                                                MaterialDialog materialDialog = new MaterialDialog.Builder(ProductDetailsActivity.this)
                                                        .setTitle("Item cannot be added to your cart!")
                                                        .setMessage("Your cart has already been setup for a store and this item does not belong to that store. You must clear your cart by placing the order or removing all the items before proceeding with this item.")
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
                                                                                return;
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
                                                                        return;
                                                                    });
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
                                                                                return;
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
                                                                                return;
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
                                                                return;
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
                                                    return;
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
                                            return;
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
                                    return;
                                });
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
                    productStoreName.setOnClickListener(v -> {
                        preferenceManager.putString(Constants.KEY_STORE, product_store_id);
                        startActivity(new Intent(ProductDetailsActivity.this, StoreDetailsActivity.class));
                        CustomIntent.customType(ProductDetailsActivity.this, "bottom-to-up");
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

        pullRefreshLayout.setColor(getColor(R.color.colorBackground));
        pullRefreshLayout.setBackgroundColor(getColor(R.color.colorAccent));
        pullRefreshLayout.setOnRefreshListener(() -> {
            if (!isConnectedToInternet(ProductDetailsActivity.this)) {
                pullRefreshLayout.setRefreshing(false);
                showConnectToInternetDialog();
                return;
            } else {
                loadProductDetails();
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