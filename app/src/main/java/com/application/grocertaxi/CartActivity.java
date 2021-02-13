package com.application.grocertaxi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.application.grocertaxi.Model.CartItem;
import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.baoyz.widget.PullRefreshLayout;
import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import dmax.dialog.SpotsDialog;
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;
import maes.tech.intentanim.CustomIntent;

public class CartActivity extends AppCompatActivity {

    private ImageView closeBtn, illustrationEmpty;
    private NestedScrollView nestedScrollView;
    private TextView textEmpty, coupon, itemsCount, totalMRP, totalDiscount, couponDiscount,
            knowMoreBtn, deliveryCharges, totalAmount, totalAmount2, totalDiscount2;
    private RecyclerView recyclerCart;
    private CardView shopBtnContainer, checkoutBtnContainer;
    private ConstraintLayout shopBtn, applyCouponBtn, checkoutBtn;
    private ProgressBar progressBar;
    private PullRefreshLayout pullRefreshLayout;

    private CollectionReference cartRef;
    private CartAdapter cartAdapter;

    private String cart_location;
    private Shimmer shimmer;
    private PreferenceManager preferenceManager;
    private AlertDialog progressDialog1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(CartActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(CartActivity.this, "fadein-to-fadeout");
            finish();
        } else if (preferenceManager.getString(Constants.KEY_USER_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_CITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).equals("") ||
                    preferenceManager.getString(Constants.KEY_USER_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).isEmpty()) {
            Intent intent = new Intent(CartActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(CartActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        progressDialog1 = new SpotsDialog.Builder().setContext(CartActivity.this)
                .setMessage("Updating cart...")
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
        nestedScrollView = findViewById(R.id.nested_scroll_view);
        illustrationEmpty = findViewById(R.id.illustration_empty);
        textEmpty = findViewById(R.id.text_empty);
        applyCouponBtn = findViewById(R.id.apply_coupon_btn);
        coupon = findViewById(R.id.coupon);
        itemsCount = findViewById(R.id.items_count);
        totalMRP = findViewById(R.id.total_mrp);
        totalDiscount = findViewById(R.id.total_discount);
        couponDiscount = findViewById(R.id.coupon_discount);
        knowMoreBtn = findViewById(R.id.know_more_btn);
        deliveryCharges = findViewById(R.id.delivery_charges);
        totalAmount = findViewById(R.id.total_amount);
        recyclerCart = findViewById(R.id.recycler_cart);
        shopBtnContainer = findViewById(R.id.shop_btn_container);
        shopBtn = findViewById(R.id.shop_btn);
        checkoutBtnContainer = findViewById(R.id.checkout_btn_container);
        checkoutBtn = findViewById(R.id.checkout_btn);
        progressBar = findViewById(R.id.progress_bar);
        totalAmount2 = findViewById(R.id.total_amount2);
        totalDiscount2 = findViewById(R.id.total_discount2);
        pullRefreshLayout = findViewById(R.id.pull_refresh_layout);
    }

    private void initFirebase() {
        cartRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .collection(Constants.KEY_COLLECTION_CART);
    }

    private void setActionOnViews() {
        closeBtn.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });

        progressBar.setVisibility(View.VISIBLE);
    }

    private void loadCartItems() {
        Query query = cartRef.whereEqualTo(Constants.KEY_CART_ITEM_LOCATION, cart_location)
                .orderBy(Constants.KEY_CART_ITEM_TIMESTAMP, Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<CartItem> options = new FirestoreRecyclerOptions.Builder<CartItem>()
                .setLifecycleOwner(CartActivity.this)
                .setQuery(query, CartItem.class)
                .build();

        cartAdapter = new CartAdapter(options);
        cartAdapter.notifyDataSetChanged();

        recyclerCart.setHasFixedSize(true);
        recyclerCart.setLayoutManager(new LinearLayoutManager(CartActivity.this));
        recyclerCart.setAdapter(cartAdapter);

        recyclerCart.getAdapter().notifyDataSetChanged();

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                if(direction == ItemTouchHelper.RIGHT) {
                    if (!isConnectedToInternet(CartActivity.this)) {
                        showConnectToInternetDialog();
                        return;
                    } else {
                        progressDialog1.show();
                        cartAdapter.deleteItem(viewHolder.getAdapterPosition());
                    }
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

                new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        .addBackgroundColor(ContextCompat.getColor(CartActivity.this, R.color.errorColor))
                        .addSwipeRightActionIcon(R.drawable.ic_dialog_remove)
                        .setSwipeRightActionIconTint(getColor(R.color.colorIconLight))
                        .create()
                        .decorate();

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }).attachToRecyclerView(recyclerCart);
    }

