package com.application.grocertaxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.application.grocertaxi.Model.Product;
import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.paging.FirestorePagingAdapter;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.firebase.ui.firestore.paging.LoadingState;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.ArrayList;
import java.util.Locale;

import maes.tech.intentanim.CustomIntent;

public class SearchProductActivity extends AppCompatActivity {

    private ImageView actionBtn, speechToText, illustrationSearch;
    private EditText inputProductSearch;
    private RecyclerView recyclerProducts;
    private TextView textSearch;
    private ProgressBar productsProgressBar;

    private CollectionReference productsRef;
    private FirestorePagingAdapter<Product, ProductViewHolder> productAdapter;

    private PreferenceManager preferenceManager;
    private static int LAST_POSITION = -1;

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
        } else if (preferenceManager.getString(Constants.KEY_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_CITY) == null ||
                preferenceManager.getString(Constants.KEY_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_LOCALITY).equals("") ||
                preferenceManager.getString(Constants.KEY_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_LOCALITY).isEmpty()) {
            Intent intent = new Intent(SearchProductActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(SearchProductActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorSearch));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        initViews();
        initFirebase();
        setActionOnViews();
    }

    private void initViews() {
        actionBtn = findViewById(R.id.action_btn);
        speechToText = findViewById(R.id.speech_to_text);
        illustrationSearch = findViewById(R.id.illustration_search);
        inputProductSearch = findViewById(R.id.input_product_search_field);
        recyclerProducts = findViewById(R.id.recycler_products);
        textSearch = findViewById(R.id.text_search);
        productsProgressBar = findViewById(R.id.products_progress_bar);
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
                return;
            }
        });

        inputProductSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (inputProductSearch.getText().toString().isEmpty()) {
                    UIUtil.hideKeyboard(SearchProductActivity.this);

                    recyclerProducts.setHasFixedSize(true);
                    recyclerProducts.setLayoutManager(null);
                    recyclerProducts.setAdapter(null);

                    illustrationSearch.setVisibility(View.VISIBLE);
                    textSearch.setVisibility(View.VISIBLE);
                    productsProgressBar.setVisibility(View.GONE);
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
                    illustrationSearch.setVisibility(View.GONE);
                    textSearch.setVisibility(View.GONE);
                    productsProgressBar.setVisibility(View.VISIBLE);

                    Query query = productsRef.orderBy(Constants.KEY_PRODUCT_SEARCH_KEYWORD, Query.Direction.ASCENDING)
                            .startAt(inputProductSearch.getText().toString().toLowerCase().trim())
                            .endAt(inputProductSearch.getText().toString().toLowerCase().trim() + "\uf8ff");

                    PagedList.Config config = new PagedList.Config.Builder()
                            .setInitialLoadSizeHint(8)
                            .setPageSize(4)
                            .build();

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

    private void loadProducts(FirestorePagingOptions<Product> options) {
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
                        .placeholder(R.drawable.thumbnail).centerCrop().into(holder.productImage);

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
                        preferenceManager.putString(Constants.KEY_PRODUCT, model.getProductID());
                        startActivity(new Intent(SearchProductActivity.this, ProductDetailsActivity.class));
                        CustomIntent.customType(SearchProductActivity.this, "bottom-to-up");
                    }
                });

                if(model.isProductInStock()) {
                    holder.addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorAccent));
                    holder.addToCartBtn.setEnabled(true);
                    holder.addToCartBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                        }
                    });
                } else {
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
                        productsProgressBar.setVisibility(View.VISIBLE);
                        illustrationSearch.setVisibility(View.GONE);
                        textSearch.setVisibility(View.GONE);
                        break;
                    case LOADED:
                    case FINISHED:
                        productsProgressBar.setVisibility(View.GONE);

                        if (getItemCount() == 0) {
                            illustrationSearch.setVisibility(View.VISIBLE);
                            illustrationSearch.setImageResource(R.drawable.illustration_empty1);
                            textSearch.setVisibility(View.VISIBLE);
                            textSearch.setText("Nothing here");
                        } else {
                            illustrationSearch.setVisibility(View.GONE);
                            textSearch.setVisibility(View.GONE);
                        }
                        break;
                    case ERROR:
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
                        break;
                }
            }
        };

        productAdapter.notifyDataSetChanged();

        recyclerProducts.setHasFixedSize(true);
        recyclerProducts.setLayoutManager(new GridLayoutManager(SearchProductActivity.this, 2));
        recyclerProducts.setAdapter(productAdapter);
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {

        CardView addToCartBtnContainer;
        ConstraintLayout clickListener, addToCartBtn;
        ImageView productImage, productTypeImage;
        TextView productName, productPrice, productMRP, productOffer, productUnit;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);

            clickListener = itemView.findViewById(R.id.click_listener);
            productImage = itemView.findViewById(R.id.product_image);
            productTypeImage = itemView.findViewById(R.id.product_type_image);
            productName = itemView.findViewById(R.id.product_name);
            addToCartBtnContainer = itemView.findViewById(R.id.add_to_cart_btn_container);
            addToCartBtn = itemView.findViewById(R.id.add_to_cart_btn);
            productPrice = itemView.findViewById(R.id.product_price);
            productMRP = itemView.findViewById(R.id.product_mrp);
            productOffer = itemView.findViewById(R.id.product_offer);
            productUnit = itemView.findViewById(R.id.product_unit);
        }
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

                    if (inputProductSearch.getText().toString().isEmpty()) {
                        UIUtil.hideKeyboard(SearchProductActivity.this);

                        recyclerProducts.setHasFixedSize(true);
                        recyclerProducts.setLayoutManager(null);
                        recyclerProducts.setAdapter(null);

                        illustrationSearch.setVisibility(View.VISIBLE);
                        textSearch.setVisibility(View.VISIBLE);
                        productsProgressBar.setVisibility(View.GONE);
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
                        illustrationSearch.setVisibility(View.GONE);
                        textSearch.setVisibility(View.GONE);
                        productsProgressBar.setVisibility(View.VISIBLE);

                        Query query = productsRef.orderBy(Constants.KEY_PRODUCT_SEARCH_KEYWORD, Query.Direction.ASCENDING)
                                .startAt(inputProductSearch.getText().toString().toLowerCase().trim())
                                .endAt(inputProductSearch.getText().toString().toLowerCase().trim() + "\uf8ff");

                        PagedList.Config config = new PagedList.Config.Builder()
                                .setInitialLoadSizeHint(8)
                                .setPageSize(4)
                                .build();

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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        CustomIntent.customType(SearchProductActivity.this, "up-to-bottom");
    }
}