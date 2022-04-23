package com.application.grocertaxi;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.application.grocertaxi.Helper.LoadingDialog;
import com.application.grocertaxi.Model.Product;
import com.application.grocertaxi.Model.Store;
import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.makeramen.roundedimageview.RoundedImageView;
import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;
import com.tapadoo.alerter.Alerter;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import dev.shreyaspatil.MaterialDialog.MaterialDialog;
import maes.tech.intentanim.CustomIntent;

public class MainActivity extends AppCompatActivity {

    private ImageView editLocationBtn, fruits, vegetables, foodGrains, dairy, bakery, beverages;
    private RoundedImageView exploreAllProductsBtn, safeDeliveryBanner;
    private TextView userLocation, greetings, viewAllStoresBtn, viewAllFruitsBtn, viewAllVegBtn, viewAllFoodGrainsBtn, viewAllPCareBtn;
    private CircleImageView userProfilePic;
    private ViewFlipper bannerSlider;
    private RecyclerView recyclerStores, recyclerFruits, recyclerVegetables, recyclerFoodGrains, recyclerPCare;
    private ConstraintLayout layoutContent, layoutEmpty, layoutNoInternet, retryBtn,
            productSearchBtn, categoryFruits, categoryVegetables, categoryFoodGrains,
            categoryDairy, categoryBakery, categoryBeverages, viewAllCategoriesBtn, layoutFirstOrderOffers,
            layoutFruits, layoutVegetables, layoutFoodGrains, layoutPCare, changeLocationBtn;
    private BottomNavigationView bottomBar;
    private FloatingActionButton cartBtn;
    private SwipeRefreshLayout refreshLayout;

    private CollectionReference userRef, storesRef, cartRef, fruitsRef, vegetablesRef, foodGrainsRef, pCareRef;
    private FirestoreRecyclerAdapter<Store, StoreViewHolder> storeAdapter;
    private FirestoreRecyclerAdapter<Product, FruitViewHolder> fruitAdapter;
    private FirestoreRecyclerAdapter<Product, VegetableViewHolder> vegetableAdapter;
    private FirestoreRecyclerAdapter<Product, FoodGrainViewHolder> foodGrainAdapter;
    private FirestoreRecyclerAdapter<Product, PersonalCareViewHolder> personalCareAdapter;

    private String cart_location;
    private PreferenceManager preferenceManager;
    private LoadingDialog loadingDialog;

