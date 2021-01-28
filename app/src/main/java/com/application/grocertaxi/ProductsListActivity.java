package com.application.grocertaxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
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
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.application.grocertaxi.Model.Product;
import com.application.grocertaxi.Model.Store;
import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.baoyz.widget.PullRefreshLayout;
import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.paging.FirestorePagingAdapter;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.firebase.ui.firestore.paging.LoadingState;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.ArrayList;
import java.util.Locale;

import maes.tech.intentanim.CustomIntent;

public class ProductsListActivity extends AppCompatActivity {

    private ImageView backBtn, speechToText, illustrationEmpty;
    private EditText inputProductSearch;
    private RecyclerView recyclerProducts;
    private TextView title, textEmpty;
    private ProgressBar productsProgressBar;
    private ConstraintLayout sortBtn;
    private PullRefreshLayout pullRefreshLayout;

    private CollectionReference productsRef;
    private FirestorePagingAdapter<Product, ProductViewHolder> productAdapter;

    private PreferenceManager preferenceManager;
    private static int LAST_POSITION = -1;

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
        } else if (preferenceManager.getString(Constants.KEY_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_CITY) == null ||
                preferenceManager.getString(Constants.KEY_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_LOCALITY).equals("") ||
                preferenceManager.getString(Constants.KEY_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_LOCALITY).isEmpty()) {
            Intent intent = new Intent(ProductsListActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(ProductsListActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        initViews();
        initFirebase();
        setActionOnViews();

        loadProducts();

        pullRefreshLayout.setColor(getColor(R.color.colorAccent));
        pullRefreshLayout.setOnRefreshListener(() -> {
            if(!isConnectedToInternet(ProductsListActivity.this)) {
                pullRefreshLayout.setRefreshing(false);
                showConnectToInternetDialog();
                return;
            } else {
                UIUtil.hideKeyboard(ProductsListActivity.this);
                inputProductSearch.setText(null);
                inputProductSearch.clearFocus();
                loadProducts();
            }
        });
    }

    private void initViews() {
        backBtn = findViewById(R.id.back_btn);
        title = findViewById(R.id.products_list_title);
        sortBtn = findViewById(R.id.sort_btn);
        speechToText = findViewById(R.id.speech_to_text);
        illustrationEmpty = findViewById(R.id.illustration_empty);
        inputProductSearch = findViewById(R.id.input_product_search_field);
        recyclerProducts = findViewById(R.id.recycler_products);
        textEmpty = findViewById(R.id.text_empty);
        productsProgressBar = findViewById(R.id.products_progress_bar);
        pullRefreshLayout = findViewById(R.id.pull_refresh_layout);
    }

    private void initFirebase() {
        if (preferenceManager.getString(Constants.KEY_CATEGORY).isEmpty() || preferenceManager.getString(Constants.KEY_CATEGORY).equals("")) {
            productsRef = FirebaseFirestore.getInstance()
                    .collection(Constants.KEY_COLLECTION_CITIES)
                    .document(preferenceManager.getString(Constants.KEY_CITY))
                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                    .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                    .collection(Constants.KEY_COLLECTION_PRODUCTS);
        } else {
            productsRef = FirebaseFirestore.getInstance()
                    .collection(Constants.KEY_COLLECTION_CITIES)
                    .document(preferenceManager.getString(Constants.KEY_CITY))
                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                    .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                    .collection(Constants.KEY_COLLECTION_CATEGORIES)
                    .document(preferenceManager.getString(Constants.KEY_CATEGORY))
                    .collection(Constants.KEY_COLLECTION_PRODUCTS);
        }
    }

    private void setActionOnViews() {
        backBtn.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });

        KeyboardVisibilityEvent.setEventListener(ProductsListActivity.this, isOpen -> {
            if (!isOpen) {
                inputProductSearch.clearFocus();
            }
        });

        if (preferenceManager.getString(Constants.KEY_CATEGORY).isEmpty() || preferenceManager.getString(Constants.KEY_CATEGORY).equals("")) {
            title.setText("All products");
            inputProductSearch.setHint("Search product");
        } else {
            title.setText(preferenceManager.getString(Constants.KEY_CATEGORY));
            inputProductSearch.setHint(String.format("Search in %s", preferenceManager.getString(Constants.KEY_CATEGORY)));
        }

        productsProgressBar.setVisibility(View.VISIBLE);

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
                return;
            }
        });

        inputProductSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Query updatedQuery;
                if (s.toString().isEmpty()) {
                    updatedQuery = productsRef.orderBy(Constants.KEY_PRODUCT_NAME, Query.Direction.ASCENDING);
                } else {
                    updatedQuery = productsRef.orderBy(Constants.KEY_PRODUCT_SEARCH_KEYWORD, Query.Direction.ASCENDING)
                            .startAt(s.toString().toLowerCase().trim()).endAt(s.toString().toLowerCase().trim() + "\uf8ff");
                }

                PagedList.Config updatedConfig = new PagedList.Config.Builder()
                        .setInitialLoadSizeHint(8)
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
                            updatedQuery = productsRef.orderBy(Constants.KEY_PRODUCT_NAME, Query.Direction.ASCENDING);
                        } else {
                            updatedQuery = productsRef.orderBy(Constants.KEY_PRODUCT_SEARCH_KEYWORD, Query.Direction.ASCENDING)
                                    .startAt(s.toString().toLowerCase().trim()).endAt(s.toString().toLowerCase().trim() + "\uf8ff");
                        }

                        PagedList.Config updatedConfig = new PagedList.Config.Builder()
                                .setInitialLoadSizeHint(8)
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

        sortBtn.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
            popupMenu.inflate(R.menu.menu_product_sort);
            popupMenu.setOnMenuItemClickListener(item -> {
                Query updatedQuery;
                PagedList.Config updatedConfig;
                FirestorePagingOptions<Product> updatedOptions;
                switch (item.getItemId()) {
                    case R.id.menu_name_az:
                        updatedQuery = productsRef.orderBy(Constants.KEY_PRODUCT_NAME, Query.Direction.ASCENDING);

                        updatedConfig = new PagedList.Config.Builder()
                                .setInitialLoadSizeHint(8)
                                .setPageSize(4)
                                .build();

                        updatedOptions = new FirestorePagingOptions.Builder<Product>()
                                .setLifecycleOwner(ProductsListActivity.this)
                                .setQuery(updatedQuery, updatedConfig, Product.class)
                                .build();

                        productAdapter.updateOptions(updatedOptions);
                        return true;
                    case R.id.menu_name_za:
                        updatedQuery = productsRef.orderBy(Constants.KEY_PRODUCT_NAME, Query.Direction.DESCENDING);

                        updatedConfig = new PagedList.Config.Builder()
                                .setInitialLoadSizeHint(8)
                                .setPageSize(4)
                                .build();

                        updatedOptions = new FirestorePagingOptions.Builder<Product>()
                                .setLifecycleOwner(ProductsListActivity.this)
                                .setQuery(updatedQuery, updatedConfig, Product.class)
                                .build();

                        productAdapter.updateOptions(updatedOptions);
                        return true;
                    case R.id.menu_newest_first:
                        updatedQuery = productsRef.orderBy(Constants.KEY_PRODUCT_TIMESTAMP, Query.Direction.DESCENDING);

                        updatedConfig = new PagedList.Config.Builder()
                                .setInitialLoadSizeHint(8)
                                .setPageSize(4)
                                .build();

                        updatedOptions = new FirestorePagingOptions.Builder<Product>()
                                .setLifecycleOwner(ProductsListActivity.this)
                                .setQuery(updatedQuery, updatedConfig, Product.class)
                                .build();

                        productAdapter.updateOptions(updatedOptions);
                        return true;
                    case R.id.menu_price_low_to_high:
                        updatedQuery = productsRef.orderBy(Constants.KEY_PRODUCT_RETAIL_PRICE, Query.Direction.ASCENDING);

                        updatedConfig = new PagedList.Config.Builder()
                                .setInitialLoadSizeHint(8)
                                .setPageSize(4)
                                .build();

                        updatedOptions = new FirestorePagingOptions.Builder<Product>()
                                .setLifecycleOwner(ProductsListActivity.this)
                                .setQuery(updatedQuery, updatedConfig, Product.class)
                                .build();

                        productAdapter.updateOptions(updatedOptions);
                        return true;
                    case R.id.menu_price_high_to_low:
                        updatedQuery = productsRef.orderBy(Constants.KEY_PRODUCT_RETAIL_PRICE, Query.Direction.DESCENDING);

                        updatedConfig = new PagedList.Config.Builder()
                                .setInitialLoadSizeHint(8)
                                .setPageSize(4)
                                .build();

                        updatedOptions = new FirestorePagingOptions.Builder<Product>()
                                .setLifecycleOwner(ProductsListActivity.this)
                                .setQuery(updatedQuery, updatedConfig, Product.class)
                                .build();

                        productAdapter.updateOptions(updatedOptions);
                        return true;
                    case R.id.menu_exclude_out_of_stock:
                        updatedQuery = productsRef.whereEqualTo(Constants.KEY_PRODUCT_IN_STOCK, true)
                                .orderBy(Constants.KEY_PRODUCT_NAME, Query.Direction.ASCENDING);

                        updatedConfig = new PagedList.Config.Builder()
                                .setInitialLoadSizeHint(8)
                                .setPageSize(4)
                                .build();

                        updatedOptions = new FirestorePagingOptions.Builder<Product>()
                                .setLifecycleOwner(ProductsListActivity.this)
                                .setQuery(updatedQuery, updatedConfig, Product.class)
                                .build();

                        productAdapter.updateOptions(updatedOptions);
                        return true;

                    default:
                        return false;
                }
            });
            popupMenu.show();
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

    private void loadProducts() {
        Query query = productsRef.orderBy(Constants.KEY_PRODUCT_NAME, Query.Direction.ASCENDING);

        PagedList.Config config = new PagedList.Config.Builder()
                .setInitialLoadSizeHint(8)
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
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_product_item, parent, false);
                return new ProductViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull ProductViewHolder holder, int position, @NonNull Product model) {
                Glide.with(holder.productImage.getContext()).load(model.getProductImage())
                        .placeholder(R.drawable.thumbnail_product).centerCrop().into(holder.productImage);

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

                holder.productName.setText(model.getProductName());
                holder.productUnit.setText(model.getProductUnit());

                if (model.getProductRetailPrice() == model.getProductMRP()) {
                    holder.productMRP.setVisibility(View.GONE);
                    holder.productOffer.setVisibility(View.GONE);
                } else {
                    holder.productMRP.setText(String.format("₹ %s", model.getProductMRP()));
                    holder.productMRP.setPaintFlags(holder.productMRP.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    float offer = (float) (((model.getProductMRP() - model.getProductRetailPrice()) / model.getProductMRP()) * 100);
                    String offer_value = ((int) offer) + "% off";
                    holder.productOffer.setText(offer_value);
                }

                holder.productPrice.setText(String.format("₹ %s", model.getProductRetailPrice()));

                holder.clickListener.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });

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
                        productsProgressBar.setVisibility(View.VISIBLE);
                        illustrationEmpty.setVisibility(View.GONE);
                        textEmpty.setVisibility(View.GONE);
                        break;
                    case LOADED:
                    case FINISHED:
                        pullRefreshLayout.setRefreshing(false);
                        productsProgressBar.setVisibility(View.GONE);

                        if (getItemCount() == 0) {
                            illustrationEmpty.setVisibility(View.VISIBLE);
                            textEmpty.setVisibility(View.VISIBLE);
                        } else {
                            illustrationEmpty.setVisibility(View.GONE);
                            textEmpty.setVisibility(View.GONE);
                        }
                        break;
                    case ERROR:
                        pullRefreshLayout.setRefreshing(false);
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
        recyclerProducts.setLayoutManager(new GridLayoutManager(ProductsListActivity.this, 2));
        recyclerProducts.setAdapter(productAdapter);
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout clickListener;
        ImageView productImage, productTypeImage;
        TextView productName, productPrice, productMRP, productOffer, productUnit;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);

            clickListener = itemView.findViewById(R.id.click_listener);
            productImage = itemView.findViewById(R.id.product_image);
            productTypeImage = itemView.findViewById(R.id.product_type_image);
            productName = itemView.findViewById(R.id.product_name);
            productPrice = itemView.findViewById(R.id.product_price);
            productMRP = itemView.findViewById(R.id.product_mrp);
            productOffer = itemView.findViewById(R.id.product_offer);
            productUnit = itemView.findViewById(R.id.product_unit);
        }
    }

    private boolean isConnectedToInternet(ProductsListActivity productsListActivity) {
        ConnectivityManager connectivityManager = (ConnectivityManager) productsListActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if ((wifiConn != null && wifiConn.isConnected()) || (mobileConn != null && mobileConn.isConnected())) {
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
        CustomIntent.customType(ProductsListActivity.this, "right-to-left");
    }
}