package com.application.grocertaxi;

import android.app.AlertDialog;
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
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.application.grocertaxi.Model.Product;
import com.application.grocertaxi.Model.Store;
import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.baoyz.widget.PullRefreshLayout;
import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.iid.FirebaseInstanceId;
import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.smarteist.autoimageslider.SliderAnimations;
import com.smarteist.autoimageslider.SliderView;
import com.smarteist.autoimageslider.SliderViewAdapter;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import dmax.dialog.SpotsDialog;
import maes.tech.intentanim.CustomIntent;

public class MainActivity extends AppCompatActivity {

    private ImageView editLocationBtn, fruits, vegetables, foodGrains, dairy, bakery, beverages,
            exploreAllProductsBtn, banner1, banner2, menuHome, menuCategory, menuStore, menuProfile, illustrationEmpty;
    private TextView userLocation, greetings, viewAllStoresBtn, viewAllFruitsBtn,
            viewAllVegBtn, viewAllFoodGrainsBtn, viewAllPCareBtn, textEmpty;
    private CircleImageView userProfilePic;
    private SliderView bannerSlider;
    private RecyclerView recyclerStores, recyclerFruits, recyclerVegetables, recyclerFoodGrains, recyclerPCare;
    private ProgressBar storesProgressBar, fruitsProgressBar, vegProgressBar, foodGrainsProgressBar, pCareProgressBar;
    private ConstraintLayout productSearchBtn, shopByCategoryLayout, categoryFruits, categoryVegetables, categoryFoodGrains,
            categoryDairy, categoryBakery, categoryBeverages, viewAllCategoriesBtn, layoutFirstOrderOffers, offer1, offer2, offer3,
            layoutStores, layoutFruits, layoutVegetables, layoutFoodGrains, layoutPCare, changeLocationBtn;
    private FloatingActionButton cartBtn;
    private CardView layoutSafeDelivery, cartIndicator, changeLocationBtnContainer;
    private PullRefreshLayout pullRefreshLayout;

    private CollectionReference userRef, storesRef, cartRef, fruitsRef, vegetablesRef, foodGrainsRef, pCareRef;
    private FirestoreRecyclerAdapter<Store, StoreViewHolder> storeAdapter;
    private FirestoreRecyclerAdapter<Product, FruitViewHolder> fruitAdapter;
    private FirestoreRecyclerAdapter<Product, VegetableViewHolder> vegetableAdapter;
    private FirestoreRecyclerAdapter<Product, FoodGrainViewHolder> foodGrainAdapter;
    private FirestoreRecyclerAdapter<Product, PersonalCareViewHolder> personalCareAdapter;

    private String cart_location;
    private Shimmer shimmer1, shimmer2, shimmer3, shimmer4;
    int[] banners = {R.drawable.banner1, R.drawable.banner2, R.drawable.banner3,
            R.drawable.banner4, R.drawable.banner5, R.drawable.banner6};
    private BannerSliderAdapter bannerSliderAdapter;
    private PreferenceManager preferenceManager;
    private AlertDialog progressDialog;

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

        progressDialog = new SpotsDialog.Builder().setContext(MainActivity.this)
                .setMessage("Adding item to cart..")
                .setCancelable(false)
                .setTheme(R.style.SpotsDialog)
                .build();