    private AppUpdateManager appUpdateManager;
    private static final int RC_APP_UPDATE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
            finish();
        } else if (preferenceManager.getString(Constants.KEY_USER_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_CITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_LOCALITY).isEmpty()) {
            Intent intent = new Intent(MainActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        loadingDialog = new LoadingDialog(MainActivity.this);

        ////////////////////////////////////////////////////////////////////////////////////////////

        appUpdateManager = AppUpdateManagerFactory.create(this);
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(result -> {
            if (result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && result.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(result, AppUpdateType.IMMEDIATE, MainActivity.this, RC_APP_UPDATE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        cart_location = String.format("%s, %s", preferenceManager.getString(Constants.KEY_USER_LOCALITY), preferenceManager.getString(Constants.KEY_USER_CITY));

        ////////////////////////////////////////////////////////////////////////////////////////////

        layoutContent = findViewById(R.id.layout_content);
        layoutNoInternet = findViewById(R.id.layout_no_internet);
        retryBtn = findViewById(R.id.retry_btn);

        refreshLayout = findViewById(R.id.refresh_layout);

        userLocation = findViewById(R.id.user_location);
        editLocationBtn = findViewById(R.id.edit_location_btn);
        greetings = findViewById(R.id.greetings);
        userProfilePic = findViewById(R.id.user_profile_pic);
        productSearchBtn = findViewById(R.id.product_search_btn);

        bannerSlider = findViewById(R.id.banner_slider);

        fruits = findViewById(R.id.category_fruits_img);
        vegetables = findViewById(R.id.category_vegetables_img);
        foodGrains = findViewById(R.id.category_foodgrains_img);
        dairy = findViewById(R.id.category_dairy_img);
        bakery = findViewById(R.id.category_bakery_img);
        beverages = findViewById(R.id.category_beverages_img);
        categoryFruits = findViewById(R.id.category_fruits);
        categoryVegetables = findViewById(R.id.category_vegetables);
        categoryFoodGrains = findViewById(R.id.category_foodgrains);
        categoryDairy = findViewById(R.id.category_dairy);
        categoryBakery = findViewById(R.id.category_bakery);
        categoryBeverages = findViewById(R.id.category_beverages);
        viewAllCategoriesBtn = findViewById(R.id.view_all_categories_btn);

        layoutFirstOrderOffers = findViewById(R.id.layout_first_order_offers);

        //Store
        viewAllStoresBtn = findViewById(R.id.view_all_stores_btn);
        recyclerStores = findViewById(R.id.recycler_stores);
        //Fruits
        layoutFruits = findViewById(R.id.layout_fruits);
        viewAllFruitsBtn = findViewById(R.id.view_all_fruits_btn);
        recyclerFruits = findViewById(R.id.recycler_fruits);
        //Vegetables
        layoutVegetables = findViewById(R.id.layout_vegetables);
        viewAllVegBtn = findViewById(R.id.view_all_veg_btn);
        recyclerVegetables = findViewById(R.id.recycler_vegetables);
        //Food Grains
        layoutFoodGrains = findViewById(R.id.layout_foodgrains);
        viewAllFoodGrainsBtn = findViewById(R.id.view_all_foodgrains_btn);
        recyclerFoodGrains = findViewById(R.id.recycler_foodgrains);
        //Personal Care
        layoutPCare = findViewById(R.id.layout_pcare);
        viewAllPCareBtn = findViewById(R.id.view_all_pcare_btn);
        recyclerPCare = findViewById(R.id.recycler_pcare);

        exploreAllProductsBtn = findViewById(R.id.explore_all_products_btn);
        safeDeliveryBanner = findViewById(R.id.banner_image2);

        bottomBar = findViewById(R.id.bottom_bar);
        cartBtn = findViewById(R.id.cart_btn);

        layoutEmpty = findViewById(R.id.layout_empty);
        changeLocationBtn = findViewById(R.id.change_location_btn);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_APP_UPDATE) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(MainActivity.this, "App Update Cancelled!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkNetworkConnection();
    }

    private void checkNetworkConnection() {
        if (!isConnectedToInternet(MainActivity.this)) {
            layoutContent.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
            layoutNoInternet.setVisibility(View.VISIBLE);
            retryBtn.setOnClickListener(v -> checkNetworkConnection());
        } else {
            initFirebase();
            sendFCMTokenToDatabase();
            setActionOnViews();
        }
    }

    private void initFirebase() {
        userRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS);

        storesRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_LOCALITY))
                .collection(Constants.KEY_COLLECTION_STORES);
        fruitsRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_LOCALITY))
                .collection(Constants.KEY_COLLECTION_CATEGORIES)
                .document("Fruits")
                .collection(Constants.KEY_COLLECTION_PRODUCTS);
        vegetablesRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_LOCALITY))
                .collection(Constants.KEY_COLLECTION_CATEGORIES)
                .document("Vegetables")
                .collection(Constants.KEY_COLLECTION_PRODUCTS);
        foodGrainsRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_LOCALITY))
                .collection(Constants.KEY_COLLECTION_CATEGORIES)
                .document("Food Grains")
                .collection(Constants.KEY_COLLECTION_PRODUCTS);
        pCareRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_USER_LOCALITY))
                .collection(Constants.KEY_COLLECTION_CATEGORIES)
                .document("Personal Care")
                .collection(Constants.KEY_COLLECTION_PRODUCTS);
        cartRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .collection(Constants.KEY_COLLECTION_CART);
    }

    private void sendFCMTokenToDatabase() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String refreshToken = task.getResult();
                        HashMap<String, Object> token = new HashMap<>();
                        token.put(Constants.KEY_USER_TOKEN, refreshToken);

                        DocumentReference documentReference = userRef.document(preferenceManager.getString(Constants.KEY_USER_ID));
                        documentReference.set(token, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                })
                                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Some ERROR occurred!", Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(MainActivity.this, "Some ERROR occurred!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setActionOnViews() {
        layoutNoInternet.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);

        refreshLayout.setRefreshing(false);
        refreshLayout.setOnRefreshListener(this::checkNetworkConnection);

        ////////////////////////////////////////////////////////////////////////////////////////////

        userLocation.setText(cart_location);

        editLocationBtn.setOnClickListener(view -> {
            if (!isConnectedToInternet(MainActivity.this)) {
                showConnectToInternetDialog();
                return;
            } else {
                MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
                        .setTitle("Change location?")
                        .setMessage("You may loose items in your cart, if you change the location. Do you want to proceed?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", R.drawable.ic_dialog_okay, (dialogInterface, which) -> {
                            dialogInterface.dismiss();
                            startActivity(new Intent(MainActivity.this, ChooseCityActivity.class));
                            CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
                        })
                        .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                materialDialog.show();
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        String name = preferenceManager.getString(Constants.KEY_USER_NAME);
        String[] splitName = name.split(" ", 2);
        greetings.setText(String.format("Hey, %s!", splitName[0]));

        if (preferenceManager.getString(Constants.KEY_USER_IMAGE).equals("") ||
                preferenceManager.getString(Constants.KEY_USER_IMAGE).length() == 0 ||
                preferenceManager.getString(Constants.KEY_USER_IMAGE).isEmpty() ||
                preferenceManager.getString(Constants.KEY_USER_IMAGE) == null) {
            userProfilePic.setImageResource(R.drawable.illustration_user_avatar);
            userProfilePic.setBorderWidth(1);
            userProfilePic.setBorderColor(getColor(R.color.colorIconDark));
        } else {
            Glide.with(MainActivity.this).load(preferenceManager.getString(Constants.KEY_USER_IMAGE)).centerCrop().into(userProfilePic);
            userProfilePic.setBorderWidth(0);
        }

        userProfilePic.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        productSearchBtn.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, SearchProductActivity.class));
            CustomIntent.customType(MainActivity.this, "bottom-to-up");
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        bannerSlider.setOnClickListener(view -> {
            switch (view.getId()) {
                case R.id.banner1:
                    preferenceManager.putString(Constants.KEY_CATEGORY, "Vegetables");
                    startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
                    CustomIntent.customType(MainActivity.this, "left-to-right");
                    break;
                case R.id.banner2:
                    preferenceManager.putString(Constants.KEY_CATEGORY, "Fruits");
                    startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
                    CustomIntent.customType(MainActivity.this, "left-to-right");
                    break;
                case R.id.banner3:
                    preferenceManager.putString(Constants.KEY_CATEGORY, "");
                    startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
                    CustomIntent.customType(MainActivity.this, "left-to-right");
                    break;
                case R.id.banner4:
                    break;
                case R.id.banner5:
                    break;
                case R.id.banner6:
                    break;
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        String cat_fruits = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Ffruits.png?alt=media&token=e682c6a7-16fe-47f1-b607-89b9b888b5d3";
        String cat_vegetables = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fvegetables.png?alt=media&token=5794ffa7-f88e-4477-9068-cd1d9ab4b247";
        String cat_foodgrains = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Ffoodgrains.png?alt=media&token=213bfe18-5525-4b3b-b1e0-83f55de8709e";
        String cat_dairy = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fdairy.png?alt=media&token=3623ac1a-8117-40c6-a13e-ec4be5e2518a";
        String cat_bakery = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fbakery.png?alt=media&token=786111f9-605c-4275-b0b5-901b6df68ec1";
        String cat_beverages = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fbeverages.png?alt=media&token=834a8b60-43df-4ccd-9e2f-559448c895d2";

        Glide.with(MainActivity.this).load(cat_fruits).centerCrop().into(fruits);
        Glide.with(MainActivity.this).load(cat_vegetables).centerCrop().into(vegetables);
        Glide.with(MainActivity.this).load(cat_foodgrains).centerCrop().into(foodGrains);
        Glide.with(MainActivity.this).load(cat_dairy).centerCrop().into(dairy);
        Glide.with(MainActivity.this).load(cat_bakery).centerCrop().into(bakery);
        Glide.with(MainActivity.this).load(cat_beverages).centerCrop().into(beverages);

        categoryFruits.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Fruits");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");
        });

        categoryVegetables.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Vegetables");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");
        });

        categoryFoodGrains.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Food Grains");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");
        });

        categoryDairy.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Dairy Items");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");
        });

        categoryBakery.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Bakery Items");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");
        });

        categoryBeverages.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Beverages");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");
        });

        viewAllCategoriesBtn.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, CategoriesActivity.class));
            CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        userRef.document(preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Alerter.create(MainActivity.this)
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
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            boolean first_order = documentSnapshot.getBoolean(Constants.KEY_USER_FIRST_ORDER);

                            if (first_order) {
                                layoutFirstOrderOffers.setVisibility(View.VISIBLE);
                            } else {
                                layoutFirstOrderOffers.setVisibility(View.GONE);
                            }
                        } else {
                            Alerter.create(MainActivity.this)
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

        ////////////////////////////////////////////////////////////////////////////////////////////

        loadStores();
        viewAllStoresBtn.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, StoresListActivity.class));
            CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        loadFruits();
        viewAllFruitsBtn.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Fruits");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        exploreAllProductsBtn.setOnClickListener(v -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        loadVegetables();
        viewAllVegBtn.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Vegetables");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        loadFoodGrains();
        viewAllFoodGrainsBtn.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Food Grains");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        loadPersonalCareProducts();
        viewAllPCareBtn.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Personal Care");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        bottomBar.setSelectedItemId(R.id.menu_home);
        bottomBar.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_home:
                    break;
                case R.id.menu_categories:
                    startActivity(new Intent(MainActivity.this, CategoriesActivity.class));
                    CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
                    break;
                case R.id.menu_stores:
                    startActivity(new Intent(MainActivity.this, StoresListActivity.class));
                    CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
                    break;
                case R.id.menu_profile:
                    startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                    CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
                    break;
            }
            return true;
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

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
            CustomIntent.customType(MainActivity.this, "bottom-to-up");
        });
    }


    ///////////////////////////////////////// LoadStores ///////////////////////////////////////////

    private void loadStores() {
        Query query = storesRef.orderBy(Constants.KEY_STORE_AVERAGE_RATING, Query.Direction.DESCENDING).limit(5);
        FirestoreRecyclerOptions<Store> options = new FirestoreRecyclerOptions.Builder<Store>()
                .setLifecycleOwner(MainActivity.this)
                .setQuery(query, Store.class)
                .build();

        storeAdapter = new FirestoreRecyclerAdapter<Store, StoreViewHolder>(options) {

            @NonNull
            @Override
            public StoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_home_store, parent, false);
                return new StoreViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull StoreViewHolder holder, int position, @NonNull Store model) {
                Glide.with(holder.storeImage.getContext()).load(model.getStoreImage())
                        .placeholder(R.drawable.thumbnail).centerCrop().into(holder.storeImage);

                holder.storeName.setText(model.getStoreName());

                if (model.isStoreStatus()) {
                    holder.storeImage.clearColorFilter();

                    holder.storeStatus.setText("Open");
                    holder.storeStatusContainer.setCardBackgroundColor(getColor(R.color.successColor));
                } else {
                    ColorMatrix matrix = new ColorMatrix();
                    matrix.setSaturation(0);

                    ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
                    holder.storeImage.setColorFilter(filter);

                    holder.storeStatus.setText("Closed");
                    holder.storeStatusContainer.setCardBackgroundColor(getColor(R.color.errorColor));
                }

                holder.clickListenerStore.setOnClickListener(v -> {
                    preferenceManager.putString(Constants.KEY_STORE, model.getStoreID());
                    startActivity(new Intent(MainActivity.this, StoreDetailsActivity.class));
                    CustomIntent.customType(MainActivity.this, "bottom-to-up");
                });
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();

                refreshLayout.setRefreshing(false);

                if (getItemCount() == 0) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    changeLocationBtn.setOnClickListener(v -> {
                        if (!isConnectedToInternet(MainActivity.this)) {
                            showConnectToInternetDialog();
                            return;
                        } else {
                            MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
                                    .setTitle("Change location?")
                                    .setMessage("You may loose items in your cart, if you change the location. Do you want to proceed?")
                                    .setCancelable(false)
                                    .setPositiveButton("Yes", R.drawable.ic_dialog_okay, (dialogInterface, which) -> {
                                        dialogInterface.dismiss();
                                        startActivity(new Intent(MainActivity.this, ChooseCityActivity.class));
                                        CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
                                    })
                                    .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                            materialDialog.show();
                        }
                    });
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(@NonNull FirebaseFirestoreException e) {
                super.onError(e);

                refreshLayout.setRefreshing(false);
                Alerter.create(MainActivity.this)
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

        storeAdapter.notifyDataSetChanged();

        recyclerStores.setHasFixedSize(true);
        recyclerStores.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        recyclerStores.setAdapter(storeAdapter);
    }

    /////////////////////////////////////// StoreViewHolder ////////////////////////////////////////

    public static class StoreViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout clickListenerStore;
        ImageView storeImage;
        CardView storeStatusContainer;
        TextView storeName, storeStatus;

        public StoreViewHolder(@NonNull View itemView) {
            super(itemView);

            clickListenerStore = itemView.findViewById(R.id.click_listener);
            storeImage = itemView.findViewById(R.id.store_image);
            storeName = itemView.findViewById(R.id.store_name);
            storeStatus = itemView.findViewById(R.id.store_status);
            storeStatusContainer = itemView.findViewById(R.id.store_status_container);
        }
    }

    ///////////////////////////////////////// LoadFruits ///////////////////////////////////////////

    private void loadFruits() {
        Query query = fruitsRef.orderBy(Constants.KEY_PRODUCT_OFFER, Query.Direction.DESCENDING).limit(5);
        FirestoreRecyclerOptions<Product> options = new FirestoreRecyclerOptions.Builder<Product>()
                .setLifecycleOwner(MainActivity.this)
                .setQuery(query, Product.class)
                .build();

        fruitAdapter = new FirestoreRecyclerAdapter<Product, FruitViewHolder>(options) {

            @NonNull
            @Override
            public FruitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_horizontal_product, parent, false);
                return new FruitViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull FruitViewHolder holder, int position, @NonNull Product model) {
                Glide.with(holder.fruitImage.getContext()).load(model.getProductImage())
                        .placeholder(R.drawable.thumbnail).centerCrop().into(holder.fruitImage);
                holder.fruitTypeImage.setImageResource(R.drawable.ic_veg);

                ////////////////////////////////////////////////////////////////////////////////////

                holder.fruitName.setText(model.getProductName());
                holder.fruitUnit.setText(model.getProductUnit());
                holder.fruitPrice.setText(String.format("₹ %s", model.getProductRetailPrice()));

                ////////////////////////////////////////////////////////////////////////////////////

                if (model.getProductOffer() == 0) {
                    holder.fruitOffer.setVisibility(View.GONE);
                } else {
                    Shimmer shimmer = new Shimmer();
                    holder.fruitOffer.setVisibility(View.VISIBLE);
                    holder.fruitOffer.setText(String.format("%d%% OFF", model.getProductOffer()));
                    shimmer.start(holder.fruitOffer);
                }

                ////////////////////////////////////////////////////////////////////////////////////

                holder.clickListenerFruit.setOnClickListener(v -> {
                    preferenceManager.putString(Constants.KEY_PRODUCT, model.getProductID());
                    startActivity(new Intent(MainActivity.this, ProductDetailsActivity.class));
                    CustomIntent.customType(MainActivity.this, "bottom-to-up");
                });

                ////////////////////////////////////////////////////////////////////////////////////

                if (model.isProductInStock()) {
                    holder.fruitImage.clearColorFilter();
                    holder.fruitOffer.setVisibility(View.VISIBLE);

                    holder.addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorAccent));
                    holder.addToCartBtn.setEnabled(true);
                    holder.addToCartBtn.setOnClickListener(v -> {
                        if (!isConnectedToInternet(MainActivity.this)) {
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
                                                Alerter.create(MainActivity.this)
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
                                                Alerter.create(MainActivity.this)
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
                                            MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
                                                    .setTitle("Item cannot be added to your cart!")
                                                    .setMessage("Your cart has already been setup for another store this item does not belong to. You must clear your cart first before proceeding with this item.")
                                                    .setCancelable(false)
                                                    .setPositiveButton("Go to Cart", R.drawable.ic_dialog_cart, (dialogInterface, which) -> {
                                                        dialogInterface.dismiss();
                                                        startActivity(new Intent(getApplicationContext(), CartActivity.class));
                                                        CustomIntent.customType(MainActivity.this, "bottom-to-up");
                                                    })
                                                    .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                                            materialDialog.show();
                                        } else {
                                            cartRef.document(cart_id).get()
                                                    .addOnCompleteListener(task -> {
                                                        if (task.isSuccessful()) {
                                                            if (task.getResult().exists()) {
                                                                long count = task.getResult().getLong(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY);
                                                                if (count < model.getProductUnitsInStock()) {
                                                                    cartRef.document(cart_id)
                                                                            .update(Constants.KEY_CART_ITEM_TIMESTAMP, FieldValue.serverTimestamp(),
                                                                                    Constants.KEY_CART_ITEM_PRODUCT_QUANTITY, task.getResult().getLong(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY) + 1)
                                                                            .addOnSuccessListener(aVoid -> {
                                                                                loadingDialog.dismissDialog();
                                                                                Alerter.create(MainActivity.this)
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
                                                                        Alerter.create(MainActivity.this)
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
                                                                    MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
                                                                            .setTitle("Item cannot be added to your cart!")
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
                                                                            Alerter.create(MainActivity.this)
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
                                                                            Alerter.create(MainActivity.this)
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
                                                            Alerter.create(MainActivity.this)
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
                                                Alerter.create(MainActivity.this)
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
                                        Alerter.create(MainActivity.this)
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
                                Alerter.create(MainActivity.this)
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
                    holder.fruitImage.setColorFilter(filter);

                    holder.fruitOffer.setVisibility(View.GONE);

                    holder.addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorInactive));
                    holder.addToCartBtn.setEnabled(false);
                }
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();

                refreshLayout.setRefreshing(false);

                if (getItemCount() == 0) {
                    layoutFruits.setVisibility(View.GONE);
                } else {
                    layoutFruits.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(@NonNull FirebaseFirestoreException e) {
                super.onError(e);

                refreshLayout.setRefreshing(false);
                Alerter.create(MainActivity.this)
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

        fruitAdapter.notifyDataSetChanged();

        recyclerFruits.setHasFixedSize(true);
        recyclerFruits.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
        recyclerFruits.setAdapter(fruitAdapter);
    }

    ////////////////////////////////////// FruitViewHolder /////////////////////////////////////////

    public static class FruitViewHolder extends RecyclerView.ViewHolder {

        CardView addToCartBtnContainer;
        ConstraintLayout clickListenerFruit, addToCartBtn;
        ImageView fruitImage, fruitTypeImage;
        TextView fruitName, fruitPrice, fruitUnit;
        ShimmerTextView fruitOffer;

        public FruitViewHolder(@NonNull View itemView) {
            super(itemView);

            clickListenerFruit = itemView.findViewById(R.id.click_listener);
            fruitImage = itemView.findViewById(R.id.product_image);
            fruitOffer = itemView.findViewById(R.id.product_offer);
            fruitTypeImage = itemView.findViewById(R.id.product_type);
            fruitName = itemView.findViewById(R.id.product_name);
            fruitUnit = itemView.findViewById(R.id.product_unit);
            fruitPrice = itemView.findViewById(R.id.product_price);
            addToCartBtnContainer = itemView.findViewById(R.id.add_to_cart_btn_container);
            addToCartBtn = itemView.findViewById(R.id.add_to_cart_btn);
        }
    }

    /////////////////////////////////////// LoadVegetables /////////////////////////////////////////

    private void loadVegetables() {
        Query query = vegetablesRef.orderBy(Constants.KEY_PRODUCT_OFFER, Query.Direction.DESCENDING).limit(5);
        FirestoreRecyclerOptions<Product> options = new FirestoreRecyclerOptions.Builder<Product>()
                .setLifecycleOwner(MainActivity.this)
                .setQuery(query, Product.class)
                .build();

        vegetableAdapter = new FirestoreRecyclerAdapter<Product, VegetableViewHolder>(options) {

            @NonNull
            @Override
            public VegetableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_horizontal_product, parent, false);
                return new VegetableViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull VegetableViewHolder holder, int position, @NonNull Product model) {
                Glide.with(holder.vegetableImage.getContext()).load(model.getProductImage())
                        .placeholder(R.drawable.thumbnail).centerCrop().into(holder.vegetableImage);
                holder.vegetableTypeImage.setImageResource(R.drawable.ic_veg);

                ////////////////////////////////////////////////////////////////////////////////////

                holder.vegetableName.setText(model.getProductName());
                holder.vegetableUnit.setText(model.getProductUnit());
                holder.vegetablePrice.setText(String.format("₹ %s", model.getProductRetailPrice()));

                ////////////////////////////////////////////////////////////////////////////////////

                if (model.getProductOffer() == 0) {
                    holder.vegetableOffer.setVisibility(View.GONE);
                } else {
                    Shimmer shimmer = new Shimmer();
                    holder.vegetableOffer.setVisibility(View.VISIBLE);
                    holder.vegetableOffer.setText(String.format("%d%% OFF", model.getProductOffer()));
                    shimmer.start(holder.vegetableOffer);
                }

                ////////////////////////////////////////////////////////////////////////////////////

                holder.clickListenerVegetable.setOnClickListener(v -> {
                    preferenceManager.putString(Constants.KEY_PRODUCT, model.getProductID());
                    startActivity(new Intent(MainActivity.this, ProductDetailsActivity.class));
                    CustomIntent.customType(MainActivity.this, "bottom-to-up");
                });

                ////////////////////////////////////////////////////////////////////////////////////

                if (model.isProductInStock()) {
                    holder.vegetableImage.clearColorFilter();
                    holder.vegetableOffer.setVisibility(View.VISIBLE);

                    holder.addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorAccent));
                    holder.addToCartBtn.setEnabled(true);
                    holder.addToCartBtn.setOnClickListener(v -> {
                        if (!isConnectedToInternet(MainActivity.this)) {
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
                                                Alerter.create(MainActivity.this)
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
                                                Alerter.create(MainActivity.this)
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
                                            MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
                                                    .setTitle("Item cannot be added to your cart!")
                                                    .setMessage("Your cart has already been setup for another store this item does not belong to. You must clear your cart first before proceeding with this item.")
                                                    .setCancelable(false)
                                                    .setPositiveButton("Go to Cart", R.drawable.ic_dialog_cart, (dialogInterface, which) -> {
                                                        dialogInterface.dismiss();
                                                        startActivity(new Intent(getApplicationContext(), CartActivity.class));
                                                        CustomIntent.customType(MainActivity.this, "bottom-to-up");
                                                    })
                                                    .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                                            materialDialog.show();
                                        } else {
                                            cartRef.document(cart_id).get()
                                                    .addOnCompleteListener(task -> {
                                                        if (task.isSuccessful()) {
                                                            if (task.getResult().exists()) {
                                                                long count = task.getResult().getLong(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY);
                                                                if (count < model.getProductUnitsInStock()) {
                                                                    cartRef.document(cart_id)
                                                                            .update(Constants.KEY_CART_ITEM_TIMESTAMP, FieldValue.serverTimestamp(),
                                                                                    Constants.KEY_CART_ITEM_PRODUCT_QUANTITY, task.getResult().getLong(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY) + 1)
                                                                            .addOnSuccessListener(aVoid -> {
                                                                                loadingDialog.dismissDialog();
                                                                                Alerter.create(MainActivity.this)
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
                                                                        Alerter.create(MainActivity.this)
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
                                                                    MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
                                                                            .setTitle("Item cannot be added to your cart!")
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
                                                                            Alerter.create(MainActivity.this)
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
                                                                            Alerter.create(MainActivity.this)
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
                                                            Alerter.create(MainActivity.this)
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
                                                Alerter.create(MainActivity.this)
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
                                        Alerter.create(MainActivity.this)
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
                                Alerter.create(MainActivity.this)
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
                    holder.vegetableImage.setColorFilter(filter);

                    holder.vegetableOffer.setVisibility(View.GONE);

                    holder.addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorInactive));
                    holder.addToCartBtn.setEnabled(false);
                }
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();

                refreshLayout.setRefreshing(false);

                if (getItemCount() == 0) {
                    layoutVegetables.setVisibility(View.GONE);
                } else {
                    layoutVegetables.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(@NonNull FirebaseFirestoreException e) {
                super.onError(e);

                refreshLayout.setRefreshing(false);
                Alerter.create(MainActivity.this)
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

        vegetableAdapter.notifyDataSetChanged();

        recyclerVegetables.setHasFixedSize(true);
        recyclerVegetables.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
        recyclerVegetables.setAdapter(vegetableAdapter);
    }

    /////////////////////////////////// VegetableViewHolder ////////////////////////////////////////

    public static class VegetableViewHolder extends RecyclerView.ViewHolder {

        CardView addToCartBtnContainer;
        ConstraintLayout clickListenerVegetable, addToCartBtn;
        ImageView vegetableImage, vegetableTypeImage;
        TextView vegetableName, vegetablePrice, vegetableUnit;
        ShimmerTextView vegetableOffer;

        public VegetableViewHolder(@NonNull View itemView) {
            super(itemView);

            clickListenerVegetable = itemView.findViewById(R.id.click_listener);
            vegetableImage = itemView.findViewById(R.id.product_image);
            vegetableTypeImage = itemView.findViewById(R.id.product_type);
            vegetableOffer = itemView.findViewById(R.id.product_offer);
            vegetableName = itemView.findViewById(R.id.product_name);
            vegetableUnit = itemView.findViewById(R.id.product_unit);
            vegetablePrice = itemView.findViewById(R.id.product_price);
            addToCartBtnContainer = itemView.findViewById(R.id.add_to_cart_btn_container);
            addToCartBtn = itemView.findViewById(R.id.add_to_cart_btn);
        }
    }

    ////////////////////////////////////// LoadFoodGrains //////////////////////////////////////////

    private void loadFoodGrains() {
        Query query = foodGrainsRef.orderBy(Constants.KEY_PRODUCT_OFFER, Query.Direction.DESCENDING).limit(5);
        FirestoreRecyclerOptions<Product> options = new FirestoreRecyclerOptions.Builder<Product>()
                .setLifecycleOwner(MainActivity.this)
                .setQuery(query, Product.class)
                .build();

        foodGrainAdapter = new FirestoreRecyclerAdapter<Product, FoodGrainViewHolder>(options) {

            @NonNull
            @Override
            public FoodGrainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_horizontal_product, parent, false);
                return new FoodGrainViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull FoodGrainViewHolder holder, int position, @NonNull Product model) {
                Glide.with(holder.foodGrainImage.getContext()).load(model.getProductImage())
                        .placeholder(R.drawable.thumbnail).centerCrop().into(holder.foodGrainImage);
                holder.foodGrainTypeImage.setImageResource(R.drawable.ic_veg);

                ////////////////////////////////////////////////////////////////////////////////////

                holder.foodGrainName.setText(model.getProductName());
                holder.foodGrainUnit.setText(model.getProductUnit());
                holder.foodGrainPrice.setText(String.format("₹ %s", model.getProductRetailPrice()));

                ////////////////////////////////////////////////////////////////////////////////////

                if (model.getProductOffer() == 0) {
                    holder.foodGrainOffer.setVisibility(View.GONE);
                } else {
                    Shimmer shimmer = new Shimmer();
                    holder.foodGrainOffer.setVisibility(View.VISIBLE);
                    holder.foodGrainOffer.setText(String.format("%d%% OFF", model.getProductOffer()));
                    shimmer.start(holder.foodGrainOffer);
                }

                ////////////////////////////////////////////////////////////////////////////////////

                holder.clickListenerFoodGrain.setOnClickListener(v -> {
                    preferenceManager.putString(Constants.KEY_PRODUCT, model.getProductID());
                    startActivity(new Intent(MainActivity.this, ProductDetailsActivity.class));
                    CustomIntent.customType(MainActivity.this, "bottom-to-up");
                });

                ////////////////////////////////////////////////////////////////////////////////////

                if (model.isProductInStock()) {
                    holder.foodGrainImage.clearColorFilter();
                    holder.foodGrainOffer.setVisibility(View.VISIBLE);

                    holder.addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorAccent));
                    holder.addToCartBtn.setEnabled(true);
                    holder.addToCartBtn.setOnClickListener(v -> {
                        if (!isConnectedToInternet(MainActivity.this)) {
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
                                                Alerter.create(MainActivity.this)
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
                                                Alerter.create(MainActivity.this)
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
                                            MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
                                                    .setTitle("Item cannot be added to your cart!")
                                                    .setMessage("Your cart has already been setup for another store this item does not belong to. You must clear your cart first before proceeding with this item.")
                                                    .setCancelable(false)
                                                    .setPositiveButton("Go to Cart", R.drawable.ic_dialog_cart, (dialogInterface, which) -> {
                                                        dialogInterface.dismiss();
                                                        startActivity(new Intent(getApplicationContext(), CartActivity.class));
                                                        CustomIntent.customType(MainActivity.this, "bottom-to-up");
                                                    })
                                                    .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                                            materialDialog.show();
                                        } else {
                                            cartRef.document(cart_id).get()
                                                    .addOnCompleteListener(task -> {
                                                        if (task.isSuccessful()) {
                                                            if (task.getResult().exists()) {
                                                                long count = task.getResult().getLong(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY);
                                                                if (count < model.getProductUnitsInStock()) {
                                                                    cartRef.document(cart_id)
                                                                            .update(Constants.KEY_CART_ITEM_TIMESTAMP, FieldValue.serverTimestamp(),
                                                                                    Constants.KEY_CART_ITEM_PRODUCT_QUANTITY, task.getResult().getLong(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY) + 1)
                                                                            .addOnSuccessListener(aVoid -> {
                                                                                loadingDialog.dismissDialog();
                                                                                Alerter.create(MainActivity.this)
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
                                                                        Alerter.create(MainActivity.this)
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
                                                                    MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
                                                                            .setTitle("Item cannot be added to your cart!")
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
                                                                            Alerter.create(MainActivity.this)
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
                                                                            Alerter.create(MainActivity.this)
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
                                                            Alerter.create(MainActivity.this)
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
                                                Alerter.create(MainActivity.this)
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
                                        Alerter.create(MainActivity.this)
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
                                Alerter.create(MainActivity.this)
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
                    holder.foodGrainImage.setColorFilter(filter);

                    holder.foodGrainOffer.setVisibility(View.GONE);

                    holder.addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorInactive));
                    holder.addToCartBtn.setEnabled(false);
                }
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();

                refreshLayout.setRefreshing(false);

                if (getItemCount() == 0) {
                    layoutFoodGrains.setVisibility(View.GONE);
                } else {
                    layoutFoodGrains.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(@NonNull FirebaseFirestoreException e) {
                super.onError(e);

                refreshLayout.setRefreshing(false);
                Alerter.create(MainActivity.this)
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

        foodGrainAdapter.notifyDataSetChanged();

        recyclerFoodGrains.setHasFixedSize(true);
        recyclerFoodGrains.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
        recyclerFoodGrains.setAdapter(foodGrainAdapter);
    }

    ///////////////////////////////////// FoodGrainViewHolder //////////////////////////////////////

    public static class FoodGrainViewHolder extends RecyclerView.ViewHolder {

        CardView addToCartBtnContainer;
        ConstraintLayout clickListenerFoodGrain, addToCartBtn;
        ImageView foodGrainImage, foodGrainTypeImage;
        TextView foodGrainName, foodGrainPrice, foodGrainUnit;
        ShimmerTextView foodGrainOffer;

        public FoodGrainViewHolder(@NonNull View itemView) {
            super(itemView);

            clickListenerFoodGrain = itemView.findViewById(R.id.click_listener);
            foodGrainImage = itemView.findViewById(R.id.product_image);
            foodGrainTypeImage = itemView.findViewById(R.id.product_type);
            foodGrainOffer = itemView.findViewById(R.id.product_offer);
            foodGrainName = itemView.findViewById(R.id.product_name);
            foodGrainUnit = itemView.findViewById(R.id.product_unit);
            foodGrainPrice = itemView.findViewById(R.id.product_price);
            addToCartBtnContainer = itemView.findViewById(R.id.add_to_cart_btn_container);
            addToCartBtn = itemView.findViewById(R.id.add_to_cart_btn);
        }
    }

    ///////////////////////////////////// LoadPersonalCare /////////////////////////////////////////

    private void loadPersonalCareProducts() {
        Query query = pCareRef.orderBy(Constants.KEY_PRODUCT_OFFER, Query.Direction.DESCENDING).limit(5);
        FirestoreRecyclerOptions<Product> options = new FirestoreRecyclerOptions.Builder<Product>()
                .setLifecycleOwner(MainActivity.this)
                .setQuery(query, Product.class)
                .build();

        personalCareAdapter = new FirestoreRecyclerAdapter<Product, PersonalCareViewHolder>(options) {

            @NonNull
            @Override
            public PersonalCareViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_horizontal_product, parent, false);
                return new PersonalCareViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull PersonalCareViewHolder holder, int position, @NonNull Product model) {
                Glide.with(holder.pCareImage.getContext()).load(model.getProductImage())
                        .placeholder(R.drawable.thumbnail).centerCrop().into(holder.pCareImage);
                holder.pCareTypeImage.setVisibility(View.GONE);

                ////////////////////////////////////////////////////////////////////////////////////

                holder.pCareName.setText(model.getProductName());
                holder.pCareUnit.setText(model.getProductUnit());
                holder.pCarePrice.setText(String.format("₹ %s", model.getProductRetailPrice()));

                ////////////////////////////////////////////////////////////////////////////////////

                if (model.getProductOffer() == 0) {
                    holder.pCareOffer.setVisibility(View.GONE);
                } else {
                    Shimmer shimmer = new Shimmer();
                    holder.pCareOffer.setVisibility(View.VISIBLE);
                    holder.pCareOffer.setText(String.format("%d%% OFF", model.getProductOffer()));
                    shimmer.start(holder.pCareOffer);
                }

                ////////////////////////////////////////////////////////////////////////////////////

                holder.clickListenerPCare.setOnClickListener(v -> {
                    preferenceManager.putString(Constants.KEY_PRODUCT, model.getProductID());
                    startActivity(new Intent(MainActivity.this, ProductDetailsActivity.class));
                    CustomIntent.customType(MainActivity.this, "bottom-to-up");
                });

                ////////////////////////////////////////////////////////////////////////////////////

                if (model.isProductInStock()) {
                    holder.pCareImage.clearColorFilter();
                    holder.pCareOffer.setVisibility(View.VISIBLE);

                    holder.addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorAccent));
                    holder.addToCartBtn.setEnabled(true);
                    holder.addToCartBtn.setOnClickListener(v -> {
                        if (!isConnectedToInternet(MainActivity.this)) {
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
                                                Alerter.create(MainActivity.this)
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
                                                Alerter.create(MainActivity.this)
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
                                            MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
                                                    .setTitle("Item cannot be added to your cart!")
                                                    .setMessage("Your cart has already been setup for another store this item does not belong to. You must clear your cart first before proceeding with this item.")
                                                    .setCancelable(false)
                                                    .setPositiveButton("Go to Cart", R.drawable.ic_dialog_cart, (dialogInterface, which) -> {
                                                        dialogInterface.dismiss();
                                                        startActivity(new Intent(getApplicationContext(), CartActivity.class));
                                                        CustomIntent.customType(MainActivity.this, "bottom-to-up");
                                                    })
                                                    .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                                            materialDialog.show();
                                        } else {
                                            cartRef.document(cart_id).get()
                                                    .addOnCompleteListener(task -> {
                                                        if (task.isSuccessful()) {
                                                            if (task.getResult().exists()) {
                                                                long count = task.getResult().getLong(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY);
                                                                if (count < model.getProductUnitsInStock()) {
                                                                    cartRef.document(cart_id)
                                                                            .update(Constants.KEY_CART_ITEM_TIMESTAMP, FieldValue.serverTimestamp(),
                                                                                    Constants.KEY_CART_ITEM_PRODUCT_QUANTITY, task.getResult().getLong(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY) + 1)
                                                                            .addOnSuccessListener(aVoid -> {
                                                                                loadingDialog.dismissDialog();
                                                                                Alerter.create(MainActivity.this)
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
                                                                        Alerter.create(MainActivity.this)
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
                                                                    MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
                                                                            .setTitle("Item cannot be added to your cart!")
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
                                                                            Alerter.create(MainActivity.this)
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
                                                                            Alerter.create(MainActivity.this)
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
                                                            Alerter.create(MainActivity.this)
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
                                                Alerter.create(MainActivity.this)
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
                                        Alerter.create(MainActivity.this)
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
                                Alerter.create(MainActivity.this)
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
                    holder.pCareImage.setColorFilter(filter);

                    holder.pCareOffer.setVisibility(View.GONE);

                    holder.addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorInactive));
                    holder.addToCartBtn.setEnabled(false);
                }
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();

                refreshLayout.setRefreshing(false);

                if (getItemCount() == 0) {
                    layoutPCare.setVisibility(View.GONE);
                } else {
                    layoutPCare.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(@NonNull FirebaseFirestoreException e) {
                super.onError(e);

                refreshLayout.setRefreshing(false);
                Alerter.create(MainActivity.this)
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

        personalCareAdapter.notifyDataSetChanged();

        recyclerPCare.setHasFixedSize(true);
        recyclerPCare.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
        recyclerPCare.setAdapter(personalCareAdapter);
    }

    //////////////////////////////////// PersonalCareViewHolder ////////////////////////////////////

    public static class PersonalCareViewHolder extends RecyclerView.ViewHolder {

        CardView addToCartBtnContainer;
        ConstraintLayout clickListenerPCare, addToCartBtn;
        ImageView pCareImage, pCareTypeImage;
        TextView pCareName, pCarePrice, pCareUnit;
        ShimmerTextView pCareOffer;

        public PersonalCareViewHolder(@NonNull View itemView) {
            super(itemView);

            clickListenerPCare = itemView.findViewById(R.id.click_listener);
            pCareImage = itemView.findViewById(R.id.product_image);
            pCareTypeImage = itemView.findViewById(R.id.product_type);
            pCareOffer = itemView.findViewById(R.id.product_offer);
            pCareName = itemView.findViewById(R.id.product_name);
            pCareUnit = itemView.findViewById(R.id.product_unit);
            pCarePrice = itemView.findViewById(R.id.product_price);
            addToCartBtnContainer = itemView.findViewById(R.id.add_to_cart_btn_container);
            addToCartBtn = itemView.findViewById(R.id.add_to_cart_btn);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(MainActivity mainActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) mainActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
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
        finishAffinity();
    }

    @Override
    protected void onResume() {
        super.onResume();
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(result -> {
            if (result.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                try {
                    appUpdateManager.startUpdateFlowForResult(result, AppUpdateType.IMMEDIATE, MainActivity.this, RC_APP_UPDATE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}