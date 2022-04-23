package com.application.grocertaxi;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.application.grocertaxi.Helper.LoadingDialog;
import com.application.grocertaxi.Model.OrderItem;
import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.makeramen.roundedimageview.RoundedImageView;
import com.tapadoo.alerter.Alerter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;

import dev.shreyaspatil.MaterialDialog.MaterialDialog;
import maes.tech.intentanim.CustomIntent;

public class TrackOrderActivity extends AppCompatActivity {

    private ImageView closeBtn;
    private TextView orderID, orderStoreName, orderStatusPending, customerName, customerAddress, orderStatusCompleted,
            orderNoOfItems, totalMRP, discountAmount, couponDiscount, deliveryCharges, textTipAdded, tipAmount, convenienceFee, totalPayable,
            orderID2, paymentMethod, orderPlacedTime, textDeliveredOn, orderCompletionTime, customerMobile, customerAddress2, instructions;
    private RecyclerView recyclerOrderItems;
    private CardView requestCancellationBtnContainer;
    private ConstraintLayout layoutContent, layoutNoInternet, retryBtn, requestCancellationBtn;
    private ProgressBar progressBar;

    private CollectionReference userOrdersRef;
    private FirestoreRecyclerAdapter<OrderItem, OrderItemViewHolder> orderItemAdapter;

