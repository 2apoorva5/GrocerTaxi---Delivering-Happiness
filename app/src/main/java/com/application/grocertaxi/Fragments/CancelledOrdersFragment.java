package com.application.grocertaxi.Fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.application.grocertaxi.Model.Order;
import com.application.grocertaxi.R;
import com.application.grocertaxi.TrackOrderActivity;
import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.firebase.ui.firestore.paging.FirestorePagingAdapter;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.firebase.ui.firestore.paging.LoadingState;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import maes.tech.intentanim.CustomIntent;

public class CancelledOrdersFragment extends Fragment {

    private TextView textEmpty;
    private ImageView illustrationEmpty;
    private RecyclerView recyclerCancelledOrders;
    private ProgressBar progressBar;

    private CollectionReference userCancelledOrdersRef;
    private FirestorePagingAdapter<Order, CancelledOrderViewHolder> cancelledOrderAdapter;

    private PreferenceManager preferenceManager;
    private static int LAST_POSITION = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_cancelled_orders, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preferenceManager = new PreferenceManager(getActivity().getApplicationContext());

        textEmpty = view.findViewById(R.id.text_empty);
        illustrationEmpty = view.findViewById(R.id.illustration_empty);
        recyclerCancelledOrders = view.findViewById(R.id.recycler_cancelled_orders);
        progressBar = view.findViewById(R.id.progress_bar);

        userCancelledOrdersRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .collection(Constants.KEY_COLLECTION_CANCELLED_ORDERS);

        progressBar.setVisibility(View.VISIBLE);
    }

    private void loadCancelledOrders() {
        Query query = userCancelledOrdersRef.orderBy(Constants.KEY_ORDER_TIMESTAMP, Query.Direction.DESCENDING);

        PagedList.Config config = new PagedList.Config.Builder()
                .setInitialLoadSizeHint(8)
                .setPageSize(4)
                .build();

        FirestorePagingOptions<Order> options = new FirestorePagingOptions.Builder<Order>()
                .setLifecycleOwner(getActivity())
                .setQuery(query, config, Order.class)
                .build();

        cancelledOrderAdapter = new FirestorePagingAdapter<Order, CancelledOrderViewHolder>(options) {

            @NonNull
            @Override
            public CancelledOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_pending_order_item, parent, false);
                return new CancelledOrderViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull CancelledOrderViewHolder holder, int position, @NonNull Order model) {
                holder.orderID.setText(model.getOrderID());
                holder.orderCancelledTime.setText(String.format("Cancelled on %s", model.getOrderPlacedTime()));

                if (model.getOrderNoOfItems() == 1) {
                    holder.noOfItems.setText(String.format("%d Item", model.getOrderNoOfItems()));
                } else {
                    holder.noOfItems.setText(String.format("%d Items", model.getOrderNoOfItems()));
                }

                holder.orderTotalPayable.setText(String.format("₹ %s", model.getOrderTotalPayable()));
                holder.orderStoreName.setText(model.getOrderFromStoreName());

                holder.clickListener.setOnClickListener(v -> {
                    if (!isConnectedToInternet(getActivity())) {
                        showConnectToInternetDialog();
                        return;
                    } else {
                        preferenceManager.putString(Constants.KEY_ORDER, model.getOrderID());
                        preferenceManager.putString(Constants.KEY_ORDER_TYPE, "Cancelled");
                        startActivity(new Intent(getActivity(), TrackOrderActivity.class));
                        CustomIntent.customType(getActivity(), "bottom-to-up");
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
                        progressBar.setVisibility(View.VISIBLE);
                        illustrationEmpty.setVisibility(View.GONE);
                        textEmpty.setVisibility(View.GONE);
                        break;
                    case LOADED:
                    case FINISHED:
                        progressBar.setVisibility(View.GONE);

                        if (getItemCount() == 0) {
                            illustrationEmpty.setVisibility(View.VISIBLE);
                            textEmpty.setVisibility(View.VISIBLE);
                        } else {
                            illustrationEmpty.setVisibility(View.GONE);
                            textEmpty.setVisibility(View.GONE);
                        }
                        break;
                    case ERROR:
                        Alerter.create(getActivity())
                                .setText("Whoa! Something Broke. Try again!")
                                .setTextAppearance(R.style.AlertText)
                                .setBackgroundColorRes(R.color.errorColor)
                                .setIcon(R.drawable.ic_error)
                                .setDuration(3000)
                                .enableIconPulse(true)
                                .enableVibration(true)
                                .disableOutsideTouch()
                                .enableProgress(true)
                                .setProgressColorInt(getActivity().getColor(android.R.color.white))
                                .show();
                        break;
                }
            }
        };

        cancelledOrderAdapter.notifyDataSetChanged();

        recyclerCancelledOrders.setHasFixedSize(true);
        recyclerCancelledOrders.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerCancelledOrders.setAdapter(cancelledOrderAdapter);
    }

    public static class CancelledOrderViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout clickListener;
        TextView orderID, orderCancelledTime, noOfItems, orderTotalPayable, orderStoreName;

        public CancelledOrderViewHolder(@NonNull View itemView) {
            super(itemView);

            clickListener = itemView.findViewById(R.id.click_listener);
            orderID = itemView.findViewById(R.id.order_id);
            orderCancelledTime = itemView.findViewById(R.id.order_cancelled_time);
            noOfItems = itemView.findViewById(R.id.no_of_items);
            orderTotalPayable = itemView.findViewById(R.id.order_total_payable);
            orderStoreName = itemView.findViewById(R.id.order_store_name);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        loadCancelledOrders();
    }

    private boolean isConnectedToInternet(Activity activity) {
        ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if ((wifiConn != null && wifiConn.isConnected()) || (mobileConn != null && mobileConn.isConnected())) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(getActivity())
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
}