    public class CartAdapter extends FirestoreRecyclerAdapter<CartItem, CartAdapter.CartViewHolder> {

        public CartAdapter(@NonNull FirestoreRecyclerOptions<CartItem> options) {
            super(options);
        }

        @NonNull
        @Override
        public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.layout_cart_item, parent, false);
            return new CartViewHolder(itemView);
        }

        @Override
        protected void onBindViewHolder(@NonNull CartViewHolder holder, int position, @NonNull CartItem model) {
            Glide.with(holder.cartProductImage.getContext()).load(model.getCartItemProductImage()).centerCrop().into(holder.cartProductImage);
            holder.cartItemID.setText(model.getCartItemID());
            holder.cartProductName.setText(model.getCartItemProductName());
            holder.cartProductUnit.setText(model.getCartItemProductUnit());
            holder.cartProductCategory.setText(model.getCartItemProductCategory());
            holder.cartProductStoreName.setText(model.getCartItemProductStoreName());
            holder.cartProductPrice.setText(String.format("₹ %s", model.getCartItemProductRetailPrice()));

            if (model.getCartItemProductRetailPrice() == model.getCartItemProductMRP()) {
                holder.cartProductMRP.setVisibility(View.GONE);
                holder.cartProductOffer.setVisibility(View.GONE);
            } else {
                holder.cartProductMRP.setVisibility(View.VISIBLE);
                holder.cartProductOffer.setVisibility(View.VISIBLE);
                shimmer = new Shimmer();
                holder.cartProductMRP.setText(String.format("₹ %s", model.getCartItemProductMRP()));
                holder.cartProductMRP.setPaintFlags(holder.cartProductMRP.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                float offer = (float) (((model.getCartItemProductMRP() - model.getCartItemProductRetailPrice()) / model.getCartItemProductMRP()) * 100);
                String offer_value = ((int) offer) + "% off";
                holder.cartProductOffer.setText(offer_value);
                shimmer.start(holder.cartProductOffer);
            }

            if (model.getCartItemProductQuantity() == 0) {
                deleteItem(holder.getAdapterPosition());
            } else {
                holder.cartProductQuantity.setText(String.valueOf(model.getCartItemProductQuantity()));
                double item_total_price = Math.round((model.getCartItemProductRetailPrice() * model.getCartItemProductQuantity()) * 100.0) / 100.0;
                double item_total_mrp = Math.round((model.getCartItemProductMRP() * model.getCartItemProductQuantity()) * 100.0) / 100.0;

                holder.cartItemTotalPrice.setText(String.format("₹ %s", item_total_price));

                if(item_total_price == item_total_mrp) {
                    holder.cartItemTotalMRP.setVisibility(View.GONE);
                } else {
                    holder.cartItemTotalMRP.setVisibility(View.VISIBLE);
                    holder.cartItemTotalMRP.setText(String.format("₹ %s", item_total_mrp));
                    holder.cartItemTotalMRP.setPaintFlags(holder.cartItemTotalMRP.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                }
            }

            holder.removeBtn.setOnClickListener(v -> {
                if (!isConnectedToInternet(CartActivity.this)) {
                    showConnectToInternetDialog();
                    return;
                } else {
                    MaterialDialog materialDialog = new MaterialDialog.Builder(CartActivity.this)
                            .setTitle("Remove item from cart?")
                            .setMessage("Are you sure of removing this item from your cart?")
                            .setCancelable(false)
                            .setPositiveButton("Remove", R.drawable.ic_dialog_remove, (dialogInterface, which) -> {
                                dialogInterface.dismiss();
                                progressDialog1.show();
                                deleteItem(holder.getAdapterPosition());
                            })
                            .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                    materialDialog.show();
                }
            });

            holder.decrementBtn.setOnClickListener(v -> {
                if (!isConnectedToInternet(CartActivity.this)) {
                    showConnectToInternetDialog();
                    return;
                } else {
                    progressDialog1.show();
                    cartRef.document(model.getCartItemID())
                            .update(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY, model.getCartItemProductQuantity() - 1)
                            .addOnSuccessListener(aVoid -> {
                                if (model.getCartItemProductQuantity() == 0) {
                                    deleteItem(holder.getAdapterPosition());
                                } else {
                                    progressDialog1.dismiss();
                                    holder.cartProductQuantity.setText(String.valueOf(model.getCartItemProductQuantity()));
                                }
                            }).addOnFailureListener(e -> {
                        progressDialog1.dismiss();
                        Alerter.create(CartActivity.this)
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

            holder.incrementBtn.setOnClickListener(v -> {
                if (!isConnectedToInternet(CartActivity.this)) {
                    showConnectToInternetDialog();
                    return;
                } else {
                    progressDialog1.show();
                    cartRef.document(model.getCartItemID())
                            .update(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY, model.getCartItemProductQuantity() + 1)
                            .addOnSuccessListener(aVoid -> {
                                progressDialog1.dismiss();
                                holder.cartProductQuantity.setText(String.valueOf(model.getCartItemProductQuantity()));
                            }).addOnFailureListener(e -> {
                        progressDialog1.dismiss();
                        Alerter.create(CartActivity.this)
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

            holder.clickListener.setOnClickListener(v -> {
                preferenceManager.putString(Constants.KEY_PRODUCT, model.getCartItemProductID());
                startActivity(new Intent(CartActivity.this, ProductDetailsActivity.class));
                CustomIntent.customType(CartActivity.this, "bottom-to-up");
            });
        }

        @Override
        public void onDataChanged() {
            super.onDataChanged();

            pullRefreshLayout.setRefreshing(false);
            progressBar.setVisibility(View.GONE);

            if (getItemCount() == 0) {
                illustrationEmpty.setVisibility(View.VISIBLE);
                textEmpty.setVisibility(View.VISIBLE);
                shopBtnContainer.setVisibility(View.VISIBLE);
                shopBtn.setEnabled(true);
                shopBtn.setOnClickListener(v -> {
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    CustomIntent.customType(CartActivity.this, "up-to-bottom");
                    finish();
                });
                checkoutBtn.setEnabled(false);
                checkoutBtnContainer.setVisibility(View.GONE);
                recyclerCart.setAdapter(null);
                cartAdapter.notifyDataSetChanged();
                nestedScrollView.setVisibility(View.GONE);
            } else {
                illustrationEmpty.setVisibility(View.GONE);
                textEmpty.setVisibility(View.GONE);
                shopBtnContainer.setVisibility(View.GONE);
                shopBtn.setEnabled(false);
                checkoutBtnContainer.setVisibility(View.VISIBLE);
                checkoutBtn.setEnabled(true);
                cartAdapter.notifyDataSetChanged();
                recyclerCart.setAdapter(cartAdapter);
                recyclerCart.getAdapter().notifyDataSetChanged();
                nestedScrollView.setVisibility(View.VISIBLE);

                double total_mrp = 0;
                double total_price = 0;

                for (int i = 0; i < getItemCount(); i++) {
                    total_mrp += Math.round((getItem(i).getCartItemProductMRP() * getItem(i).getCartItemProductQuantity()) * 100.0) / 100.0;
                    total_price += Math.round((getItem(i).getCartItemProductRetailPrice() * getItem(i).getCartItemProductQuantity()) * 100.0) / 100.0;
                }

                double total_discount = Math.round((total_mrp - total_price) * 100.0) / 100.0;
                String total_price_value = String.format("₹ %s", Math.round(total_price * 100.0) / 100.0);

                itemsCount.setText(String.format("(%d Items)", getItemCount()));
                totalMRP.setText(String.format("₹ %s", Math.round(total_mrp * 100.0) / 100.0));
                totalDiscount.setText(String.format("- ₹ %s", total_discount));
                totalAmount.setText(total_price_value);
                totalAmount2.setText(total_price_value);
                totalDiscount2.setText(String.format("Saved ₹ %s", total_discount));

                checkoutBtn.setOnClickListener(v -> {

                });
            }
        }

        @Override
        public void onError(@NonNull FirebaseFirestoreException e) {
            super.onError(e);
            pullRefreshLayout.setRefreshing(false);
            Alerter.create(CartActivity.this)
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

        public void deleteItem(int position) {
            getSnapshots().getSnapshot(position).getReference().delete()
                    .addOnSuccessListener(aVoid -> {
                        progressDialog1.dismiss();
                        Alerter.create(CartActivity.this)
                                .setText("Ahan! One item has been removed from cart.")
                                .setTextAppearance(R.style.AlertText)
                                .setBackgroundColorRes(R.color.infoColor)
                                .setIcon(R.drawable.ic_dialog_okay)
                                .setDuration(3000)
                                .enableIconPulse(true)
                                .enableVibration(true)
                                .disableOutsideTouch()
                                .enableProgress(true)
                                .setProgressColorInt(getColor(android.R.color.white))
                                .show();
                    }).addOnFailureListener(e -> {
                progressDialog1.dismiss();
                Alerter.create(CartActivity.this)
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

        public class CartViewHolder extends RecyclerView.ViewHolder {

            TextView cartItemID, cartProductName, cartProductUnit, cartProductCategory, cartProductStoreName,
                    cartProductPrice, cartProductMRP, cartItemTotalPrice, cartItemTotalMRP, cartProductQuantity;
            ShimmerTextView cartProductOffer;
            ImageView cartProductImage, removeBtn;
            ConstraintLayout clickListener, decrementBtn, incrementBtn;

            public CartViewHolder(@NonNull View itemView) {
                super(itemView);

                clickListener = itemView.findViewById(R.id.click_listener);
                cartItemID = itemView.findViewById(R.id.cart_item_id);
                cartProductImage = itemView.findViewById(R.id.cart_product_image);
                cartProductName = itemView.findViewById(R.id.cart_product_name);
                cartProductUnit = itemView.findViewById(R.id.cart_product_unit);
                cartProductCategory = itemView.findViewById(R.id.cart_product_category);
                cartProductStoreName = itemView.findViewById(R.id.cart_product_store_name);
                cartProductPrice = itemView.findViewById(R.id.cart_product_price);
                cartProductMRP = itemView.findViewById(R.id.cart_product_mrp);
                cartItemTotalPrice = itemView.findViewById(R.id.cart_item_total_price);
                cartItemTotalMRP = itemView.findViewById(R.id.cart_item_total_mrp);
                cartProductOffer = itemView.findViewById(R.id.cart_product_offer);
                cartProductQuantity = itemView.findViewById(R.id.cart_product_quantity);
                removeBtn = itemView.findViewById(R.id.remove_btn);
                decrementBtn = itemView.findViewById(R.id.decrement_btn);
                incrementBtn = itemView.findViewById(R.id.increment_btn);
            }
        }
    }

    private boolean isConnectedToInternet(CartActivity cartActivity) {
        ConnectivityManager connectivityManager = (ConnectivityManager) cartActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if ((wifiConn != null && wifiConn.isConnected()) || (mobileConn != null && mobileConn.isConnected())) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(CartActivity.this)
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

        loadCartItems();

        pullRefreshLayout.setColor(getColor(R.color.colorAccent));
        pullRefreshLayout.setOnRefreshListener(() -> {
            if (!isConnectedToInternet(CartActivity.this)) {
                pullRefreshLayout.setRefreshing(false);
                showConnectToInternetDialog();
                return;
            } else {
                loadCartItems();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        CustomIntent.customType(CartActivity.this, "up-to-bottom");
    }
}