        initViews();
        initFirebase();
        setActionOnViews();

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                sendFCMTokenToDatabase(task.getResult().getToken());
            }
        });
    }

    private void initViews() {
        userLocation = findViewById(R.id.user_location);
        editLocationBtn = findViewById(R.id.edit_location_btn);
        greetings = findViewById(R.id.greetings);
        userProfilePic = findViewById(R.id.user_profile_pic);
        productSearchBtn = findViewById(R.id.product_search_btn);

        bannerSlider = findViewById(R.id.banner_slider);

        shopByCategoryLayout = findViewById(R.id.explore_by_category_layout);
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
        offer1 = findViewById(R.id.offer1);
        offer2 = findViewById(R.id.offer2);
        offer3 = findViewById(R.id.offer3);
        layoutSafeDelivery = findViewById(R.id.layout_safe_delivery);

        //Store
        layoutStores = findViewById(R.id.layout_stores);
        viewAllStoresBtn = findViewById(R.id.view_all_stores_btn);
        recyclerStores = findViewById(R.id.recycler_stores);
        storesProgressBar = findViewById(R.id.stores_progress_bar);
        //Fruits
        layoutFruits = findViewById(R.id.layout_fruits);
        viewAllFruitsBtn = findViewById(R.id.view_all_fruits_btn);
        recyclerFruits = findViewById(R.id.recycler_fruits);
        fruitsProgressBar = findViewById(R.id.fruits_progress_bar);
        //Vegetables
        layoutVegetables = findViewById(R.id.layout_vegetables);
        viewAllVegBtn = findViewById(R.id.view_all_veg_btn);
        recyclerVegetables = findViewById(R.id.recycler_vegetables);
        vegProgressBar = findViewById(R.id.veg_progress_bar);
        //Food Grains
        layoutFoodGrains = findViewById(R.id.layout_foodgrains);
        viewAllFoodGrainsBtn = findViewById(R.id.view_all_foodgrains_btn);
        recyclerFoodGrains = findViewById(R.id.recycler_foodgrains);
        foodGrainsProgressBar = findViewById(R.id.foodgrains_progress_bar);
        //Personal Care
        layoutPCare = findViewById(R.id.layout_pcare);
        viewAllPCareBtn = findViewById(R.id.view_all_pcare_btn);
        recyclerPCare = findViewById(R.id.recycler_pcare);
        pCareProgressBar = findViewById(R.id.pcare_progress_bar);

        exploreAllProductsBtn = findViewById(R.id.explore_all_products_btn);
        banner1 = findViewById(R.id.banner_image1);
        banner2 = findViewById(R.id.banner_image2);

        pullRefreshLayout = findViewById(R.id.pull_refresh_layout);

        menuHome = findViewById(R.id.menu_home);
        menuCategory = findViewById(R.id.menu_category);
        menuStore = findViewById(R.id.menu_store);
        menuProfile = findViewById(R.id.menu_profile);
        cartBtn = findViewById(R.id.cart_btn);
        cartIndicator = findViewById(R.id.cart_indicator);

        illustrationEmpty = findViewById(R.id.illustration_empty);
        textEmpty = findViewById(R.id.text_empty);
        changeLocationBtnContainer = findViewById(R.id.change_location_btn_container);
        changeLocationBtn = findViewById(R.id.change_location_btn);
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

    private void setActionOnViews() {
        storesProgressBar.setVisibility(View.VISIBLE);
        fruitsProgressBar.setVisibility(View.VISIBLE);
        vegProgressBar.setVisibility(View.VISIBLE);
        foodGrainsProgressBar.setVisibility(View.VISIBLE);
        pCareProgressBar.setVisibility(View.VISIBLE);

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

        productSearchBtn.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, SearchProductActivity.class));
            CustomIntent.customType(MainActivity.this, "bottom-to-up");
        });

        bannerSliderAdapter = new BannerSliderAdapter(banners);
        bannerSlider.setSliderAdapter(bannerSliderAdapter);
        bannerSlider.setSliderTransformAnimation(SliderAnimations.SIMPLETRANSFORMATION);
        bannerSlider.startAutoCycle();

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

        viewAllStoresBtn.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, StoresListActivity.class));
            CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
        });

        viewAllFruitsBtn.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Fruits");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");
        });

        exploreAllProductsBtn.setOnClickListener(v -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");
        });

        viewAllVegBtn.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Vegetables");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");
        });

        viewAllFoodGrainsBtn.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Food Grains");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");
        });

        viewAllPCareBtn.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Personal Care");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");
        });

        menuHome.setOnClickListener(view -> {
            return;
        });

        menuCategory.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, CategoriesActivity.class));
            CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
        });

        menuStore.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, StoresListActivity.class));
            CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
        });

        menuProfile.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
        });

        cartBtn.setOnClickListener(v -> {
            preferenceManager.putString(Constants.KEY_ORDER_ID, "");
            preferenceManager.putString(Constants.KEY_ORDER_BY_USERID, "");
            preferenceManager.putString(Constants.KEY_ORDER_BY_USERNAME, "");
            preferenceManager.putString(Constants.KEY_ORDER_FROM_STOREID, "");
            preferenceManager.putString(Constants.KEY_ORDER_FROM_STORENAME, "");
            preferenceManager.putString(Constants.KEY_ORDER_CUSTOMER_NAME, "");
            preferenceManager.putString(Constants.KEY_ORDER_CUSTOMER_MOBILE, "");
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_ADDRESS, "");
            preferenceManager.putString(Constants.KEY_ORDER_NO_OF_ITEMS, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_TOTAL_MRP, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_TOTAL_RETAIL_PRICE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_TOTAL_DISCOUNT, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_DELIVERY_CHARGES, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_TIP_AMOUNT, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_SUB_TOTAL, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_PAYMENT_MODE, "");
            preferenceManager.putString(Constants.KEY_ORDER_CONVENIENCE_FEE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_TOTAL_PAYABLE, String.valueOf(0));
            preferenceManager.putString(Constants.KEY_ORDER_STATUS, "");
            preferenceManager.putString(Constants.KEY_ORDER_PLACED_TIME, "");
            preferenceManager.putString(Constants.KEY_ORDER_COMPLETION_TIME, "");
            preferenceManager.putString(Constants.KEY_ORDER_CANCELLATION_TIME, "");
            preferenceManager.putString(Constants.KEY_ORDER_TIMESTAMP, "");
            startActivity(new Intent(getApplicationContext(), CartActivity.class));
            CustomIntent.customType(MainActivity.this, "bottom-to-up");
        });
    }


    /////////////////////////////////// Load Banners Adapter ///////////////////////////////////////

    public class BannerSliderAdapter extends SliderViewAdapter<BannerSliderAdapter.Holder> {

        int[] banners;

        public BannerSliderAdapter(int[] banners) {
            this.banners = banners;
        }

        @Override
        public BannerSliderAdapter.Holder onCreateViewHolder(ViewGroup parent) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.layout_banner_item, parent, false);
            return new BannerSliderAdapter.Holder(view);
        }

        @Override
        public void onBindViewHolder(BannerSliderAdapter.Holder viewHolder, int position) {
            viewHolder.bannerImage.setImageResource(banners[position]);
            viewHolder.itemView.setOnClickListener(v -> {
                if (position == 0) {
                    startActivity(new Intent(MainActivity.this, StoresListActivity.class));
                    CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
                } else if (position == 1) {
                    startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                    CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
                } else if (position == 2) {
                    preferenceManager.putString(Constants.KEY_CATEGORY, "");
                    startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
                    CustomIntent.customType(MainActivity.this, "left-to-right");
                } else if (position == 3) {
                    return;
                } else if (position == 4) {
                    return;
                } else if (position == 5) {
                    return;
                }
            });
        }

        @Override
        public int getCount() {
            return banners.length;
        }

        public class Holder extends SliderViewAdapter.ViewHolder {

            ImageView bannerImage;

            public Holder(View itemView) {
                super(itemView);

                bannerImage = itemView.findViewById(R.id.banner_image);
            }
        }
    }


    ///////////////////////////////////// Load Stores Adapter //////////////////////////////////////

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
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_home_store_item, parent, false);
                return new StoreViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull StoreViewHolder holder, int position, @NonNull Store model) {
                Glide.with(holder.storeImage.getContext()).load(model.getStoreImage())
                        .placeholder(R.drawable.thumbnail).centerCrop().into(holder.storeImage);

                holder.storeName.setText(model.getStoreName());

                if (model.isStoreStatus()) {
                    holder.storeStatus.setText("Open");
                    holder.storeStatusContainer.setCardBackgroundColor(getColor(R.color.successColor));
                } else {
                    holder.storeStatus.setText("Closed");
                    holder.storeStatusContainer.setCardBackgroundColor(getColor(R.color.errorColor));
                }

                holder.storeRating.setVisibility(View.VISIBLE);
                holder.storeRating.setText(String.valueOf(model.getStoreAverageRating()));
                holder.storeRatingBar.setVisibility(View.VISIBLE);
                holder.storeRatingBar.setRating((float) model.getStoreAverageRating());

                holder.clickListenerStore.setOnClickListener(v -> {
                    preferenceManager.putString(Constants.KEY_STORE, model.getStoreID());
                    startActivity(new Intent(MainActivity.this, StoreDetailsActivity.class));
                    CustomIntent.customType(MainActivity.this, "bottom-to-up");
                });
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();

                pullRefreshLayout.setRefreshing(false);
                storesProgressBar.setVisibility(View.GONE);

                if (getItemCount() == 0) {
                    productSearchBtn.setEnabled(false);
                    bannerSlider.setVisibility(View.GONE);
                    shopByCategoryLayout.setVisibility(View.GONE);
                    layoutFirstOrderOffers.setVisibility(View.GONE);
                    layoutSafeDelivery.setVisibility(View.GONE);
                    layoutStores.setVisibility(View.GONE);
                    layoutFruits.setVisibility(View.GONE);
                    exploreAllProductsBtn.setVisibility(View.GONE);
                    layoutVegetables.setVisibility(View.GONE);
                    banner1.setVisibility(View.GONE);
                    layoutFoodGrains.setVisibility(View.GONE);
                    layoutPCare.setVisibility(View.GONE);
                    banner2.setVisibility(View.GONE);
                    illustrationEmpty.setVisibility(View.VISIBLE);
                    textEmpty.setVisibility(View.VISIBLE);
                    changeLocationBtnContainer.setVisibility(View.VISIBLE);
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
                    productSearchBtn.setEnabled(true);
                    bannerSlider.setVisibility(View.VISIBLE);
                    shopByCategoryLayout.setVisibility(View.VISIBLE);
                    userRef.document(preferenceManager.getString(Constants.KEY_USER_ID))
                            .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot documentSnapshot = task.getResult();
                            if (documentSnapshot.exists()) {
                                boolean first_order = documentSnapshot.getBoolean(Constants.KEY_USER_FIRST_ORDER);

                                if (first_order) {
                                    layoutFirstOrderOffers.setVisibility(View.VISIBLE);

                                    offer1.setOnClickListener(v -> {
                                        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MainActivity.this);
                                        bottomSheetDialog.setContentView(R.layout.bottom_sheet_delivery_charges);
                                        bottomSheetDialog.setCanceledOnTouchOutside(false);

                                        ImageView closeSheetBtn = bottomSheetDialog.findViewById(R.id.close_bottom_sheet_btn);
                                        closeSheetBtn.setOnClickListener(v12 -> bottomSheetDialog.dismiss());

                                        bottomSheetDialog.show();
                                    });

                                    offer2.setOnClickListener(v -> {
                                        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MainActivity.this);
                                        bottomSheetDialog.setContentView(R.layout.bottom_sheet_convenience_fee);
                                        bottomSheetDialog.setCanceledOnTouchOutside(false);

                                        ImageView closeSheetBtn = bottomSheetDialog.findViewById(R.id.close_bottom_sheet_btn);
                                        closeSheetBtn.setOnClickListener(v12 -> bottomSheetDialog.dismiss());

                                        bottomSheetDialog.show();
                                    });

                                    offer3.setOnClickListener(v -> {
                                        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MainActivity.this);
                                        bottomSheetDialog.setContentView(R.layout.bottom_sheet_first_order_offer);
                                        bottomSheetDialog.setCanceledOnTouchOutside(false);

                                        ImageView closeSheetBtn = bottomSheetDialog.findViewById(R.id.close_bottom_sheet_btn);
                                        closeSheetBtn.setOnClickListener(v12 -> bottomSheetDialog.dismiss());

                                        bottomSheetDialog.show();
                                    });
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
                    });
                    layoutSafeDelivery.setVisibility(View.VISIBLE);
                    layoutStores.setVisibility(View.VISIBLE);
                    layoutFruits.setVisibility(View.VISIBLE);
                    exploreAllProductsBtn.setVisibility(View.VISIBLE);
                    layoutVegetables.setVisibility(View.VISIBLE);
                    banner1.setVisibility(View.VISIBLE);
                    layoutFoodGrains.setVisibility(View.VISIBLE);
                    layoutPCare.setVisibility(View.VISIBLE);
                    banner2.setVisibility(View.VISIBLE);
                    illustrationEmpty.setVisibility(View.GONE);
                    textEmpty.setVisibility(View.GONE);
                    changeLocationBtnContainer.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(@NonNull FirebaseFirestoreException e) {
                super.onError(e);

                pullRefreshLayout.setRefreshing(false);
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


    ////////////////////////////////// Load Fruits Adapter /////////////////////////////////////////

    private void loadFruits() {
        Query query = fruitsRef.orderBy(Constants.KEY_PRODUCT_NAME, Query.Direction.ASCENDING).limit(8);

        FirestoreRecyclerOptions<Product> options = new FirestoreRecyclerOptions.Builder<Product>()
                .setLifecycleOwner(MainActivity.this)
                .setQuery(query, Product.class)
                .build();

        fruitAdapter = new FirestoreRecyclerAdapter<Product, FruitViewHolder>(options) {

            @NonNull
            @Override
            public FruitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_horizontal_product_item, parent, false);
                return new FruitViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull FruitViewHolder holder, int position, @NonNull Product model) {
                Glide.with(holder.fruitImage.getContext()).load(model.getProductImage())
                        .placeholder(R.drawable.thumbnail).centerCrop().into(holder.fruitImage);

                holder.fruitTypeImage.setImageResource(R.drawable.ic_veg);

                holder.fruitName.setText(model.getProductName());
                holder.fruitUnit.setText(model.getProductUnit());

                if (model.getProductRetailPrice() == model.getProductMRP()) {
                    holder.fruitMRP.setVisibility(View.GONE);
                    holder.fruitOffer.setVisibility(View.GONE);
                } else {
                    holder.fruitMRP.setVisibility(View.VISIBLE);
                    holder.fruitOffer.setVisibility(View.VISIBLE);
                    shimmer1 = new Shimmer();
                    holder.fruitMRP.setText(String.format("₹ %s", model.getProductMRP()));
                    holder.fruitMRP.setPaintFlags(holder.fruitMRP.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    float offer = (float) (((model.getProductMRP() - model.getProductRetailPrice()) / model.getProductMRP()) * 100);
                    String offer_value = ((int) offer) + "% off";
                    holder.fruitOffer.setText(offer_value);
                    shimmer1.start(holder.fruitOffer);
                }

                holder.fruitPrice.setText(String.format("₹ %s", model.getProductRetailPrice()));

                holder.clickListenerFruit.setOnClickListener(v -> {
                    preferenceManager.putString(Constants.KEY_PRODUCT, model.getProductID());
                    startActivity(new Intent(MainActivity.this, ProductDetailsActivity.class));
                    CustomIntent.customType(MainActivity.this, "bottom-to-up");
                });

                if (model.isProductInStock()) {
                    holder.addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorAccent));
                    holder.addToCartBtn.setEnabled(true);
                    holder.addToCartBtn.setOnClickListener(v -> {
                        if (!isConnectedToInternet(MainActivity.this)) {
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
                            newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY, 1);

                            cartRef.get().addOnSuccessListener(queryDocumentSnapshots1 -> {
                                if (queryDocumentSnapshots1.getDocuments().size() == 0) {
                                    cartRef.document(cart_id).set(newCartItem)
                                            .addOnSuccessListener(aVoid -> {
                                                progressDialog.dismiss();
                                                cartIndicator.setVisibility(View.VISIBLE);
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
                                                progressDialog.dismiss();
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
                                            progressDialog.dismiss();
                                            MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
                                                    .setTitle("Item cannot be added to your cart!")
                                                    .setMessage("Your cart has already been setup for a store this item does not belong to. You must clear your cart first before proceeding with this item.")
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
                                                                cartRef.document(cart_id)
                                                                        .update(Constants.KEY_CART_ITEM_TIMESTAMP, FieldValue.serverTimestamp(),
                                                                                Constants.KEY_CART_ITEM_PRODUCT_QUANTITY, task.getResult().getLong(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY) + 1)
                                                                        .addOnSuccessListener(aVoid -> {
                                                                            progressDialog.dismiss();
                                                                            cartIndicator.setVisibility(View.VISIBLE);
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
                                                                    progressDialog.dismiss();
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
                                                                cartRef.document(cart_id).set(newCartItem)
                                                                        .addOnSuccessListener(aVoid -> {
                                                                            progressDialog.dismiss();
                                                                            cartIndicator.setVisibility(View.VISIBLE);
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
                                                                            progressDialog.dismiss();
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
                                                            progressDialog.dismiss();
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
                                                progressDialog.dismiss();
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
                                        progressDialog.dismiss();
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
                                progressDialog.dismiss();
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
                    holder.addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorInactive));
                    holder.addToCartBtn.setEnabled(false);
                }
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();

                pullRefreshLayout.setRefreshing(false);
                fruitsProgressBar.setVisibility(View.GONE);

                if (getItemCount() == 0) {
                    layoutFruits.setVisibility(View.GONE);
                } else {
                    layoutFruits.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(@NonNull FirebaseFirestoreException e) {
                super.onError(e);

                pullRefreshLayout.setRefreshing(false);
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


    /////////////////////////////////// Load Vegetables Adapter ////////////////////////////////////

    private void loadVegetables() {
        Query query = vegetablesRef.orderBy(Constants.KEY_PRODUCT_NAME, Query.Direction.ASCENDING).limit(8);

        FirestoreRecyclerOptions<Product> options = new FirestoreRecyclerOptions.Builder<Product>()
                .setLifecycleOwner(MainActivity.this)
                .setQuery(query, Product.class)
                .build();

        vegetableAdapter = new FirestoreRecyclerAdapter<Product, VegetableViewHolder>(options) {

            @NonNull
            @Override
            public VegetableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_horizontal_product_item, parent, false);
                return new VegetableViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull VegetableViewHolder holder, int position, @NonNull Product model) {
                Glide.with(holder.vegetableImage.getContext()).load(model.getProductImage())
                        .placeholder(R.drawable.thumbnail).centerCrop().into(holder.vegetableImage);

                holder.vegetableTypeImage.setImageResource(R.drawable.ic_veg);

                holder.vegetableName.setText(model.getProductName());
                holder.vegetableUnit.setText(model.getProductUnit());

                if (model.getProductRetailPrice() == model.getProductMRP()) {
                    holder.vegetableMRP.setVisibility(View.GONE);
                    holder.vegetableOffer.setVisibility(View.GONE);
                } else {
                    holder.vegetableMRP.setVisibility(View.VISIBLE);
                    holder.vegetableOffer.setVisibility(View.VISIBLE);
                    shimmer2 = new Shimmer();
                    holder.vegetableMRP.setText(String.format("₹ %s", model.getProductMRP()));
                    holder.vegetableMRP.setPaintFlags(holder.vegetableMRP.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    float offer = (float) (((model.getProductMRP() - model.getProductRetailPrice()) / model.getProductMRP()) * 100);
                    String offer_value = ((int) offer) + "% off";
                    holder.vegetableOffer.setText(offer_value);
                    shimmer2.start(holder.vegetableOffer);
                }

                holder.vegetablePrice.setText(String.format("₹ %s", model.getProductRetailPrice()));

                holder.clickListenerVegetable.setOnClickListener(v -> {
                    preferenceManager.putString(Constants.KEY_PRODUCT, model.getProductID());
                    startActivity(new Intent(MainActivity.this, ProductDetailsActivity.class));
                    CustomIntent.customType(MainActivity.this, "bottom-to-up");
                });

                if (model.isProductInStock()) {
                    holder.addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorAccent));
                    holder.addToCartBtn.setEnabled(true);
                    holder.addToCartBtn.setOnClickListener(v -> {
                        if (!isConnectedToInternet(MainActivity.this)) {
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
                            newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY, 1);

                            cartRef.get().addOnSuccessListener(queryDocumentSnapshots1 -> {
                                if (queryDocumentSnapshots1.getDocuments().size() == 0) {
                                    cartRef.document(cart_id).set(newCartItem)
                                            .addOnSuccessListener(aVoid -> {
                                                progressDialog.dismiss();
                                                cartIndicator.setVisibility(View.VISIBLE);
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
                                                progressDialog.dismiss();
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
                                            progressDialog.dismiss();
                                            MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
                                                    .setTitle("Item cannot be added to your cart!")
                                                    .setMessage("Your cart has already been setup for a store this item does not belong to. You must clear your cart first before proceeding with this item.")
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
                                                                cartRef.document(cart_id)
                                                                        .update(Constants.KEY_CART_ITEM_TIMESTAMP, FieldValue.serverTimestamp(),
                                                                                Constants.KEY_CART_ITEM_PRODUCT_QUANTITY, task.getResult().getLong(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY) + 1)
                                                                        .addOnSuccessListener(aVoid -> {
                                                                            progressDialog.dismiss();
                                                                            cartIndicator.setVisibility(View.VISIBLE);
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
                                                                    progressDialog.dismiss();
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
                                                                cartRef.document(cart_id).set(newCartItem)
                                                                        .addOnSuccessListener(aVoid -> {
                                                                            progressDialog.dismiss();
                                                                            cartIndicator.setVisibility(View.VISIBLE);
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
                                                                            progressDialog.dismiss();
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
                                                            progressDialog.dismiss();
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
                                                progressDialog.dismiss();
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
                                        progressDialog.dismiss();
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
                                progressDialog.dismiss();
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
                    holder.addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorInactive));
                    holder.addToCartBtn.setEnabled(false);
                }
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();

                pullRefreshLayout.setRefreshing(false);
                vegProgressBar.setVisibility(View.GONE);

                if (getItemCount() == 0) {
                    layoutVegetables.setVisibility(View.GONE);
                } else {
                    layoutVegetables.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(@NonNull FirebaseFirestoreException e) {
                super.onError(e);

                pullRefreshLayout.setRefreshing(false);
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


    ////////////////////////////////// Load FoodGrains Adapter /////////////////////////////////////

    private void loadFoodGrains() {
        Query query = foodGrainsRef.orderBy(Constants.KEY_PRODUCT_NAME, Query.Direction.ASCENDING).limit(8);

        FirestoreRecyclerOptions<Product> options = new FirestoreRecyclerOptions.Builder<Product>()
                .setLifecycleOwner(MainActivity.this)
                .setQuery(query, Product.class)
                .build();

        foodGrainAdapter = new FirestoreRecyclerAdapter<Product, FoodGrainViewHolder>(options) {

            @NonNull
            @Override
            public FoodGrainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_horizontal_product_item, parent, false);
                return new FoodGrainViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull FoodGrainViewHolder holder, int position, @NonNull Product model) {
                Glide.with(holder.foodGrainImage.getContext()).load(model.getProductImage())
                        .placeholder(R.drawable.thumbnail).centerCrop().into(holder.foodGrainImage);

                holder.foodGrainTypeImage.setImageResource(R.drawable.ic_veg);

                holder.foodGrainName.setText(model.getProductName());
                holder.foodGrainUnit.setText(model.getProductUnit());

                if (model.getProductRetailPrice() == model.getProductMRP()) {
                    holder.foodGrainMRP.setVisibility(View.GONE);
                    holder.foodGrainOffer.setVisibility(View.GONE);
                } else {
                    holder.foodGrainMRP.setVisibility(View.VISIBLE);
                    holder.foodGrainOffer.setVisibility(View.VISIBLE);
                    shimmer3 = new Shimmer();
                    holder.foodGrainMRP.setText(String.format("₹ %s", model.getProductMRP()));
                    holder.foodGrainMRP.setPaintFlags(holder.foodGrainMRP.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    float offer = (float) (((model.getProductMRP() - model.getProductRetailPrice()) / model.getProductMRP()) * 100);
                    String offer_value = ((int) offer) + "% off";
                    holder.foodGrainOffer.setText(offer_value);
                    shimmer3.start(holder.foodGrainOffer);
                }

                holder.foodGrainPrice.setText(String.format("₹ %s", model.getProductRetailPrice()));

                holder.clickListenerFoodGrain.setOnClickListener(v -> {
                    preferenceManager.putString(Constants.KEY_PRODUCT, model.getProductID());
                    startActivity(new Intent(MainActivity.this, ProductDetailsActivity.class));
                    CustomIntent.customType(MainActivity.this, "bottom-to-up");
                });

                if (model.isProductInStock()) {
                    holder.addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorAccent));
                    holder.addToCartBtn.setEnabled(true);
                    holder.addToCartBtn.setOnClickListener(v -> {
                        if (!isConnectedToInternet(MainActivity.this)) {
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
                            newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY, 1);

                            cartRef.get().addOnSuccessListener(queryDocumentSnapshots1 -> {
                                if (queryDocumentSnapshots1.getDocuments().size() == 0) {
                                    cartRef.document(cart_id).set(newCartItem)
                                            .addOnSuccessListener(aVoid -> {
                                                progressDialog.dismiss();
                                                cartIndicator.setVisibility(View.VISIBLE);
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
                                                progressDialog.dismiss();
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
                                            progressDialog.dismiss();
                                            MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
                                                    .setTitle("Item cannot be added to your cart!")
                                                    .setMessage("Your cart has already been setup for a store this item does not belong to. You must clear your cart first before proceeding with this item.")
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
                                                                cartRef.document(cart_id)
                                                                        .update(Constants.KEY_CART_ITEM_TIMESTAMP, FieldValue.serverTimestamp(),
                                                                                Constants.KEY_CART_ITEM_PRODUCT_QUANTITY, task.getResult().getLong(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY) + 1)
                                                                        .addOnSuccessListener(aVoid -> {
                                                                            progressDialog.dismiss();
                                                                            cartIndicator.setVisibility(View.VISIBLE);
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
                                                                    progressDialog.dismiss();
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
                                                                cartRef.document(cart_id).set(newCartItem)
                                                                        .addOnSuccessListener(aVoid -> {
                                                                            progressDialog.dismiss();
                                                                            cartIndicator.setVisibility(View.VISIBLE);
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
                                                                            progressDialog.dismiss();
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
                                                            progressDialog.dismiss();
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
                                                progressDialog.dismiss();
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
                                        progressDialog.dismiss();
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
                                progressDialog.dismiss();
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
                    holder.addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorInactive));
                    holder.addToCartBtn.setEnabled(false);
                }
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();

                pullRefreshLayout.setRefreshing(false);
                foodGrainsProgressBar.setVisibility(View.GONE);

                if (getItemCount() == 0) {
                    layoutFoodGrains.setVisibility(View.GONE);
                } else {
                    layoutFoodGrains.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(@NonNull FirebaseFirestoreException e) {
                super.onError(e);

                pullRefreshLayout.setRefreshing(false);
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


    ///////////////////////////////// Load PersonalCare Adapter ////////////////////////////////////

    private void loadPersonalCareProducts() {
        Query query = pCareRef.orderBy(Constants.KEY_PRODUCT_NAME, Query.Direction.ASCENDING).limit(8);

        FirestoreRecyclerOptions<Product> options = new FirestoreRecyclerOptions.Builder<Product>()
                .setLifecycleOwner(MainActivity.this)
                .setQuery(query, Product.class)
                .build();

        personalCareAdapter = new FirestoreRecyclerAdapter<Product, PersonalCareViewHolder>(options) {

            @NonNull
            @Override
            public PersonalCareViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_horizontal_product_item, parent, false);
                return new PersonalCareViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull PersonalCareViewHolder holder, int position, @NonNull Product model) {
                Glide.with(holder.pCareImage.getContext()).load(model.getProductImage())
                        .placeholder(R.drawable.thumbnail).centerCrop().into(holder.pCareImage);

                holder.pCareTypeImage.setVisibility(View.GONE);

                holder.pCareName.setText(model.getProductName());
                holder.pCareUnit.setText(model.getProductUnit());

                if (model.getProductRetailPrice() == model.getProductMRP()) {
                    holder.pCareMRP.setVisibility(View.GONE);
                    holder.pCareOffer.setVisibility(View.GONE);
                } else {
                    holder.pCareMRP.setVisibility(View.VISIBLE);
                    holder.pCareOffer.setVisibility(View.VISIBLE);
                    shimmer4 = new Shimmer();
                    holder.pCareMRP.setText(String.format("₹ %s", model.getProductMRP()));
                    holder.pCareMRP.setPaintFlags(holder.pCareMRP.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    float offer = (float) (((model.getProductMRP() - model.getProductRetailPrice()) / model.getProductMRP()) * 100);
                    String offer_value = ((int) offer) + "% off";
                    holder.pCareOffer.setText(offer_value);
                    shimmer4.start(holder.pCareOffer);
                }

                holder.pCarePrice.setText(String.format("₹ %s", model.getProductRetailPrice()));

                holder.clickListenerPCare.setOnClickListener(v -> {
                    preferenceManager.putString(Constants.KEY_PRODUCT, model.getProductID());
                    startActivity(new Intent(MainActivity.this, ProductDetailsActivity.class));
                    CustomIntent.customType(MainActivity.this, "bottom-to-up");
                });

                if (model.isProductInStock()) {
                    holder.addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorAccent));
                    holder.addToCartBtn.setEnabled(true);
                    holder.addToCartBtn.setOnClickListener(v -> {
                        if (!isConnectedToInternet(MainActivity.this)) {
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
                            newCartItem.put(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY, 1);

                            cartRef.get().addOnSuccessListener(queryDocumentSnapshots1 -> {
                                if (queryDocumentSnapshots1.getDocuments().size() == 0) {
                                    cartRef.document(cart_id).set(newCartItem)
                                            .addOnSuccessListener(aVoid -> {
                                                progressDialog.dismiss();
                                                cartIndicator.setVisibility(View.VISIBLE);
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
                                                progressDialog.dismiss();
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
                                            progressDialog.dismiss();
                                            MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
                                                    .setTitle("Item cannot be added to your cart!")
                                                    .setMessage("Your cart has already been setup for a store this item does not belong to. You must clear your cart first before proceeding with this item.")
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
                                                                cartRef.document(cart_id)
                                                                        .update(Constants.KEY_CART_ITEM_TIMESTAMP, FieldValue.serverTimestamp(),
                                                                                Constants.KEY_CART_ITEM_PRODUCT_QUANTITY, task.getResult().getLong(Constants.KEY_CART_ITEM_PRODUCT_QUANTITY) + 1)
                                                                        .addOnSuccessListener(aVoid -> {
                                                                            progressDialog.dismiss();
                                                                            cartIndicator.setVisibility(View.VISIBLE);
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
                                                                    progressDialog.dismiss();
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
                                                                cartRef.document(cart_id).set(newCartItem)
                                                                        .addOnSuccessListener(aVoid -> {
                                                                            progressDialog.dismiss();
                                                                            cartIndicator.setVisibility(View.VISIBLE);
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
                                                                            progressDialog.dismiss();
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
                                                            progressDialog.dismiss();
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
                                                progressDialog.dismiss();
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
                                        progressDialog.dismiss();
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
                                progressDialog.dismiss();
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
                    holder.addToCartBtnContainer.setCardBackgroundColor(getColor(R.color.colorInactive));
                    holder.addToCartBtn.setEnabled(false);
                }
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();

                pullRefreshLayout.setRefreshing(false);
                pCareProgressBar.setVisibility(View.GONE);

                if (getItemCount() == 0) {
                    layoutPCare.setVisibility(View.GONE);
                } else {
                    layoutPCare.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(@NonNull FirebaseFirestoreException e) {
                super.onError(e);

                pullRefreshLayout.setRefreshing(false);
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


    /////////////////////////////////////// StoreViewHolder ////////////////////////////////////////

    public static class StoreViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout clickListenerStore;
        ImageView storeImage;
        CardView storeStatusContainer;
        TextView storeName, storeRating, storeStatus;
        RatingBar storeRatingBar;

        public StoreViewHolder(@NonNull View itemView) {
            super(itemView);

            clickListenerStore = itemView.findViewById(R.id.click_listener_store);
            storeImage = itemView.findViewById(R.id.store_image);
            storeName = itemView.findViewById(R.id.store_name);
            storeRating = itemView.findViewById(R.id.store_rating);
            storeStatus = itemView.findViewById(R.id.store_status);
            storeStatusContainer = itemView.findViewById(R.id.store_status_container);
            storeRatingBar = itemView.findViewById(R.id.store_rating_bar);
        }
    }


    ////////////////////////////////////// FruitViewHolder /////////////////////////////////////////

    public static class FruitViewHolder extends RecyclerView.ViewHolder {

        CardView addToCartBtnContainer;
        ConstraintLayout clickListenerFruit, addToCartBtn;
        ImageView fruitImage, fruitTypeImage;
        TextView fruitName, fruitPrice, fruitMRP,
                fruitUnit;
        ShimmerTextView fruitOffer;

        public FruitViewHolder(@NonNull View itemView) {
            super(itemView);

            addToCartBtnContainer = itemView.findViewById(R.id.add_to_cart_btn_container);
            clickListenerFruit = itemView.findViewById(R.id.click_listener_product);
            addToCartBtn = itemView.findViewById(R.id.add_to_cart_btn);
            fruitImage = itemView.findViewById(R.id.product_image);
            fruitTypeImage = itemView.findViewById(R.id.product_type_image);
            fruitName = itemView.findViewById(R.id.product_name);
            fruitPrice = itemView.findViewById(R.id.product_price);
            fruitMRP = itemView.findViewById(R.id.product_mrp);
            fruitOffer = itemView.findViewById(R.id.product_offer);
            fruitUnit = itemView.findViewById(R.id.product_unit);
        }
    }


    /////////////////////////////////// VegetableViewHolder ////////////////////////////////////////

    public static class VegetableViewHolder extends RecyclerView.ViewHolder {

        CardView addToCartBtnContainer;
        ConstraintLayout clickListenerVegetable, addToCartBtn;
        ImageView vegetableImage, vegetableTypeImage;
        TextView vegetableName, vegetablePrice, vegetableMRP,
                vegetableUnit;
        ShimmerTextView vegetableOffer;

        public VegetableViewHolder(@NonNull View itemView) {
            super(itemView);

            addToCartBtnContainer = itemView.findViewById(R.id.add_to_cart_btn_container);
            clickListenerVegetable = itemView.findViewById(R.id.click_listener_product);
            addToCartBtn = itemView.findViewById(R.id.add_to_cart_btn);
            vegetableImage = itemView.findViewById(R.id.product_image);
            vegetableTypeImage = itemView.findViewById(R.id.product_type_image);
            vegetableName = itemView.findViewById(R.id.product_name);
            vegetablePrice = itemView.findViewById(R.id.product_price);
            vegetableMRP = itemView.findViewById(R.id.product_mrp);
            vegetableOffer = itemView.findViewById(R.id.product_offer);
            vegetableUnit = itemView.findViewById(R.id.product_unit);
        }
    }


    ///////////////////////////////////// FoodGrainViewHolder //////////////////////////////////////

    public static class FoodGrainViewHolder extends RecyclerView.ViewHolder {

        CardView addToCartBtnContainer;
        ConstraintLayout clickListenerFoodGrain, addToCartBtn;
        ImageView foodGrainImage, foodGrainTypeImage;
        TextView foodGrainName, foodGrainPrice, foodGrainMRP,
                foodGrainUnit;
        ShimmerTextView foodGrainOffer;

        public FoodGrainViewHolder(@NonNull View itemView) {
            super(itemView);

            addToCartBtnContainer = itemView.findViewById(R.id.add_to_cart_btn_container);
            clickListenerFoodGrain = itemView.findViewById(R.id.click_listener_product);
            addToCartBtn = itemView.findViewById(R.id.add_to_cart_btn);
            foodGrainImage = itemView.findViewById(R.id.product_image);
            foodGrainTypeImage = itemView.findViewById(R.id.product_type_image);
            foodGrainName = itemView.findViewById(R.id.product_name);
            foodGrainPrice = itemView.findViewById(R.id.product_price);
            foodGrainMRP = itemView.findViewById(R.id.product_mrp);
            foodGrainOffer = itemView.findViewById(R.id.product_offer);
            foodGrainUnit = itemView.findViewById(R.id.product_unit);
        }
    }


    //////////////////////////////////// PersonalCareViewHolder ////////////////////////////////////

    public static class PersonalCareViewHolder extends RecyclerView.ViewHolder {

        CardView addToCartBtnContainer;
        ConstraintLayout clickListenerPCare, addToCartBtn;
        ImageView pCareImage, pCareTypeImage;
        TextView pCareName, pCarePrice, pCareMRP,
                pCareUnit;
        ShimmerTextView pCareOffer;

        public PersonalCareViewHolder(@NonNull View itemView) {
            super(itemView);

            addToCartBtnContainer = itemView.findViewById(R.id.add_to_cart_btn_container);
            clickListenerPCare = itemView.findViewById(R.id.click_listener_product);
            addToCartBtn = itemView.findViewById(R.id.add_to_cart_btn);
            pCareImage = itemView.findViewById(R.id.product_image);
            pCareTypeImage = itemView.findViewById(R.id.product_type_image);
            pCareName = itemView.findViewById(R.id.product_name);
            pCarePrice = itemView.findViewById(R.id.product_price);
            pCareMRP = itemView.findViewById(R.id.product_mrp);
            pCareOffer = itemView.findViewById(R.id.product_offer);
            pCareUnit = itemView.findViewById(R.id.product_unit);
        }
    }

    private void sendFCMTokenToDatabase(String token) {
        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        DocumentReference documentReference = firebaseFirestore.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));

        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnSuccessListener(aVoid -> {
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Some ERROR occurred!", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onStart() {
        super.onStart();

        cart_location = String.format("%s, %s", preferenceManager.getString(Constants.KEY_USER_LOCALITY), preferenceManager.getString(Constants.KEY_USER_CITY));

        loadStores();
        loadFruits();
        loadVegetables();
        loadFoodGrains();
        loadPersonalCareProducts();

        cartRef.whereEqualTo(Constants.KEY_CART_ITEM_LOCATION, cart_location).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.size() == 0) {
                cartIndicator.setVisibility(View.GONE);
            } else {
                cartIndicator.setVisibility(View.VISIBLE);
            }
        }).addOnFailureListener(e -> {
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

        pullRefreshLayout.setColor(getColor(R.color.colorAccent));
        pullRefreshLayout.setBackgroundColor(getColor(R.color.colorBackground));
        pullRefreshLayout.setOnRefreshListener(() -> {
            if (!isConnectedToInternet(MainActivity.this)) {
                pullRefreshLayout.setRefreshing(false);
                showConnectToInternetDialog();
                return;
            } else {
                loadStores();
                loadFruits();
                loadVegetables();
                loadFoodGrains();
                loadPersonalCareProducts();

                cartRef.whereEqualTo(Constants.KEY_CART_ITEM_LOCATION, cart_location).get().addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.size() == 0) {
                        cartIndicator.setVisibility(View.GONE);
                    } else {
                        cartIndicator.setVisibility(View.VISIBLE);
                    }
                }).addOnFailureListener(e -> {
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
    }

    private boolean isConnectedToInternet(MainActivity mainActivity) {
        ConnectivityManager connectivityManager = (ConnectivityManager) mainActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if ((wifiConn != null && wifiConn.isConnected()) || (mobileConn != null && mobileConn.isConnected())) {
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
        KeyboardVisibilityEvent.setEventListener(MainActivity.this, isOpen -> {
            if (isOpen) {
                UIUtil.hideKeyboard(MainActivity.this);
            }
        });
        finishAffinity();
    }
}