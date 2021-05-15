package com.application.grocertaxi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
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
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.application.grocertaxi.Model.CartItem;
import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;
import com.tomergoldst.tooltips.ToolTip;
import com.tomergoldst.tooltips.ToolTipsManager;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import dmax.dialog.SpotsDialog;
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;
import maes.tech.intentanim.CustomIntent;

public class CartActivity extends AppCompatActivity {

    private ImageView closeBtn, knowMoreBtn1, knowMoreBtn2;
    private TextView noOfItems, coupon, itemsCount, totalMRP, discountAmount, couponDiscount,
            deliveryCharges, subTotal, totalAmount, totalDiscount, readPolicyBtn;
    private RecyclerView recyclerCart;
    private ConstraintLayout layoutContent, layoutEmpty, layoutNoInternet, retryBtn,
            layoutNoConvenienceFee, removeAllBtn, applyCouponBtn, proceedBtn, shopBtn;
    private ProgressBar progressBar;

    private CollectionReference cartRef, userRef, storeRef;
    private CartAdapter cartAdapter;

    private String cart_location;
    private PreferenceManager preferenceManager;
    private AlertDialog progressDialog;

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

        progressDialog = new SpotsDialog.Builder().setContext(CartActivity.this)
                .setMessage("Hold on..")
                .setCancelable(false)
                .setTheme(R.style.SpotsDialog)
                .build();

        ////////////////////////////////////////////////////////////////////////////////////////////

        cart_location = String.format("%s, %s", preferenceManager.getString(Constants.KEY_USER_LOCALITY), preferenceManager.getString(Constants.KEY_USER_CITY));

        ////////////////////////////////////////////////////////////////////////////////////////////

        layoutContent = findViewById(R.id.layout_content);
        layoutEmpty = findViewById(R.id.layout_empty);
        layoutNoInternet = findViewById(R.id.layout_no_internet);
        retryBtn = findViewById(R.id.retry_btn);

        closeBtn = findViewById(R.id.close_btn);
        noOfItems = findViewById(R.id.cart_no_of_items);
        layoutNoConvenienceFee = findViewById(R.id.layout_no_convenience_fee);
        removeAllBtn = findViewById(R.id.remove_all_btn);
        recyclerCart = findViewById(R.id.recycler_cart);
        applyCouponBtn = findViewById(R.id.apply_coupon_btn);
        coupon = findViewById(R.id.coupon);
        itemsCount = findViewById(R.id.items_count);
        totalMRP = findViewById(R.id.total_mrp);
        discountAmount = findViewById(R.id.discount_amount);
        couponDiscount = findViewById(R.id.coupon_discount);
        knowMoreBtn1 = findViewById(R.id.know_more_btn1);
        knowMoreBtn2 = findViewById(R.id.know_more_btn2);
        deliveryCharges = findViewById(R.id.delivery_charges);
        subTotal = findViewById(R.id.sub_total);

        proceedBtn = findViewById(R.id.proceed_btn);
        totalAmount = findViewById(R.id.total_amount);
        totalDiscount = findViewById(R.id.total_discount);

        readPolicyBtn = findViewById(R.id.read_policy_btn);

        shopBtn = findViewById(R.id.shop_btn);

