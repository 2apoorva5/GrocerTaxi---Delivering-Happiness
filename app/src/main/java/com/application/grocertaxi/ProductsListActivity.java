package com.application.grocertaxi;

import android.app.AlertDialog;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.application.grocertaxi.Model.Product;
import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.baoyz.widget.PullRefreshLayout;
import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.paging.FirestorePagingAdapter;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.firebase.ui.firestore.paging.LoadingState;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import dmax.dialog.SpotsDialog;
import maes.tech.intentanim.CustomIntent;

public class ProductsListActivity extends AppCompatActivity {

    private ImageView backBtn, speechToText;
    private EditText inputProductSearch;
    private RecyclerView recyclerProducts;
    private TextView title, cartInfo;
    private ConstraintLayout layoutContent, layoutEmpty, layoutCartInfo, cartBtn, layoutNoInternet, retryBtn, sortBtn;
    private PullRefreshLayout pullRefreshLayout;
    private ShimmerFrameLayout shimmerLayout;

    private CollectionReference productsRef, cartRef;
    private FirestorePagingAdapter<Product, ProductViewHolder> productAdapter;

    private String cart_location;
    private PreferenceManager preferenceManager;
    private static int LAST_POSITION = -1;
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products_list);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(ProductsListActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(ProductsListActivity.this, "fadein-to-fadeout");
            finish();
        } else if (preferenceManager.getString(Constants.KEY_USER_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_CITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).isEmpty()) {
            Intent intent = new Intent(ProductsListActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(ProductsListActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        progressDialog = new SpotsDialog.Builder().setContext(ProductsListActivity.this)
                .setMessage("Adding item to cart..")
                .setCancelable(false)
                .setTheme(R.style.SpotsDialog)
                .build();

        ////////////////////////////////////////////////////////////////////////////////////////////

        cart_location = String.format("%s, %s", preferenceManager.getString(Constants.KEY_USER_LOCALITY), preferenceManager.getString(Constants.KEY_USER_CITY));

        ////////////////////////////////////////////////////////////////////////////////////////////

        backBtn = findViewById(R.id.back_btn);
        title = findViewById(R.id.products_list_title);
        sortBtn = findViewById(R.id.sort_btn);
        speechToText = findViewById(R.id.speech_to_text);
        inputProductSearch = findViewById(R.id.input_product_search_field);
        recyclerProducts = findViewById(R.id.recycler_products);
        pullRefreshLayout = findViewById(R.id.pull_refresh_layout);
        shimmerLayout = findViewById(R.id.shimmer_layout);
        layoutContent = findViewById(R.id.layout_content);
        layoutEmpty = findViewById(R.id.layout_empty);
        layoutNoInternet = findViewById(R.id.layout_no_internet);
        retryBtn = findViewById(R.id.retry_btn);
        layoutCartInfo = findViewById(R.id.layout_cart_info);
        cartInfo = findViewById(R.id.cart_info);
        cartBtn = findViewById(R.id.cart_btn);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkNetworkConnection();
    }

    private void checkNetworkConnection() {
        if (!isConnectedToInternet(ProductsListActivity.this)) {
            layoutContent.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
            layoutNoInternet.setVisibility(View.VISIBLE);
            retryBtn.setOnClickListener(v -> checkNetworkConnection());
        } else {
            initFirebase();
            setActionOnViews();
        }
    }

    private void initFirebase() {
        if (preferenceManager.getString(Constants.KEY_CATEGORY).isEmpty() || preferenceManager.getString(Constants.KEY_CATEGORY).equals("")) {
            productsRef = FirebaseFirestore.getInstance()
                    .collection(Constants.KEY_COLLECTION_CITIES)
                    .document(preferenceManager.getString(Constants.KEY_USER_CITY))
                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                    .document(preferenceManager.getString(Constants.KEY_USER_LOCALITY))
                    .collection(Constants.KEY_COLLECTION_PRODUCTS);
        } else {
            productsRef = FirebaseFirestore.getInstance()
                    .collection(Constants.KEY_COLLECTION_CITIES)
                    .document(preferenceManager.getString(Constants.KEY_USER_CITY))
                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                    .document(preferenceManager.getString(Constants.KEY_USER_LOCALITY))
                    .collection(Constants.KEY_COLLECTION_CATEGORIES)
                    .document(preferenceManager.getString(Constants.KEY_CATEGORY))
                    .collection(Constants.KEY_COLLECTION_PRODUCTS);
        }

        cartRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .collection(Constants.KEY_COLLECTION_CART);
    }

    private void setActionOnViews() {
        layoutNoInternet.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        layoutCartInfo.setVisibility(View.GONE);

        pullRefreshLayout.setRefreshing(false);

        ////////////////////////////////////////////////////////////////////////////////////////////

        pullRefreshLayout.setColor(getColor(R.color.colorAccent));
        pullRefreshLayout.setBackgroundColor(getColor(R.color.colorBackground));
        pullRefreshLayout.setOnRefreshListener(this::checkNetworkConnection);

        ////////////////////////////////////////////////////////////////////////////////////////////

        backBtn.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        KeyboardVisibilityEvent.setEventListener(ProductsListActivity.this, isOpen -> {
            if (!isOpen) {
                inputProductSearch.clearFocus();
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        if (preferenceManager.getString(Constants.KEY_CATEGORY).isEmpty() || preferenceManager.getString(Constants.KEY_CATEGORY).equals("")) {
            title.setText("All products");
            inputProductSearch.setHint("Search products");
        } else {
            title.setText(preferenceManager.getString(Constants.KEY_CATEGORY));
            inputProductSearch.setHint(String.format("Search in %s", preferenceManager.getString(Constants.KEY_CATEGORY)));
        }

        ////////////////////////////////////////////////////////////////////////////////////////////

        loadProducts();

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
                Alerter.create(ProductsListActivity.this)
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

        inputProductSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Query updatedQuery;
                if (s.toString().isEmpty()) {
                    updatedQuery = productsRef;
                } else {
                    updatedQuery = productsRef.orderBy(Constants.KEY_PRODUCT_SEARCH_KEYWORD, Query.Direction.ASCENDING)
                            .startAt(s.toString().toLowerCase().trim()).endAt(s.toString().toLowerCase().trim() + "\uf8ff");
                }

                PagedList.Config updatedConfig = new PagedList.Config.Builder()
                        .setInitialLoadSizeHint(4)
                        .setPageSize(4)
                        .build();
                FirestorePagingOptions<Product> updatedOptions = new FirestorePagingOptions.Builder<Product>()
                        .setLifecycleOwner(ProductsListActivity.this)
                        .setQuery(updatedQuery, updatedConfig, Product.class)
                        .build();

                productAdapter.updateOptions(updatedOptions);
            }

            @Override
            public void afterTextChanged(Editable s) {
                inputProductSearch.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        UIUtil.hideKeyboard(ProductsListActivity.this);
                        Query updatedQuery;
                        if (s.toString().isEmpty()) {
                            updatedQuery = productsRef;
                        } else {
                            updatedQuery = productsRef.orderBy(Constants.KEY_PRODUCT_SEARCH_KEYWORD, Query.Direction.ASCENDING)
                                    .startAt(s.toString().toLowerCase().trim()).endAt(s.toString().toLowerCase().trim() + "\uf8ff");
                        }

                        PagedList.Config updatedConfig = new PagedList.Config.Builder()
                                .setInitialLoadSizeHint(4)
                                .setPageSize(4)
                                .build();
                        FirestorePagingOptions<Product> updatedOptions = new FirestorePagingOptions.Builder<Product>()
                                .setLifecycleOwner(ProductsListActivity.this)
                                .setQuery(updatedQuery, updatedConfig, Product.class)
                                .build();

                        productAdapter.updateOptions(updatedOptions);
                        return true;
                    }
                    return false;
                });
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        sortBtn.setOnClickListener(v -> {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(ProductsListActivity.this);
            bottomSheetDialog.setContentView(R.layout.bottom_sheet_sort_products);
            bottomSheetDialog.setCanceledOnTouchOutside(false);

            ImageView closeBtn = bottomSheetDialog.findViewById(R.id.close_bottom_sheet_btn);
            ConstraintLayout relevance = bottomSheetDialog.findViewById(R.id.relevance);
            ConstraintLayout newestFirst = bottomSheetDialog.findViewById(R.id.newest_first);
            ConstraintLayout nameAZ = bottomSheetDialog.findViewById(R.id.name_a_z);
            ConstraintLayout nameZA = bottomSheetDialog.findViewById(R.id.name_z_a);
            ConstraintLayout offer = bottomSheetDialog.findViewById(R.id.offer);
            ConstraintLayout priceLowHigh = bottomSheetDialog.findViewById(R.id.price_low_high);
            ConstraintLayout priceHighLow = bottomSheetDialog.findViewById(R.id.price_high_low);
            ConstraintLayout excludeOutOfStock = bottomSheetDialog.findViewById(R.id.exclude_out_of_stock);

            closeBtn.setOnClickListener(v1 -> bottomSheetDialog.dismiss());

            relevance.setOnClickListener(v12 -> {
                Query sortQuery = productsRef;
                PagedList.Config sortConfig = new PagedList.Config.Builder()
                        .setInitialLoadSizeHint(4)
                        .setPageSize(4)
                        .build();
                FirestorePagingOptions<Product> sortOptions = new FirestorePagingOptions.Builder<Product>()
                        .setLifecycleOwner(ProductsListActivity.this)
                        .setQuery(sortQuery, sortConfig, Product.class)
                        .build();

                productAdapter.updateOptions(sortOptions);

                bottomSheetDialog.dismiss();
            });

            newestFirst.setOnClickListener(v13 -> {
                Query sortQuery = productsRef.orderBy(Constants.KEY_PRODUCT_TIMESTAMP, Query.Direction.DESCENDING);
                PagedList.Config sortConfig = new PagedList.Config.Builder()
                        .setInitialLoadSizeHint(4)
                        .setPageSize(4)
                        .build();
                FirestorePagingOptions<Product> sortOptions = new FirestorePagingOptions.Builder<Product>()
                        .setLifecycleOwner(ProductsListActivity.this)
                        .setQuery(sortQuery, sortConfig, Product.class)
                        .build();

                productAdapter.updateOptions(sortOptions);

                bottomSheetDialog.dismiss();
            });

            nameAZ.setOnClickListener(v18 -> {
                Query sortQuery = productsRef.orderBy(Constants.KEY_PRODUCT_NAME, Query.Direction.ASCENDING);
                PagedList.Config sortConfig = new PagedList.Config.Builder()
                        .setInitialLoadSizeHint(4)
                        .setPageSize(4)
                        .build();
                FirestorePagingOptions<Product> sortOptions = new FirestorePagingOptions.Builder<Product>()
                        .setLifecycleOwner(ProductsListActivity.this)
                        .setQuery(sortQuery, sortConfig, Product.class)
                        .build();

                productAdapter.updateOptions(sortOptions);

                bottomSheetDialog.dismiss();
            });

            nameZA.setOnClickListener(v17 -> {
                Query sortQuery = productsRef.orderBy(Constants.KEY_PRODUCT_NAME, Query.Direction.DESCENDING);
                PagedList.Config sortConfig = new PagedList.Config.Builder()
                        .setInitialLoadSizeHint(4)
                        .setPageSize(4)
                        .build();
                FirestorePagingOptions<Product> sortOptions = new FirestorePagingOptions.Builder<Product>()
                        .setLifecycleOwner(ProductsListActivity.this)
                        .setQuery(sortQuery, sortConfig, Product.class)
                        .build();

                productAdapter.updateOptions(sortOptions);

                bottomSheetDialog.dismiss();
            });

            offer.setOnClickListener(v17 -> {
                Query sortQuery = productsRef.orderBy(Constants.KEY_PRODUCT_OFFER, Query.Direction.DESCENDING);
                PagedList.Config sortConfig = new PagedList.Config.Builder()
                        .setInitialLoadSizeHint(4)
                        .setPageSize(4)
                        .build();
                FirestorePagingOptions<Product> sortOptions = new FirestorePagingOptions.Builder<Product>()
                        .setLifecycleOwner(ProductsListActivity.this)
                        .setQuery(sortQuery, sortConfig, Product.class)
                        .build();

                productAdapter.updateOptions(sortOptions);

                bottomSheetDialog.dismiss();
            });

            priceLowHigh.setOnClickListener(v16 -> {
                Query sortQuery = productsRef.orderBy(Constants.KEY_PRODUCT_RETAIL_PRICE, Query.Direction.ASCENDING);
                PagedList.Config sortConfig = new PagedList.Config.Builder()
                        .setInitialLoadSizeHint(4)
                        .setPageSize(4)
                        .build();
                FirestorePagingOptions<Product> sortOptions = new FirestorePagingOptions.Builder<Product>()
                        .setLifecycleOwner(ProductsListActivity.this)
                        .setQuery(sortQuery, sortConfig, Product.class)
                        .build();

                productAdapter.updateOptions(sortOptions);

                bottomSheetDialog.dismiss();
            });

            priceHighLow.setOnClickListener(v15 -> {
                Query sortQuery = productsRef.orderBy(Constants.KEY_PRODUCT_RETAIL_PRICE, Query.Direction.DESCENDING);
                PagedList.Config sortConfig = new PagedList.Config.Builder()
                        .setInitialLoadSizeHint(4)
                        .setPageSize(4)
                        .build();
                FirestorePagingOptions<Product> sortOptions = new FirestorePagingOptions.Builder<Product>()
                        .setLifecycleOwner(ProductsListActivity.this)
                        .setQuery(sortQuery, sortConfig, Product.class)
                        .build();

                productAdapter.updateOptions(sortOptions);

                bottomSheetDialog.dismiss();
            });

            excludeOutOfStock.setOnClickListener(v14 -> {
                Query sortQuery = productsRef.whereEqualTo(Constants.KEY_PRODUCT_IN_STOCK, true);
                PagedList.Config sortConfig = new PagedList.Config.Builder()
                        .setInitialLoadSizeHint(4)
                        .setPageSize(4)
                        .build();
                FirestorePagingOptions<Product> sortOptions = new FirestorePagingOptions.Builder<Product>()
                        .setLifecycleOwner(ProductsListActivity.this)
                        .setQuery(sortQuery, sortConfig, Product.class)
                        .build();

                productAdapter.updateOptions(sortOptions);

                bottomSheetDialog.dismiss();
            });

            bottomSheetDialog.show();
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        cartRef.addSnapshotListener((queryDocumentSnapshots, error) -> {
            if (error != null) {
                progressDialog.dismiss();
                Alerter.create(ProductsListActivity.this)
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
                if (queryDocumentSnapshots.size() != 0 && !queryDocumentSnapshots.isEmpty()) {
                    int cartItemCount = queryDocumentSnapshots.size();
                    double total_price = 0;

                    for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                        total_price += Math.round((queryDocumentSnapshots.getDocuments().get(i).getDouble(Constants.KEY_CART_ITEM_PRODUCT_RETAIL_PRICE)
                                * queryDocumentSnapshots.getDocuments().get(i).getLong(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY)) * 100.0) / 100.0;
                    }

                    layoutCartInfo.setVisibility(View.VISIBLE);
                    if (cartItemCount == 1) {
                        cartInfo.setText(String.format("%d item    |    ₹ %s", cartItemCount, total_price));
                    } else {
                        cartInfo.setText(String.format("%d items    |    ₹ %s", cartItemCount, total_price));
                    }
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

                        startActivity(new Intent(ProductsListActivity.this, CartActivity.class));
                        CustomIntent.customType(ProductsListActivity.this, "bottom-to-up");
                    });
                } else {
                    layoutCartInfo.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 123:
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    inputProductSearch.setText(result.get(0));
                    inputProductSearch.clearFocus();
                }
                break;
        }
    }

    //////////////////////////////////////// Load Products /////////////////////////////////////////

    private void loadProducts() {
        shimmerLayout.setVisibility(View.VISIBLE);
        shimmerLayout.startShimmer();

        Query query = productsRef;
        PagedList.Config config = new PagedList.Config.Builder()
                .setInitialLoadSizeHint(4)
                .setPageSize(4)
                .build();
        FirestorePagingOptions<Product> options = new FirestorePagingOptions.Builder<Product>()
                .setLifecycleOwner(ProductsListActivity.this)
                .setQuery(query, config, Product.class)
                .build();

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
                    startActivity(new Intent(ProductsListActivity.this, ProductDetailsActivity.class));
                    CustomIntent.customType(ProductsListActivity.this, "bottom-to-up");
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
                        if (!isConnectedToInternet(ProductsListActivity.this)) {
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
                                                Alerter.create(ProductsListActivity.this)
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
                                                Alerter.create(ProductsListActivity.this)
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
                                            MaterialDialog materialDialog = new MaterialDialog.Builder(ProductsListActivity.this)
                                                    .setTitle("Item cannot be added to your cart!")
                                                    .setMessage("Your cart has already been setup for another store this item does not belong to. You must clear your cart first before proceeding with this item.")
                                                    .setCancelable(false)
                                                    .setPositiveButton("Go to Cart", R.drawable.ic_dialog_cart, (dialogInterface, which) -> {
                                                        dialogInterface.dismiss();
                                                        startActivity(new Intent(getApplicationContext(), CartActivity.class));
                                                        CustomIntent.customType(ProductsListActivity.this, "bottom-to-up");
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
                                                                                Alerter.create(ProductsListActivity.this)
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
                                                                        Alerter.create(ProductsListActivity.this)
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
                                                                    MaterialDialog materialDialog = new MaterialDialog.Builder(ProductsListActivity.this)
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
                                                                            Alerter.create(ProductsListActivity.this)
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
                                                                            Alerter.create(ProductsListActivity.this)
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
                                                            Alerter.create(ProductsListActivity.this)
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
                                                Alerter.create(ProductsListActivity.this)
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
                                        Alerter.create(ProductsListActivity.this)
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
                                Alerter.create(ProductsListActivity.this)
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

            @Override
            protected void onLoadingStateChanged(@NonNull LoadingState state) {
                super.onLoadingStateChanged(state);
                switch (state) {
                    case LOADING_INITIAL:
                    case LOADING_MORE:
                        pullRefreshLayout.setRefreshing(false);
                        break;
                    case LOADED:
                    case FINISHED:
                        pullRefreshLayout.setRefreshing(false);
                        shimmerLayout.stopShimmer();
                        shimmerLayout.setVisibility(View.GONE);

                        if (getItemCount() == 0) {
                            layoutEmpty.setVisibility(View.VISIBLE);
                        } else {
                            layoutEmpty.setVisibility(View.GONE);
                        }
                        break;
                    case ERROR:
                        pullRefreshLayout.setRefreshing(false);
                        shimmerLayout.stopShimmer();
                        shimmerLayout.setVisibility(View.GONE);
                        Alerter.create(ProductsListActivity.this)
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
                        break;
                }
            }
        };

        productAdapter.notifyDataSetChanged();

        recyclerProducts.setHasFixedSize(true);
        recyclerProducts.setLayoutManager(new LinearLayoutManager(ProductsListActivity.this));
        recyclerProducts.setAdapter(productAdapter);
    }

    ////////////////////////////////////// ProductViewHolder ///////////////////////////////////////

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

    private boolean isConnectedToInternet(ProductsListActivity productsListActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) productsListActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(ProductsListActivity.this)
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
        KeyboardVisibilityEvent.setEventListener(ProductsListActivity.this, isOpen -> {
            if (isOpen) {
                UIUtil.hideKeyboard(ProductsListActivity.this);
            }
        });
        CustomIntent.customType(ProductsListActivity.this, "right-to-left");
    }
}