    private PreferenceManager preferenceManager;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_order);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(TrackOrderActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(TrackOrderActivity.this, "fadein-to-fadeout");
            finish();
        } else if (preferenceManager.getString(Constants.KEY_USER_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_CITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).isEmpty()) {
            Intent intent = new Intent(TrackOrderActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(TrackOrderActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        loadingDialog = new LoadingDialog(TrackOrderActivity.this);

        ////////////////////////////////////////////////////////////////////////////////////////////

        layoutContent = findViewById(R.id.layout_content);
        layoutNoInternet = findViewById(R.id.layout_no_internet);
        retryBtn = findViewById(R.id.retry_btn);

        closeBtn = findViewById(R.id.close_btn);
        orderID = findViewById(R.id.order_id);
        orderStoreName = findViewById(R.id.order_store_name);
        orderStatusPending = findViewById(R.id.order_status_pending);
        customerName = findViewById(R.id.customer_name);
        customerAddress = findViewById(R.id.customer_address);
        orderStatusCompleted = findViewById(R.id.order_status_completed);

        orderNoOfItems = findViewById(R.id.no_of_items);
        recyclerOrderItems = findViewById(R.id.recycler_order_items);
        totalMRP = findViewById(R.id.total_mrp);
        discountAmount = findViewById(R.id.discount_amount);
        couponDiscount = findViewById(R.id.coupon_discount);
        deliveryCharges = findViewById(R.id.delivery_charges);
        textTipAdded = findViewById(R.id.text_tip_added);
        tipAmount = findViewById(R.id.tip_amount);
        convenienceFee = findViewById(R.id.convenience_fee);
        totalPayable = findViewById(R.id.total_payable);

        orderID2 = findViewById(R.id.order_id2);
        paymentMethod = findViewById(R.id.payment_method);
        orderPlacedTime = findViewById(R.id.order_placing_time);
        textDeliveredOn = findViewById(R.id.text_delivered_on);
        orderCompletionTime = findViewById(R.id.order_completed_time);
        customerMobile = findViewById(R.id.customer_mobile);
        customerAddress2 = findViewById(R.id.customer_address2);
        instructions = findViewById(R.id.instructions);

        requestCancellationBtnContainer = findViewById(R.id.request_cancellation_btn_container);
        requestCancellationBtn = findViewById(R.id.request_cancellation_btn);
        progressBar = findViewById(R.id.progress_bar);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkNetworkConnection();
    }

    private void checkNetworkConnection() {
        if (!isConnectedToInternet(TrackOrderActivity.this)) {
            layoutContent.setVisibility(View.GONE);
            layoutNoInternet.setVisibility(View.VISIBLE);
            retryBtn.setOnClickListener(v -> checkNetworkConnection());
        } else {
            initFirebase();
            setActionOnViews();
        }
    }

    private void initFirebase() {
        if (preferenceManager.getString(Constants.KEY_ORDER_TYPE).equals("Pending")) {
            userOrdersRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                    .document(preferenceManager.getString(Constants.KEY_USER_ID))
                    .collection(Constants.KEY_COLLECTION_PENDING_ORDERS);
        } else if (preferenceManager.getString(Constants.KEY_ORDER_TYPE).equals("Completed")) {
            userOrdersRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                    .document(preferenceManager.getString(Constants.KEY_USER_ID))
                    .collection(Constants.KEY_COLLECTION_COMPLETED_ORDERS);
        } else if (preferenceManager.getString(Constants.KEY_ORDER_TYPE).equals("Cancelled")) {
            userOrdersRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                    .document(preferenceManager.getString(Constants.KEY_USER_ID))
                    .collection(Constants.KEY_COLLECTION_CANCELLED_ORDERS);
        }
    }

    private void setActionOnViews() {
        layoutNoInternet.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);

        ////////////////////////////////////////////////////////////////////////////////////////////

        closeBtn.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        progressBar.setVisibility(View.VISIBLE);
        loadOrderDetails();
        loadOrderItems();
    }

    ////////////////////////////////////// LoadOrderDetails ////////////////////////////////////////

    private void loadOrderDetails() {
        final DocumentReference orderDocumentRef = userOrdersRef.document(preferenceManager.getString(Constants.KEY_ORDER));

        orderDocumentRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
                progressBar.setVisibility(View.GONE);
                Alerter.create(TrackOrderActivity.this)
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
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    /////////////////////////////// Order ID ///////////////////////////////////

                    orderID.setText(preferenceManager.getString(Constants.KEY_ORDER));
                    orderID2.setText(preferenceManager.getString(Constants.KEY_ORDER));

                    ///////////////////////////// Names & Address //////////////////////////////

                    String order_store_name = documentSnapshot.getString(Constants.KEY_ORDER_FROM_STORENAME);
                    String customer_name = documentSnapshot.getString(Constants.KEY_ORDER_CUSTOMER_NAME);
                    String customer_address = documentSnapshot.getString(Constants.KEY_ORDER_DELIVERY_ADDRESS);

                    orderStoreName.setText(order_store_name);
                    customerName.setText(customer_name);
                    customerAddress.setText(customer_address);
                    customerAddress2.setText(customer_address);

                    ///////////////////////// Order Status & Time //////////////////////////////

                    String order_status = documentSnapshot.getString(Constants.KEY_ORDER_STATUS);
                    String order_placing_time = documentSnapshot.getString(Constants.KEY_ORDER_PLACED_TIME);
                    String order_completion_time = documentSnapshot.getString(Constants.KEY_ORDER_COMPLETION_TIME);
                    String order_cancellation_time = documentSnapshot.getString(Constants.KEY_ORDER_CANCELLATION_TIME);

                    if (preferenceManager.getString(Constants.KEY_ORDER_TYPE).equals("Pending")) {
                        orderStatusPending.setVisibility(View.VISIBLE);
                        orderStatusPending.setText(order_status);
                        orderStatusPending.setTextColor(getColor(R.color.processingColor));
                        orderStatusCompleted.setVisibility(View.INVISIBLE);

                        orderPlacedTime.setText(order_placing_time);
                        textDeliveredOn.setText("Delivered on");
                        orderCompletionTime.setText("Order isn't delivered yet.");
                    } else if (preferenceManager.getString(Constants.KEY_ORDER_TYPE).equals("Completed")) {
                        orderStatusPending.setVisibility(View.INVISIBLE);
                        orderStatusCompleted.setVisibility(View.VISIBLE);
                        orderStatusCompleted.setText("Delivered");
                        orderStatusCompleted.setTextColor(getColor(R.color.successColor));

                        orderPlacedTime.setText(order_placing_time);
                        textDeliveredOn.setText("Delivered on");
                        orderCompletionTime.setText(order_completion_time);
                    } else if (preferenceManager.getString(Constants.KEY_ORDER_TYPE).equals("Cancelled")) {
                        orderStatusPending.setVisibility(View.VISIBLE);
                        orderStatusPending.setText("Cancelled");
                        orderStatusPending.setTextColor(getColor(R.color.errorColor));
                        orderStatusCompleted.setVisibility(View.INVISIBLE);

                        orderPlacedTime.setText(order_placing_time);
                        textDeliveredOn.setText("Cancelled on");
                        orderCompletionTime.setText(order_cancellation_time);
                    }

                    ///////////////////////////// Order Summary ////////////////////////////////

                    long no_of_items = documentSnapshot.getLong(Constants.KEY_ORDER_NO_OF_ITEMS);
                    double total_mrp = documentSnapshot.getDouble(Constants.KEY_ORDER_TOTAL_MRP);
                    double discount_amount = documentSnapshot.getDouble(Constants.KEY_ORDER_TOTAL_DISCOUNT);
                    double coupon_discount = documentSnapshot.getDouble(Constants.KEY_ORDER_COUPON_DISCOUNT);
                    double delivery_charges = documentSnapshot.getDouble(Constants.KEY_ORDER_DELIVERY_CHARGES);
                    double tip_amount = documentSnapshot.getDouble(Constants.KEY_ORDER_TIP_AMOUNT);
                    double convenience_fee = documentSnapshot.getDouble(Constants.KEY_ORDER_CONVENIENCE_FEE);
                    double total_payable = documentSnapshot.getDouble(Constants.KEY_ORDER_TOTAL_PAYABLE);

                    if (no_of_items == 1) {
                        orderNoOfItems.setText(String.format("(%d Item)", no_of_items));
                    } else {
                        orderNoOfItems.setText(String.format("(%d Items)", no_of_items));
                    }

                    totalMRP.setText(String.format("₹ %s", total_mrp));
                    discountAmount.setText(String.format("₹ %s", discount_amount));

                    if (coupon_discount == 0) {
                        couponDiscount.setText("No coupon applied");
                        couponDiscount.setTextColor(getColor(R.color.colorTextDark));
                    } else {
                        couponDiscount.setText(String.format("₹ %s", coupon_discount));
                        couponDiscount.setTextColor(getColor(R.color.successColor));
                    }

                    if (delivery_charges == 0) {
                        deliveryCharges.setText("FREE");
                        deliveryCharges.setTextColor(getColor(R.color.successColor));
                    } else {
                        deliveryCharges.setText(String.format("₹ %s", delivery_charges));
                        deliveryCharges.setTextColor(getColor(R.color.errorColor));
                    }

                    if (tip_amount == 0) {
                        textTipAdded.setVisibility(View.GONE);
                        tipAmount.setVisibility(View.GONE);
                    } else {
                        textTipAdded.setVisibility(View.VISIBLE);
                        tipAmount.setVisibility(View.VISIBLE);
                        tipAmount.setText(String.format("+ ₹ %s", tip_amount));
                    }

                    if (convenience_fee == 0) {
                        convenienceFee.setText("FREE");
                        convenienceFee.setTextColor(getColor(R.color.successColor));
                    } else {
                        convenienceFee.setText(String.format("+ ₹ %s", convenience_fee));
                        convenienceFee.setTextColor(getColor(R.color.errorColor));
                    }

                    totalPayable.setText(String.format("₹ %s", total_payable));

                    ////////////////////////////// Order Details ///////////////////////////////////

                    String payment_method = documentSnapshot.getString(Constants.KEY_ORDER_PAYMENT_MODE);
                    String customer_mobile = documentSnapshot.getString(Constants.KEY_ORDER_CUSTOMER_MOBILE);
                    String instruction = documentSnapshot.getString(Constants.KEY_ORDER_INSTRUCTIONS);

                    paymentMethod.setText(payment_method);
                    customerMobile.setText(customer_mobile);

                    if (instruction.equals("") || instruction.isEmpty()) {
                        instructions.setText("No instructions");
                    } else {
                        instructions.setText(instruction);
                    }

                    ///////////////////////// Request Cancellation Button //////////////////////////

                    if (preferenceManager.getString(Constants.KEY_ORDER_TYPE).equals("Pending")) {
                        requestCancellationBtnContainer.setCardBackgroundColor(getColor(R.color.errorColor));
                        requestCancellationBtn.setEnabled(true);
                        requestCancellationBtn.setOnClickListener(v -> {
                            if (!isConnectedToInternet(TrackOrderActivity.this)) {
                                showConnectToInternetDialog();
                                return;
                            } else {
                                if (order_status.equals("Out for Delivery")) {
                                    loadingDialog.dismissDialog();
                                    MaterialDialog materialDialog = new MaterialDialog.Builder(TrackOrderActivity.this)
                                            .setTitle("Can't proceed!")
                                            .setMessage("This order can't be cancelled since it is already out for delivery already.")
                                            .setCancelable(false)
                                            .setPositiveButton("Okay", R.drawable.ic_dialog_okay, (dialogInterface1, which1) -> dialogInterface1.dismiss())
                                            .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface1, which1) -> dialogInterface1.dismiss()).build();
                                    materialDialog.show();
                                } else {
                                    MaterialDialog materialDialog1 = new MaterialDialog.Builder(TrackOrderActivity.this)
                                            .setMessage("Are you sure you want to request a cancellation of this order?")
                                            .setCancelable(false)
                                            .setPositiveButton("Yes", R.drawable.ic_dialog_okay, (dialogInterface, which) -> {
                                                dialogInterface.dismiss();
                                                loadingDialog.startDialog();

                                                Random random = new Random();
                                                int number1 = random.nextInt(9000) + 1000;
                                                int number2 = random.nextInt(9000) + 1000;
                                                String request_id = "#" + number1 + number2;

                                                Calendar calendar = Calendar.getInstance();
                                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE , dd-MMM-yyyy hh:mm a");
                                                String currentTime = simpleDateFormat.format(calendar.getTime());

                                                HashMap<String, Object> newCancelRequest = new HashMap<>();
                                                newCancelRequest.put("requestID", request_id);
                                                newCancelRequest.put("orderID", preferenceManager.getString(Constants.KEY_ORDER));
                                                newCancelRequest.put("placingTime", order_placing_time);
                                                newCancelRequest.put("requestTime", currentTime);
                                                newCancelRequest.put("requestTimestamp", FieldValue.serverTimestamp());

                                                FirebaseFirestore.getInstance()
                                                        .collection(Constants.KEY_COLLECTION_CITIES)
                                                        .document(preferenceManager.getString(Constants.KEY_USER_CITY))
                                                        .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                        .document(preferenceManager.getString(Constants.KEY_USER_LOCALITY))
                                                        .collection(Constants.KEY_COLLECTION_STORES)
                                                        .document(documentSnapshot.getString(Constants.KEY_ORDER_FROM_STOREID))
                                                        .get()
                                                        .addOnCompleteListener(task -> {
                                                            if (task.isSuccessful()) {
                                                                if (task.getResult().exists()) {
                                                                    FirebaseFirestore.getInstance()
                                                                            .collection(Constants.KEY_COLLECTION_CITIES)
                                                                            .document(preferenceManager.getString(Constants.KEY_USER_CITY))
                                                                            .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                                            .document(preferenceManager.getString(Constants.KEY_USER_LOCALITY))
                                                                            .collection(Constants.KEY_COLLECTION_STORES)
                                                                            .document(documentSnapshot.getString(Constants.KEY_ORDER_FROM_STOREID))
                                                                            .collection(Constants.KEY_COLLECTION_CANCELLATION_REQUESTS)
                                                                            .whereEqualTo("orderID", preferenceManager.getString(Constants.KEY_ORDER))
                                                                            .get()
                                                                            .addOnCompleteListener(task1 -> {
                                                                                if (task1.isSuccessful()) {
                                                                                    if (!task1.getResult().getDocuments().isEmpty() || task1.getResult().getDocuments().size() != 0) {
                                                                                        loadingDialog.dismissDialog();
                                                                                        MaterialDialog materialDialog = new MaterialDialog.Builder(TrackOrderActivity.this)
                                                                                                .setTitle("Already requested!")
                                                                                                .setMessage("You've already requested for cancellation of this order. You'll get the response from the store soon!")
                                                                                                .setCancelable(false)
                                                                                                .setPositiveButton("Okay", R.drawable.ic_dialog_okay, (dialogInterface1, which1) -> dialogInterface1.dismiss())
                                                                                                .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface1, which1) -> dialogInterface1.dismiss()).build();
                                                                                        materialDialog.show();
                                                                                    } else {
                                                                                        FirebaseFirestore.getInstance()
                                                                                                .collection(Constants.KEY_COLLECTION_CITIES)
                                                                                                .document(preferenceManager.getString(Constants.KEY_USER_CITY))
                                                                                                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                                                                .document(preferenceManager.getString(Constants.KEY_USER_LOCALITY))
                                                                                                .collection(Constants.KEY_COLLECTION_STORES)
                                                                                                .document(documentSnapshot.getString(Constants.KEY_ORDER_FROM_STOREID))
                                                                                                .collection(Constants.KEY_COLLECTION_CANCELLATION_REQUESTS)
                                                                                                .document(request_id)
                                                                                                .set(newCancelRequest)
                                                                                                .addOnSuccessListener(aVoid -> {
                                                                                                    loadingDialog.dismissDialog();
                                                                                                    Alerter.create(TrackOrderActivity.this)
                                                                                                            .setText("Success! Cancellation request submitted.")
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
                                                                                            Alerter.create(TrackOrderActivity.this)
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
                                                                                    Alerter.create(TrackOrderActivity.this)
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
                                                                            });
                                                                } else {
                                                                    loadingDialog.dismissDialog();
                                                                    MaterialDialog materialDialog2 = new MaterialDialog.Builder(TrackOrderActivity.this)
                                                                            .setTitle("Can't proceed!")
                                                                            .setMessage("You've to be in the same location where the store is to proceed with the request for cancellation.")
                                                                            .setCancelable(false)
                                                                            .setPositiveButton("Okay", R.drawable.ic_dialog_okay, (dialogInterface1, which1) -> dialogInterface1.dismiss())
                                                                            .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface1, which1) -> dialogInterface1.dismiss()).build();
                                                                    materialDialog2.show();
                                                                }
                                                            } else {
                                                                loadingDialog.dismissDialog();
                                                                Alerter.create(TrackOrderActivity.this)
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
                                                        });
                                            })
                                            .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface1, which1) -> dialogInterface1.dismiss()).build();
                                    materialDialog1.show();
                                }
                            }
                        });
                    } else {
                        requestCancellationBtnContainer.setCardBackgroundColor(getColor(R.color.colorInactive));
                        requestCancellationBtn.setEnabled(false);
                    }
                } else {
                    Alerter.create(TrackOrderActivity.this)
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
            }
        });
    }

    ///////////////////////////////////// LoadOrderItems ///////////////////////////////////////////

    private void loadOrderItems() {
        Query query = null;

        if (preferenceManager.getString(Constants.KEY_ORDER_TYPE).equals("Pending")) {
            query = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                    .document(preferenceManager.getString(Constants.KEY_USER_ID))
                    .collection(Constants.KEY_COLLECTION_PENDING_ORDERS)
                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                    .collection(Constants.KEY_COLLECTION_ORDER_ITEMS)
                    .orderBy(Constants.KEY_ORDER_ITEM_TIMESTAMP, Query.Direction.ASCENDING);
        } else if (preferenceManager.getString(Constants.KEY_ORDER_TYPE).equals("Completed")) {
            query = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                    .document(preferenceManager.getString(Constants.KEY_USER_ID))
                    .collection(Constants.KEY_COLLECTION_COMPLETED_ORDERS)
                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                    .collection(Constants.KEY_COLLECTION_ORDER_ITEMS)
                    .orderBy(Constants.KEY_ORDER_ITEM_TIMESTAMP, Query.Direction.ASCENDING);
        } else if (preferenceManager.getString(Constants.KEY_ORDER_TYPE).equals("Cancelled")) {
            query = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                    .document(preferenceManager.getString(Constants.KEY_USER_ID))
                    .collection(Constants.KEY_COLLECTION_CANCELLED_ORDERS)
                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                    .collection(Constants.KEY_COLLECTION_ORDER_ITEMS)
                    .orderBy(Constants.KEY_ORDER_ITEM_TIMESTAMP, Query.Direction.ASCENDING);
        }

        FirestoreRecyclerOptions<OrderItem> options = new FirestoreRecyclerOptions.Builder<OrderItem>()
                .setLifecycleOwner(TrackOrderActivity.this)
                .setQuery(query, OrderItem.class)
                .build();

        orderItemAdapter = new FirestoreRecyclerAdapter<OrderItem, OrderItemViewHolder>(options) {

            @NonNull
            @Override
            public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_order_item, parent, false);
                return new OrderItemViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position, @NonNull OrderItem model) {
                Glide.with(holder.orderItemProductImage.getContext()).load(model.getOrderItemProductImage())
                        .placeholder(R.drawable.thumbnail).centerCrop().into(holder.orderItemProductImage);

                holder.orderItemProductName.setText(model.getOrderItemProductName());

                holder.orderItemProductQuantity.setText(String.valueOf(model.getOrderItemProductQuantity()));
                holder.orderItemProductPrice.setText(String.format("₹ %s", model.getOrderItemProductRetailPrice()));

                if (model.getOrderItemProductRetailPrice() == model.getOrderItemProductMRP()) {
                    holder.orderItemProductMRP.setVisibility(View.GONE);
                } else {
                    holder.orderItemProductMRP.setVisibility(View.VISIBLE);
                    holder.orderItemProductMRP.setText(String.format("₹ %s", model.getOrderItemProductMRP()));
                    holder.orderItemProductMRP.setPaintFlags(holder.orderItemProductMRP.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                }

                double total_price = Math.round((model.getOrderItemProductQuantity() * model.getOrderItemProductRetailPrice()) * 100.0) / 100.0;

                holder.orderItemProductTotalPrice.setText(String.format("₹ %s", total_price));
            }

            @Override
            public void onError(@NonNull FirebaseFirestoreException e) {
                super.onError(e);
                Alerter.create(TrackOrderActivity.this)
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

        orderItemAdapter.notifyDataSetChanged();

        recyclerOrderItems.setLayoutManager(new LinearLayoutManager(TrackOrderActivity.this));
        recyclerOrderItems.getLayoutManager().setAutoMeasureEnabled(true);
        recyclerOrderItems.setNestedScrollingEnabled(false);
        recyclerOrderItems.setHasFixedSize(false);
        recyclerOrderItems.setAdapter(orderItemAdapter);
    }

    public static class OrderItemViewHolder extends RecyclerView.ViewHolder {

        RoundedImageView orderItemProductImage;
        TextView orderItemProductName, orderItemProductQuantity, orderItemProductPrice, orderItemProductMRP, orderItemProductTotalPrice;

        public OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);

            orderItemProductImage = itemView.findViewById(R.id.order_item_product_image);
            orderItemProductName = itemView.findViewById(R.id.order_item_product_name);
            orderItemProductQuantity = itemView.findViewById(R.id.order_item_product_quantity);
            orderItemProductPrice = itemView.findViewById(R.id.order_item_product_price);
            orderItemProductMRP = itemView.findViewById(R.id.order_item_product_mrp);
            orderItemProductTotalPrice = itemView.findViewById(R.id.order_item_product_total_price);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(TrackOrderActivity trackOrderActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) trackOrderActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(TrackOrderActivity.this)
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
        CustomIntent.customType(TrackOrderActivity.this, "up-to-bottom");
    }
}