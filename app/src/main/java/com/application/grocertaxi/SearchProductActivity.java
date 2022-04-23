package com.application.grocertaxi;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.paging.LoadState;
import androidx.paging.PagingConfig;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.application.grocertaxi.Helper.LoadingDialog;
import com.application.grocertaxi.Model.Product;
import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.paging.FirestorePagingAdapter;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import dev.shreyaspatil.MaterialDialog.MaterialDialog;
import maes.tech.intentanim.CustomIntent;

public class SearchProductActivity extends AppCompatActivity {

    private ImageView actionBtn, speechToText;
    private AutoCompleteTextView inputProductSearch;
    private RecyclerView recyclerProducts;
    private ConstraintLayout layoutSearch, layoutEmpty, layoutNoInternet, retryBtn;
    private ProgressBar progressBar;

    private CollectionReference productsRef, cartRef;
    private FirestorePagingAdapter<Product, ProductViewHolder> productAdapter;
    List<String> productsList;

    private String cart_location;
    private PreferenceManager preferenceManager;
    private static int LAST_POSITION = -1;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_product);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(SearchProductActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(SearchProductActivity.this, "fadein-to-fadeout");
            finish();
        } else if (preferenceManager.getString(Constants.KEY_USER_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_CITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).isEmpty()) {
            Intent intent = new Intent(SearchProductActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(SearchProductActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorSearch));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        loadingDialog = new LoadingDialog(SearchProductActivity.this);

        ////////////////////////////////////////////////////////////////////////////////////////////

        cart_location = String.format("%s, %s", preferenceManager.getString(Constants.KEY_USER_LOCALITY), preferenceManager.getString(Constants.KEY_USER_CITY));

        ////////////////////////////////////////////////////////////////////////////////////////////

        actionBtn = findViewById(R.id.action_btn);
        speechToText = findViewById(R.id.speech_to_text);
        inputProductSearch = findViewById(R.id.input_product_search_field);
        recyclerProducts = findViewById(R.id.recycler_products);
        layoutSearch = findViewById(R.id.layout_search);
        layoutEmpty = findViewById(R.id.layout_empty);
        progressBar = findViewById(R.id.progress_bar);
        layoutNoInternet = findViewById(R.id.layout_no_internet);
        retryBtn = findViewById(R.id.retry_btn);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkNetworkConnection();
    }

    private void checkNetworkConnection() {
        if (!isConnectedToInternet(SearchProductActivity.this)) {
            inputProductSearch.setEnabled(false);
            speechToText.setEnabled(false);
            layoutSearch.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
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

        cartRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .collection(Constants.KEY_COLLECTION_CART);
    }

    private void setActionOnViews() {
        layoutNoInternet.setVisibility(View.GONE);
        inputProductSearch.setEnabled(true);
        speechToText.setEnabled(true);
        layoutEmpty.setVisibility(View.GONE);
        layoutSearch.setVisibility(View.VISIBLE);

        inputProductSearch.requestFocus();

        KeyboardVisibilityEvent.setEventListener(SearchProductActivity.this, isOpen -> {
            if (!isOpen) {
                actionBtn.setImageResource(R.drawable.ic_search);
                actionBtn.setAlpha(0.5f);
                actionBtn.setEnabled(false);
                inputProductSearch.clearFocus();
            } else {
                actionBtn.setImageResource(R.drawable.ic_close);
                actionBtn.setAlpha(1f);
                actionBtn.setEnabled(true);
                actionBtn.setOnClickListener(v -> {
                    UIUtil.hideKeyboard(SearchProductActivity.this);
                    onBackPressed();
                    finish();
                });
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        speechToText.setOnClickListener(view -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, String.format("Name a product!"));

            try {
                startActivityForResult(intent, 123);
            } catch (ActivityNotFoundException e) {
                Alerter.create(SearchProductActivity.this)
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

        ////////////////////////////////////////////////////////////////////////////////////////////

        productsList = new ArrayList<>();
        productsRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                productsList.add(documentSnapshot.getString(Constants.KEY_PRODUCT_NAME));
            }
        }).addOnFailureListener(e ->
                Alerter.create(SearchProductActivity.this)
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
                        .show());

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(SearchProductActivity.this, R.layout.layout_search_text, productsList);
        inputProductSearch.setAdapter(adapter);

        ////////////////////////////////////////////////////////////////////////////////////////////

        inputProductSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (inputProductSearch.getText().toString().isEmpty()) {
                    UIUtil.hideKeyboard(SearchProductActivity.this);

                    recyclerProducts.setHasFixedSize(true);
                    recyclerProducts.setLayoutManager(null);
                    recyclerProducts.setAdapter(null);

                    layoutSearch.setVisibility(View.VISIBLE);
                    layoutEmpty.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    Alerter.create(SearchProductActivity.this)
                            .setText("Type something in the search bar")
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
                    UIUtil.hideKeyboard(SearchProductActivity.this);
                    layoutSearch.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);

                    Query query = productsRef.orderBy(Constants.KEY_PRODUCT_SEARCH_KEYWORD, Query.Direction.ASCENDING)
                            .startAt(inputProductSearch.getText().toString().toLowerCase().trim())
                            .endAt(inputProductSearch.getText().toString().toLowerCase().trim() + "\uf8ff");

                    PagingConfig config = new PagingConfig(4, 4, false);

                    FirestorePagingOptions<Product> options = new FirestorePagingOptions.Builder<Product>()
                            .setLifecycleOwner(SearchProductActivity.this)
                            .setQuery(query, config, Product.class)
                            .build();

                    loadProducts(options);
                }
                return true;
            }
            return false;
        });
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 123:
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    inputProductSearch.setText(result.get(0));
                    inputProductSearch.clearFocus();

                    if (inputProductSearch.getText().toString().isEmpty()) {
                        UIUtil.hideKeyboard(SearchProductActivity.this);

                        recyclerProducts.setHasFixedSize(true);
                        recyclerProducts.setLayoutManager(null);
                        recyclerProducts.setAdapter(null);

                        layoutSearch.setVisibility(View.VISIBLE);
                        layoutEmpty.setVisibility(View.GONE);
                        progressBar.setVisibility(View.GONE);
                        Alerter.create(SearchProductActivity.this)
                                .setText("Type something in the search bar")
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
                        UIUtil.hideKeyboard(SearchProductActivity.this);
                        layoutSearch.setVisibility(View.GONE);
                        progressBar.setVisibility(View.VISIBLE);

                        Query query = productsRef.orderBy(Constants.KEY_PRODUCT_SEARCH_KEYWORD, Query.Direction.ASCENDING)
                                .startAt(inputProductSearch.getText().toString().toLowerCase().trim())
                                .endAt(inputProductSearch.getText().toString().toLowerCase().trim() + "\uf8ff");

                        PagingConfig config = new PagingConfig(4, 4, false);

                        FirestorePagingOptions<Product> options = new FirestorePagingOptions.Builder<Product>()
                                .setLifecycleOwner(SearchProductActivity.this)
                                .setQuery(query, config, Product.class)
                                .build();

                        loadProducts(options);
                    }
                }
                break;
        }
    }

    ///////////////////////////////////////// Load Products ////////////////////////////////////////

    @SuppressLint("NotifyDataSetChanged")
    private void loadProducts(FirestorePagingOptions<Product> options) {
        productAdapter = new FirestorePagingAdapter<Product, ProductViewHolder>(options) {

            @NonNull
            @Override
            public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_product, parent, false);
                return new ProductViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull ProductViewHolder holder, int position, @NonNull Product model) {
                Glide.with(holder.productImage.getContext()).load(model.getProductImage())
                        .placeholder(R.drawable.thumbnail).centerCrop().into(holder.productImage);

                ////////////////////////////////////////////////////////////////////////////////////

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

                ////////////////////////////////////////////////////////////////////////////////////

                holder.productName.setText(model.getProductName());
                holder.productUnit.setText(model.getProductUnit());
                holder.productCategory.setText(model.getProductCategory());
                holder.productPrice.setText(String.format("₹ %s", model.getProductRetailPrice()));

                ////////////////////////////////////////////////////////////////////////////////////

                if (model.getProductOffer() == 0) {
                    holder.productMRP.setVisibility(View.GONE);
                    holder.productOffer.setVisibility(View.GONE);
                } else {
                    holder.productMRP.setVisibility(View.VISIBLE);
                    holder.productMRP.setText(String.format("₹ %s", model.getProductMRP()));
                    holder.productMRP.setPaintFlags(holder.productMRP.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    Shimmer shimmer = new Shimmer();
                    holder.productOffer.setVisibility(View.VISIBLE);
                    holder.productOffer.setText(String.format("%d%% OFF", model.getProductOffer()));
                    shimmer.start(holder.productOffer);
                }

                ////////////////////////////////////////////////////////////////////////////////////

                holder.clickListener.setOnClickListener(v -> {
                    preferenceManager.putString(Constants.KEY_PRODUCT, model.getProductID());
                    startActivity(new Intent(SearchProductActivity.this, ProductDetailsActivity.class));
                    CustomIntent.customType(SearchProductActivity.this, "bottom-to-up");
                });

                ////////////////////////////////////////////////////////////////////////////////////

                if (model.isProductInStock()) {
                    holder.productImage.clearColorFilter();
                    holder.productOffer.setVisibility(View.VISIBLE);

                    holder.productStatus.setText("In Stock");
                    holder.productStatus.setTextColor(getColor(R.color.successColor));

                    holder.productUnitInStock.setVisibility(View.VISIBLE);
                    if (model.getProductUnitsInStock() >= 1 && model.getProductUnitsInStock() <= 5) {
                        holder.productUnitInStock.setText(String.format("(Hurry! Only %d left)", model.getProductUnitsInStock()));
                    } else if (model.getProductUnitsInStock() > 5) {
                        holder.productUnitInStock.setText(String.format("(%d units left)", model.getProductUnitsInStock()));
                    }

                    holder.addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorAccent));
                    holder.addToCartBtn.setEnabled(true);
                    holder.addToCartBtn.setOnClickListener(v -> {
                        if (!isConnectedToInternet(SearchProductActivity.this)) {
                            showConnectToInternetDialog();
                            return;
                        } else {
                            loadingDialog.startDialog();

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
                                                loadingDialog.dismissDialog();
                                                Alerter.create(SearchProductActivity.this)
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
                                                loadingDialog.dismissDialog();
                                                Alerter.create(SearchProductActivity.this)
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
                                            loadingDialog.dismissDialog();
                                            MaterialDialog materialDialog = new MaterialDialog.Builder(SearchProductActivity.this)
                                                    .setTitle("Item cannot be added to your cart!")
                                                    .setMessage("Your cart has already been setup for another store this item does not belong to. You must clear your cart first before proceeding with this item.")
                                                    .setCancelable(false)
                                                    .setPositiveButton("Go to Cart", R.drawable.ic_dialog_cart, (dialogInterface, which) -> {
                                                        dialogInterface.dismiss();
                                                        startActivity(new Intent(getApplicationContext(), CartActivity.class));
                                                        CustomIntent.customType(SearchProductActivity.this, "bottom-to-up");
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
                                                                                loadingDialog.dismissDialog();
                                                                                Alerter.create(SearchProductActivity.this)
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
                                                                        loadingDialog.dismissDialog();
                                                                        Alerter.create(SearchProductActivity.this)
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
                                                                    loadingDialog.dismissDialog();
                                                                    MaterialDialog materialDialog = new MaterialDialog.Builder(SearchProductActivity.this)
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
                                                                            loadingDialog.dismissDialog();
                                                                            Alerter.create(SearchProductActivity.this)
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
                                                                            loadingDialog.dismissDialog();
                                                                            Alerter.create(SearchProductActivity.this)
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
                                                            loadingDialog.dismissDialog();
                                                            Alerter.create(SearchProductActivity.this)
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
                                                loadingDialog.dismissDialog();
                                                Alerter.create(SearchProductActivity.this)
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
                                        loadingDialog.dismissDialog();
                                        Alerter.create(SearchProductActivity.this)
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
                                loadingDialog.dismissDialog();
                                Alerter.create(SearchProductActivity.this)
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

                    holder.productStatus.setText("Out of Stock");
                    holder.productStatus.setTextColor(getColor(R.color.errorColor));
                    holder.productUnitInStock.setVisibility(View.GONE);

                    holder.addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorInactive));
                    holder.addToCartBtn.setEnabled(false);
                }

                setAnimation(holder.itemView, position);
            }

            public void setAnimation(View viewToAnimate, int position) {
                if (position > LAST_POSITION) {
                    ScaleAnimation scaleAnimation = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f,
                            Animation.RELATIVE_TO_SELF, 0.5f,
                            Animation.RELATIVE_TO_SELF, 0.5f);
                    scaleAnimation.setDuration(1000);

                    viewToAnimate.setAnimation(scaleAnimation);
                    LAST_POSITION = position;
                }
            }
        };

        productAdapter.notifyDataSetChanged();

        productAdapter.addLoadStateListener(states -> {
            LoadState refresh = states.getRefresh();
            LoadState append = states.getAppend();

            if (refresh instanceof LoadState.Error || append instanceof LoadState.Error) {
                progressBar.setVisibility(View.GONE);
                layoutSearch.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.GONE);
                Alerter.create(SearchProductActivity.this)
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

            if (refresh instanceof LoadState.Loading) {

            }

            if (append instanceof LoadState.Loading) {
                progressBar.setVisibility(View.VISIBLE);
                layoutSearch.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.GONE);
            }

            if (append instanceof LoadState.NotLoading) {
                LoadState.NotLoading notLoading = (LoadState.NotLoading) append;
                if (notLoading.getEndOfPaginationReached()) {
                    progressBar.setVisibility(View.GONE);

                    if (productAdapter.getItemCount() == 0) {
                        layoutSearch.setVisibility(View.GONE);
                        layoutEmpty.setVisibility(View.VISIBLE);
                    } else {
                        layoutSearch.setVisibility(View.GONE);
                        layoutEmpty.setVisibility(View.GONE);
                    }
                    return null;
                }

                if (refresh instanceof LoadState.NotLoading) {
                    return null;
                }
            }

            return null;
        });

        recyclerProducts.setHasFixedSize(true);
        recyclerProducts.setLayoutManager(new LinearLayoutManager(SearchProductActivity.this));
        recyclerProducts.setAdapter(productAdapter);
    }

    //////////////////////////////////// ProductViewHolder /////////////////////////////////////////

    public static class ProductViewHolder extends RecyclerView.ViewHolder {

        CardView addToCartBtnContainer;
        ConstraintLayout clickListener, addToCartBtn;
        ImageView productImage, productTypeImage;
        TextView productName, productUnit, productCategory, productStatus, productUnitInStock, productPrice, productMRP;
        ShimmerTextView productOffer;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);

            clickListener = itemView.findViewById(R.id.click_listener);
            productImage = itemView.findViewById(R.id.product_image);
            productTypeImage = itemView.findViewById(R.id.product_type);
            productOffer = itemView.findViewById(R.id.product_offer);
            productName = itemView.findViewById(R.id.product_name);
            productUnit = itemView.findViewById(R.id.product_unit);
            productCategory = itemView.findViewById(R.id.product_category);
            productStatus = itemView.findViewById(R.id.product_status);
            productUnitInStock = itemView.findViewById(R.id.product_unit_in_stock);
            productPrice = itemView.findViewById(R.id.product_price);
            productMRP = itemView.findViewById(R.id.product_mrp);
            addToCartBtnContainer = itemView.findViewById(R.id.add_to_cart_btn_container);
            addToCartBtn = itemView.findViewById(R.id.add_to_cart_btn);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(SearchProductActivity searchProductActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) searchProductActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(SearchProductActivity.this)
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
        KeyboardVisibilityEvent.setEventListener(SearchProductActivity.this, isOpen -> {
            if (isOpen) {
                UIUtil.hideKeyboard(SearchProductActivity.this);
            }
        });
        CustomIntent.customType(SearchProductActivity.this, "up-to-bottom");
    }
}