        progressBar = findViewById(R.id.progress_bar);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkNetworkConnection();
    }

    private void checkNetworkConnection() {
        if (!isConnectedToInternet(CartActivity.this)) {
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
        cartRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .collection(Constants.KEY_COLLECTION_CART);

        userRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS);

        storeRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_LOCALITY))
                .collection(Constants.KEY_COLLECTION_STORES);
    }

    private void setActionOnViews() {
        layoutNoInternet.setVisibility(View.GONE);
        layoutContent.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);

        ////////////////////////////////////////////////////////////////////////////////////////////

        closeBtn.setOnClickListener(v -> onBackPressed());

        ////////////////////////////////////////////////////////////////////////////////////////////

        progressBar.setVisibility(View.VISIBLE);
        loadCartItems();
    }

    ///////////////////////////////////// LoadCartItems ////////////////////////////////////////////

    private void loadCartItems() {
        Query query = cartRef.whereEqualTo(Constants.KEY_CART_ITEM_LOCATION, cart_location)
                .orderBy(Constants.KEY_CART_ITEM_TIMESTAMP, Query.Direction.ASCENDING);
        FirestoreRecyclerOptions<CartItem> options = new FirestoreRecyclerOptions.Builder<CartItem>()
                .setLifecycleOwner(CartActivity.this)
                .setQuery(query, CartItem.class)
                .build();

        cartAdapter = new CartAdapter(options);
        cartAdapter.notifyDataSetChanged();

        recyclerCart.setLayoutManager(new LinearLayoutManager(CartActivity.this));
        recyclerCart.getLayoutManager().setAutoMeasureEnabled(true);
        recyclerCart.setNestedScrollingEnabled(false);
        recyclerCart.setHasFixedSize(false);
        recyclerCart.setAdapter(cartAdapter);

        recyclerCart.getAdapter().notifyDataSetChanged();

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.RIGHT) {
                    if (!isConnectedToInternet(CartActivity.this)) {
                        showConnectToInternetDialog();
                        return;
                    } else {
                        progressDialog.show();
                        cartAdapter.deleteItem(viewHolder.getAdapterPosition());
                        recyclerCart.getAdapter().notifyDataSetChanged();
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

    //////////////////////////////////////// CartAdapter ///////////////////////////////////////////

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
            Glide.with(holder.cartProductImage.getContext()).load(model.getCartItemProductImage())
                    .placeholder(R.drawable.thumbnail).centerCrop().into(holder.cartProductImage);

            ////////////////////////////////////////////////////////////////////////////////////////

            holder.cartProductName.setText(model.getCartItemProductName());
            holder.cartProductUnit.setText(model.getCartItemProductUnit());
            holder.cartProductCategory.setText(model.getCartItemProductCategory());
            holder.cartProductStoreName.setText(model.getCartItemProductStoreName());

            ////////////////////////////////////////////////////////////////////////////////////////

            if (model.getCartItemProductOffer() == 0) {
                holder.cartProductOffer.setVisibility(View.GONE);
            } else {
                Shimmer shimmer = new Shimmer();
                holder.cartProductOffer.setVisibility(View.VISIBLE);
                holder.cartProductOffer.setText(String.format("%d%% OFF", model.getCartItemProductOffer()));
                shimmer.start(holder.cartProductOffer);
            }

            ////////////////////////////////////////////////////////////////////////////////////

            storeRef.document(model.getCartItemProductStoreID()).collection(Constants.KEY_COLLECTION_PRODUCTS)
                    .document(model.getCartItemProductID())
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (error != null) {
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
                        } else {
                            if (documentSnapshot.exists()) {
                                boolean status = documentSnapshot.getBoolean(Constants.KEY_PRODUCT_IN_STOCK);
                                long count = documentSnapshot.getLong(Constants.KEY_PRODUCT_UNITS_IN_STOCK);

                                if (status) {
                                    holder.cartProductImage.clearColorFilter();
                                    holder.cartProductOffer.setVisibility(View.VISIBLE);

                                    holder.cartProductStatus.setText("In Stock");
                                    holder.cartProductStatus.setTextColor(getColor(R.color.successColor));

                                    holder.cartProductUnitsInStock.setVisibility(View.VISIBLE);
                                    if (count >= 1 && count <= 5) {
                                        holder.cartProductUnitsInStock.setText(String.format("(Hurry! Only %d left)", count));
                                    } else if (count > 5) {
                                        holder.cartProductUnitsInStock.setText(String.format("(%d units left)", count));
                                    }

                                    //Decrement Quantity
                                    holder.decrementBtnContainer.setCardBackgroundColor(getColor(R.color.colorAccent));
                                    holder.decrementBtn.setEnabled(true);
                                    holder.decrementBtn.setOnClickListener(v -> {
                                        if (!isConnectedToInternet(CartActivity.this)) {
                                            showConnectToInternetDialog();
                                            return;
                                        } else {
                                            progressDialog.show();
                                            cartRef.document(model.getCartItemID())
                                                    .update(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY, model.getCartItemProductQuantity() - 1)
                                                    .addOnSuccessListener(aVoid -> {
                                                        if (model.getCartItemProductQuantity() == 0) {
                                                            deleteItem(holder.getAdapterPosition());
                                                        } else {
                                                            progressDialog.dismiss();
                                                            holder.cartProductQuantity.setText(String.valueOf(model.getCartItemProductQuantity()));
                                                        }
                                                    }).addOnFailureListener(e -> {
                                                progressDialog.dismiss();
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
                                    });

                                    //Increment Quantity
                                    holder.incrementBtnContainer.setCardBackgroundColor(getColor(R.color.colorAccent));
                                    holder.incrementBtn.setEnabled(true);
                                    holder.incrementBtn.setOnClickListener(v -> {
                                        if (!isConnectedToInternet(CartActivity.this)) {
                                            showConnectToInternetDialog();
                                            return;
                                        } else {
                                            progressDialog.show();
                                            if (model.getCartItemProductQuantity() <= count) {
                                                cartRef.document(model.getCartItemID())
                                                        .update(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY, model.getCartItemProductQuantity() + 1)
                                                        .addOnSuccessListener(aVoid -> {
                                                            progressDialog.dismiss();
                                                            holder.cartProductQuantity.setText(String.valueOf(model.getCartItemProductQuantity()));
                                                        }).addOnFailureListener(e -> {
                                                    progressDialog.dismiss();
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
                                            } else {
                                                progressDialog.dismiss();
                                                MaterialDialog materialDialog = new MaterialDialog.Builder(CartActivity.this)
                                                        .setTitle("Item can't be further added to your cart!")
                                                        .setMessage("The store doesn't have more of this product than the quantity you already have in your cart.")
                                                        .setCancelable(false)
                                                        .setPositiveButton("Okay", R.drawable.ic_dialog_okay, (dialogInterface, which) -> dialogInterface.dismiss())
                                                        .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                                                materialDialog.show();
                                            }
                                        }
                                    });

                                    //Checking if item quantity is more than what is there in stock
                                    if (model.getCartItemProductQuantity() > count) {
                                        proceedBtn.setOnClickListener(v -> {
                                            MaterialDialog materialDialog = new MaterialDialog.Builder(CartActivity.this)
                                                    .setTitle("Can't proceed!")
                                                    .setMessage("You can't proceed with item quantity more than what the store has right now in its stock.")
                                                    .setCancelable(false)
                                                    .setPositiveButton("Okay", R.drawable.ic_dialog_okay, (dialogInterface, which) -> dialogInterface.dismiss())
                                                    .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                                            materialDialog.show();
                                            return;
                                        });
                                    }
                                } else {
                                    ColorMatrix matrix = new ColorMatrix();
                                    matrix.setSaturation(0);

                                    ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
                                    holder.cartProductImage.setColorFilter(filter);

                                    holder.cartProductOffer.setVisibility(View.GONE);

                                    holder.cartProductStatus.setText("Out of Stock");
                                    holder.cartProductStatus.setTextColor(getColor(R.color.errorColor));
                                    holder.cartProductUnitsInStock.setVisibility(View.GONE);

                                    holder.decrementBtnContainer.setCardBackgroundColor(getColor(R.color.colorInactive));
                                    holder.decrementBtn.setEnabled(false);
                                    holder.incrementBtnContainer.setCardBackgroundColor(getColor(R.color.colorInactive));
                                    holder.incrementBtn.setEnabled(false);

                                    proceedBtn.setOnClickListener(v -> {
                                        MaterialDialog materialDialog = new MaterialDialog.Builder(CartActivity.this)
                                                .setTitle("Can't proceed!")
                                                .setMessage("There are items in your cart that are out of stock right now. Remove them first to proceed.")
                                                .setCancelable(false)
                                                .setPositiveButton("Okay", R.drawable.ic_dialog_okay, (dialogInterface, which) -> dialogInterface.dismiss())
                                                .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                                        materialDialog.show();
                                        return;
                                    });
                                }
                            } else {
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
                        }
                    });

            ////////////////////////////////////////////////////////////////////////////////////////

            if (model.getCartItemProductQuantity() == 0) {
                deleteItem(holder.getAdapterPosition());
            } else {
                holder.cartProductQuantity.setText(String.valueOf(model.getCartItemProductQuantity()));
                double item_total_price = Math.round((model.getCartItemProductRetailPrice() * model.getCartItemProductQuantity()) * 100.0) / 100.0;
                double item_total_mrp = Math.round((model.getCartItemProductMRP() * model.getCartItemProductQuantity()) * 100.0) / 100.0;

                holder.cartItemTotalPrice.setText(String.format("₹ %s", item_total_price));

                if (item_total_price == item_total_mrp) {
                    holder.cartItemTotalMRP.setVisibility(View.GONE);
                } else {
                    holder.cartItemTotalMRP.setVisibility(View.VISIBLE);
                    holder.cartItemTotalMRP.setText(String.format("₹ %s", item_total_mrp));
                    holder.cartItemTotalMRP.setPaintFlags(holder.cartItemTotalMRP.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                }
            }

            ////////////////////////////////////////////////////////////////////////////////////////

            holder.clickListener.setOnClickListener(v -> {
                preferenceManager.putString(Constants.KEY_PRODUCT, model.getCartItemProductID());
                startActivity(new Intent(CartActivity.this, ProductDetailsActivity.class));
                CustomIntent.customType(CartActivity.this, "bottom-to-up");
            });
        }

        @Override
        public void onDataChanged() {
            super.onDataChanged();

            progressBar.setVisibility(View.GONE);

            if (getItemCount() == 0) {
                layoutContent.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
                shopBtn.setOnClickListener(v -> {
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    CustomIntent.customType(CartActivity.this, "up-to-bottom");
                    finish();
                });

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
            } else {
                layoutContent.setVisibility(View.VISIBLE);
                layoutEmpty.setVisibility(View.GONE);

                cartAdapter.notifyDataSetChanged();
                recyclerCart.setAdapter(cartAdapter);
                recyclerCart.getAdapter().notifyDataSetChanged();

                noOfItems.setVisibility(View.VISIBLE);
                if (getItemCount() == 1) {
                    noOfItems.setText(String.format("%d Item", getItemCount()));
                    itemsCount.setText(String.format("(%d Item)", getItemCount()));
                } else {
                    noOfItems.setText(String.format("%d Items", getItemCount()));
                    itemsCount.setText(String.format("(%d Items)", getItemCount()));
                }

                //////////////////////////////////// Remove All ////////////////////////////////////

                removeAllBtn.setOnClickListener(v -> {
                    if (!isConnectedToInternet(CartActivity.this)) {
                        showConnectToInternetDialog();
                        return;
                    } else {
                        MaterialDialog materialDialog = new MaterialDialog.Builder(CartActivity.this)
                                .setTitle("Empty your cart?")
                                .setMessage("Are you sure of removing all the items from your cart?")
                                .setCancelable(false)
                                .setPositiveButton("Remove", R.drawable.ic_dialog_remove, (dialogInterface, which) -> {
                                    dialogInterface.dismiss();
                                    progressDialog.show();
                                    for (int i = 0; i < getItemCount(); i++) {
                                        getSnapshots().getSnapshot(i).getReference().delete()
                                                .addOnSuccessListener(aVoid -> {
                                                    progressDialog.dismiss();
                                                }).addOnFailureListener(e -> {
                                            progressDialog.dismiss();
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
                                })
                                .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                        materialDialog.show();
                    }
                });

                ////////////////////////////////////////////////////////////////////////////////////

                double total_mrp = 0;
                double total_price = 0;

                for (int i = 0; i < getItemCount(); i++) {
                    total_mrp += Math.round((getItem(i).getCartItemProductMRP() * getItem(i).getCartItemProductQuantity()) * 100.0) / 100.0;
                    total_price += Math.round((getItem(i).getCartItemProductRetailPrice() * getItem(i).getCartItemProductQuantity()) * 100.0) / 100.0;
                }
                double discount_amount = Math.round((total_mrp - total_price) * 100.0) / 100.0;

                preferenceManager.putString(Constants.KEY_ORDER_NO_OF_ITEMS, String.valueOf(getItemCount()));
                preferenceManager.putString(Constants.KEY_ORDER_TOTAL_MRP, String.valueOf(total_mrp));
                preferenceManager.putString(Constants.KEY_ORDER_TOTAL_RETAIL_PRICE, String.valueOf(total_price));
                preferenceManager.putString(Constants.KEY_ORDER_TOTAL_DISCOUNT, String.valueOf(discount_amount));

                totalMRP.setText(String.format("₹ %s", Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TOTAL_MRP))));
                discountAmount.setText(String.format("₹ %s", Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TOTAL_DISCOUNT))));

                /////////////////////////////////// Apply Coupon ///////////////////////////////////

                applyCouponBtn.setOnClickListener(v -> {
                    startActivity(new Intent(CartActivity.this, CouponsListActivity.class));
                    CustomIntent.customType(CartActivity.this, "bottom-to-up");
                });

                if (preferenceManager.getString(Constants.KEY_COUPON).isEmpty() ||
                        preferenceManager.getString(Constants.KEY_COUPON).equals("")) {
                    coupon.setText("Apply Coupon");
                    couponDiscount.setText("No coupon applied");
                    couponDiscount.setTextColor(getColor(R.color.colorTextDark));

                    preferenceManager.putString(Constants.KEY_ORDER_COUPON_APPLIED, "");
                    preferenceManager.putString(Constants.KEY_ORDER_COUPON_DISCOUNT, String.valueOf(0));

                    totalDiscount.setText(String.format("Saved ₹ %s", Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TOTAL_DISCOUNT))
                            + Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_COUPON_DISCOUNT))));
                } else {
                    coupon.setText(preferenceManager.getString(Constants.KEY_COUPON));
                    double coupon_discount = Long.parseLong(preferenceManager.getString(Constants.KEY_COUPON_DISCOUNT_PERCENT)) * 0.01
                            * Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TOTAL_RETAIL_PRICE));
                    couponDiscount.setText("₹ " + coupon_discount);
                    couponDiscount.setTextColor(getColor(R.color.successColor));

                    preferenceManager.putString(Constants.KEY_ORDER_COUPON_APPLIED, preferenceManager.getString(Constants.KEY_COUPON));
                    preferenceManager.putString(Constants.KEY_ORDER_COUPON_DISCOUNT, String.valueOf(coupon_discount));

                    totalDiscount.setText(String.format("Saved ₹ %s", Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TOTAL_DISCOUNT))
                            + Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_COUPON_DISCOUNT))));
                }

                ////////////////////////////////////////////////////////////////////////////////////

                userRef.document(preferenceManager.getString(Constants.KEY_USER_ID))
                        .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot documentSnapshot = task.getResult();
                        if (documentSnapshot.exists()) {
                            boolean first_order = documentSnapshot.getBoolean(Constants.KEY_USER_FIRST_ORDER);

                            if (first_order) {
                                layoutNoConvenienceFee.setVisibility(View.VISIBLE);

                                knowMoreBtn1.setOnClickListener(v -> {
                                    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(CartActivity.this);
                                    bottomSheetDialog.setContentView(R.layout.bottom_sheet_info3);
                                    bottomSheetDialog.setCanceledOnTouchOutside(false);

                                    ImageView closeSheetBtn = bottomSheetDialog.findViewById(R.id.close_bottom_sheet_btn);
                                    closeSheetBtn.setOnClickListener(v12 -> bottomSheetDialog.dismiss());

                                    bottomSheetDialog.show();
                                });

                                preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_CHARGES, String.valueOf(0));
                                deliveryCharges.setText("FREE");
                                deliveryCharges.setTextColor(getColor(R.color.successColor));

                                double sub_total = Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TOTAL_RETAIL_PRICE)) -
                                        Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_COUPON_DISCOUNT)) +
                                        Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_DELIVERY_CHARGES));
                                preferenceManager.putString(Constants.KEY_ORDER_SUB_TOTAL, String.valueOf(sub_total));

                                subTotal.setText(String.format("₹ %s", Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_SUB_TOTAL))));
                                totalAmount.setText(String.format("₹ %s", Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_SUB_TOTAL))));
                            } else {
                                layoutNoConvenienceFee.setVisibility(View.GONE);

                                if (Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TOTAL_RETAIL_PRICE)) > 0 &&
                                        Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TOTAL_RETAIL_PRICE)) <= 500) {
                                    preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_CHARGES, String.valueOf(40));
                                    deliveryCharges.setText(String.format("₹ %s", Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_DELIVERY_CHARGES))));
                                    deliveryCharges.setTextColor(getColor(R.color.errorColor));

                                    double sub_total = Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TOTAL_RETAIL_PRICE)) -
                                            Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_COUPON_DISCOUNT)) +
                                            Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_DELIVERY_CHARGES));
                                    preferenceManager.putString(Constants.KEY_ORDER_SUB_TOTAL, String.valueOf(sub_total));

                                    subTotal.setText(String.format("₹ %s", Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_SUB_TOTAL))));
                                    totalAmount.setText(String.format("₹ %s", Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_SUB_TOTAL))));
                                } else if (Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TOTAL_RETAIL_PRICE)) > 500) {
                                    preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_CHARGES, String.valueOf(0));
                                    deliveryCharges.setText("FREE");
                                    deliveryCharges.setTextColor(getColor(R.color.successColor));

                                    double sub_total = Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TOTAL_RETAIL_PRICE)) -
                                            Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_COUPON_DISCOUNT)) +
                                            Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_DELIVERY_CHARGES));
                                    preferenceManager.putString(Constants.KEY_ORDER_SUB_TOTAL, String.valueOf(sub_total));

                                    subTotal.setText(String.format("₹ %s", Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_SUB_TOTAL))));
                                    totalAmount.setText(String.format("₹ %s", Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_SUB_TOTAL))));
                                }
                            }
                        } else {
                            Alerter.create(CartActivity.this)
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
                    } else {
                        Alerter.create(CartActivity.this)
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

                ////////////////////////////////////////////////////////////////////////////////////

                knowMoreBtn2.setOnClickListener(v -> {
                    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(CartActivity.this);
                    bottomSheetDialog.setContentView(R.layout.bottom_sheet_info2);
                    bottomSheetDialog.setCanceledOnTouchOutside(false);

                    ImageView closeSheetBtn = bottomSheetDialog.findViewById(R.id.close_bottom_sheet_btn);
                    closeSheetBtn.setOnClickListener(v12 -> bottomSheetDialog.dismiss());

                    bottomSheetDialog.show();
                });

                ////////////////////////////////////////////////////////////////////////////////////

                readPolicyBtn.setOnClickListener(v -> {
                    String privacyPolicyUrl = "https://grocertaxi.wixsite.com/refund-policy";
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl));
                    startActivity(browserIntent);
                });

                ////////////////////////////////////////////////////////////////////////////////////

                proceedBtn.setOnClickListener(v -> {
                    if (!isConnectedToInternet(CartActivity.this)) {
                        showConnectToInternetDialog();
                        return;
                    } else {
                        progressDialog.show();

                        storeRef.document(getItem(0).getCartItemProductStoreID()).get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot documentSnapshot = task.getResult();
                                        if (documentSnapshot.exists()) {
                                            progressDialog.dismiss();
                                            if (Double.parseDouble(preferenceManager.getString(Constants.KEY_ORDER_TOTAL_RETAIL_PRICE))
                                                    >= documentSnapshot.getDouble(Constants.KEY_STORE_MINIMUM_ORDER_VALUE)) {

                                                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(CartActivity.this);
                                                bottomSheetDialog.setContentView(R.layout.bottom_sheet_tip);
                                                bottomSheetDialog.setCanceledOnTouchOutside(false);

                                                ConstraintLayout container = bottomSheetDialog.findViewById(R.id.bottom_sheet_tip);
                                                ChipGroup tipChipGroup = bottomSheetDialog.findViewById(R.id.tip_chip_group);
                                                ImageView tipInfoBtn = bottomSheetDialog.findViewById(R.id.tip_info_btn);
                                                ConstraintLayout checkoutBtn = bottomSheetDialog.findViewById(R.id.checkout_btn);

                                                ToolTipsManager toolTipsManager = new ToolTipsManager();

                                                tipInfoBtn.setOnClickListener(v12 -> {
                                                    ToolTip.Builder builder = new ToolTip.Builder(
                                                            bottomSheetDialog.getContext(), tipInfoBtn, container, "This amount goes into your\ndelivery superhero salary", ToolTip.POSITION_LEFT_TO
                                                    );
                                                    builder.setBackgroundColor(Color.parseColor("#B3000000"));
                                                    builder.setTextAppearance(R.style.TooltipTextAppearance);

                                                    toolTipsManager.show(builder.build());
                                                });

                                                checkoutBtn.setOnClickListener(v1 -> {
                                                    if (tipChipGroup.getCheckedChipId() == R.id.tip10_chip) {
                                                        preferenceManager.putString(Constants.KEY_ORDER_TIP_AMOUNT, String.valueOf(10));
                                                    } else if (tipChipGroup.getCheckedChipId() == R.id.tip20_chip) {
                                                        preferenceManager.putString(Constants.KEY_ORDER_TIP_AMOUNT, String.valueOf(20));
                                                    } else if (tipChipGroup.getCheckedChipId() == R.id.tip30_chip) {
                                                        preferenceManager.putString(Constants.KEY_ORDER_TIP_AMOUNT, String.valueOf(30));
                                                    } else if (!tipChipGroup.isSelected()) {
                                                        preferenceManager.putString(Constants.KEY_ORDER_TIP_AMOUNT, String.valueOf(0));
                                                    }

                                                    if (!isConnectedToInternet(CartActivity.this)) {
                                                        showConnectToInternetDialog();
                                                        return;
                                                    } else {
                                                        bottomSheetDialog.dismiss();
                                                        preferenceManager.putString(Constants.KEY_ORDER_FROM_STOREID, documentSnapshot.getString(Constants.KEY_STORE_ID));
                                                        preferenceManager.putString(Constants.KEY_ORDER_FROM_STORENAME, documentSnapshot.getString(Constants.KEY_STORE_NAME));
                                                        startActivity(new Intent(CartActivity.this, OrderAddressActivity.class));
                                                        CustomIntent.customType(CartActivity.this, "left-to-right");
                                                        finish();
                                                    }
                                                });

                                                bottomSheetDialog.show();
                                            } else {
                                                MaterialDialog materialDialog = new MaterialDialog.Builder(CartActivity.this)
                                                        .setTitle("Ahan! Can't proceed!")
                                                        .setMessage("A minimum order value of ₹ " + documentSnapshot.getDouble(Constants.KEY_STORE_MINIMUM_ORDER_VALUE) + " (excluding delivery charges) is required to make this order eligible for home delivery.")
                                                        .setCancelable(false)
                                                        .setPositiveButton("Okay", R.drawable.ic_dialog_okay, (dialogInterface, which) -> {
                                                            dialogInterface.dismiss();
                                                        })
                                                        .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                                                materialDialog.show();
                                            }
                                        } else {
                                            progressDialog.dismiss();
                                            Alerter.create(CartActivity.this)
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
                                    } else {
                                        progressDialog.dismiss();
                                        Alerter.create(CartActivity.this)
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
                    }
                });
            }
        }

        @Override
        public void onError(@NonNull FirebaseFirestoreException e) {
            super.onError(e);
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
                        progressDialog.dismiss();
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
                progressDialog.dismiss();
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

            TextView cartProductName, cartProductUnit, cartProductCategory, cartProductStoreName,
                    cartProductStatus, cartProductUnitsInStock, cartItemTotalPrice, cartItemTotalMRP, cartProductQuantity;
            ShimmerTextView cartProductOffer;
            ImageView cartProductImage;
            CardView decrementBtnContainer, incrementBtnContainer;
            ConstraintLayout clickListener, decrementBtn, incrementBtn;

            public CartViewHolder(@NonNull View itemView) {
                super(itemView);

                clickListener = itemView.findViewById(R.id.click_listener);
                cartProductImage = itemView.findViewById(R.id.cart_product_image);
                cartProductName = itemView.findViewById(R.id.cart_product_name);
                cartProductUnit = itemView.findViewById(R.id.cart_product_unit);
                cartProductCategory = itemView.findViewById(R.id.cart_product_category);
                cartProductStoreName = itemView.findViewById(R.id.cart_product_store_name);
                cartProductStatus = itemView.findViewById(R.id.cart_product_status);
                cartProductUnitsInStock = itemView.findViewById(R.id.cart_product_unit_in_stock);
                cartItemTotalPrice = itemView.findViewById(R.id.cart_item_total_price);
                cartItemTotalMRP = itemView.findViewById(R.id.cart_item_total_mrp);
                cartProductOffer = itemView.findViewById(R.id.cart_product_offer);
                cartProductQuantity = itemView.findViewById(R.id.cart_product_quantity);
                decrementBtn = itemView.findViewById(R.id.decrement_btn);
                decrementBtnContainer = itemView.findViewById(R.id.decrement_btn_container);
                incrementBtn = itemView.findViewById(R.id.increment_btn);
                incrementBtnContainer = itemView.findViewById(R.id.increment_btn_container);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(CartActivity cartActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) cartActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
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
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        CustomIntent.customType(CartActivity.this, "up-to-bottom");
    }
}