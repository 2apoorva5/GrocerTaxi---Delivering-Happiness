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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.NestedScrollView;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.application.grocertaxi.Model.Product;
import com.application.grocertaxi.Model.Store;
import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.baoyz.widget.PullRefreshLayout;
import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.firebase.ui.firestore.paging.FirestorePagingAdapter;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.firebase.ui.firestore.paging.LoadingState;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.iid.FirebaseInstanceId;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.smarteist.autoimageslider.SliderAnimations;
import com.smarteist.autoimageslider.SliderView;
import com.smarteist.autoimageslider.SliderViewAdapter;
import com.tapadoo.alerter.Alerter;

import de.hdodenhof.circleimageview.CircleImageView;
import maes.tech.intentanim.CustomIntent;

public class MainActivity extends AppCompatActivity {

    private ImageView editLocationBtn, fruits, vegetables, foodGrains, dairy, bakery, beverages,
            illustrationEmptyStores, illustrationEmptyFruits, illustrationEmptyVeg, illustrationEmptyFoodGrains, illustrationEmptyPCare, banner1,
            menuHome, menuCategory, menuStore, menuProfile;
    private TextView userLocation, greetings, viewAllStoresBtn, viewAllFruitsBtn, viewAllVegBtn, viewAllFoodGrainsBtn, viewAllPCareBtn,
            textEmptyStores, textEmptyFruits, textEmptyVeg, textEmptyFoodGrains, textEmptyPCare;
    private CircleImageView userProfilePic;
    private SliderView bannerSlider;
    private RecyclerView recyclerStores, recyclerFruits, recyclerVegetables, recyclerFoodGrains, recyclerPCare;
    private ProgressBar storesProgressBar, fruitsProgressBar, vegProgressBar, foodGrainsProgressBar, pCareProgressBar;
    private ConstraintLayout productSearchBtn, categoryFruits, categoryVegetables, categoryFoodGrains,
            categoryDairy, categoryBakery, categoryBeverages, viewAllCategoriesBtn;
    private FloatingActionButton cartBtn;
    private PullRefreshLayout pullRefreshLayout;

    private CollectionReference storesRef, fruitsRef, vegetablesRef, foodGrainsRef, pCareRef;
    private FirestoreRecyclerAdapter<Store, StoreViewHolder> storeAdapter;
    private FirestoreRecyclerAdapter<Product, FruitViewHolder> fruitAdapter;
    private FirestoreRecyclerAdapter<Product, VegetableViewHolder> vegetableAdapter;
    private FirestoreRecyclerAdapter<Product, FoodGrainViewHolder> foodGrainAdapter;
    private FirestoreRecyclerAdapter<Product, PersonalCareViewHolder> personalCareAdapter;

    private PreferenceManager preferenceManager;
    int[] banners = {R.drawable.banner1, R.drawable.banner2, R.drawable.banner3,
            R.drawable.banner4, R.drawable.banner5, R.drawable.banner6};
    private BannerSliderAdapter bannerSliderAdapter;

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
        } else if (preferenceManager.getString(Constants.KEY_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_CITY) == null ||
                preferenceManager.getString(Constants.KEY_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_LOCALITY).equals("") ||
                preferenceManager.getString(Constants.KEY_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_LOCALITY).isEmpty()) {
            Intent intent = new Intent(MainActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        initViews();
        initFirebase();
        setActionOnViews();

        loadStores();
        loadFruits();
        loadVegetables();
        loadFoodGrains();
        loadPersonalCareProducts();

        pullRefreshLayout.setColor(getColor(R.color.colorAccent));
        pullRefreshLayout.setBackgroundColor(getColor(R.color.colorPrimary));
        pullRefreshLayout.setOnRefreshListener(() -> {
            if(!isConnectedToInternet(MainActivity.this)) {
                pullRefreshLayout.setRefreshing(false);
                showConnectToInternetDialog();
                return;
            } else {
                loadStores();
                loadFruits();
                loadVegetables();
                loadFoodGrains();
                loadPersonalCareProducts();
            }
        });

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                sendFCMTokenToDatabase(task.getResult().getToken());
            }
        });
    }

    private void initViews() {
        editLocationBtn = findViewById(R.id.edit_location_btn);
        fruits = findViewById(R.id.category_fruits_img);
        vegetables = findViewById(R.id.category_vegetables_img);
        foodGrains = findViewById(R.id.category_foodgrains_img);
        dairy = findViewById(R.id.category_dairy_img);
        bakery = findViewById(R.id.category_bakery_img);
        beverages = findViewById(R.id.category_beverages_img);
        userLocation = findViewById(R.id.user_location);
        greetings = findViewById(R.id.greetings);
        userProfilePic = findViewById(R.id.user_profile_pic);
        bannerSlider = findViewById(R.id.banner_slider);
        productSearchBtn = findViewById(R.id.product_search_btn);
        categoryFruits = findViewById(R.id.category_fruits);
        categoryVegetables = findViewById(R.id.category_vegetables);
        categoryFoodGrains = findViewById(R.id.category_foodgrains);
        categoryDairy = findViewById(R.id.category_dairy);
        categoryBakery = findViewById(R.id.category_bakery);
        categoryBeverages = findViewById(R.id.category_beverages);
        viewAllCategoriesBtn = findViewById(R.id.view_all_categories_btn);

        //Store
        viewAllStoresBtn = findViewById(R.id.view_all_stores_btn);
        recyclerStores = findViewById(R.id.recycler_stores);
        storesProgressBar = findViewById(R.id.stores_progress_bar);
        illustrationEmptyStores = findViewById(R.id.illustration_empty_stores);
        textEmptyStores = findViewById(R.id.text_empty_stores);
        //Fruits
        viewAllFruitsBtn = findViewById(R.id.view_all_fruits_btn);
        recyclerFruits = findViewById(R.id.recycler_fruits);
        fruitsProgressBar = findViewById(R.id.fruits_progress_bar);
        illustrationEmptyFruits = findViewById(R.id.illustration_empty_fruits);
        textEmptyFruits = findViewById(R.id.text_empty_fruits);
        //Vegetables
        viewAllVegBtn = findViewById(R.id.view_all_veg_btn);
        recyclerVegetables = findViewById(R.id.recycler_vegetables);
        vegProgressBar = findViewById(R.id.veg_progress_bar);
        illustrationEmptyVeg = findViewById(R.id.illustration_empty_veg);
        textEmptyVeg = findViewById(R.id.text_empty_veg);
        //Food Grains
        viewAllFoodGrainsBtn = findViewById(R.id.view_all_foodgrains_btn);
        recyclerFoodGrains = findViewById(R.id.recycler_foodgrains);
        foodGrainsProgressBar = findViewById(R.id.foodgrains_progress_bar);
        illustrationEmptyFoodGrains = findViewById(R.id.illustration_empty_foodgrains);
        textEmptyFoodGrains = findViewById(R.id.text_empty_foodgrains);
        //Personal Care
        viewAllPCareBtn = findViewById(R.id.view_all_pcare_btn);
        recyclerPCare = findViewById(R.id.recycler_pcare);
        pCareProgressBar = findViewById(R.id.pcare_progress_bar);
        illustrationEmptyPCare = findViewById(R.id.illustration_empty_pcare);
        textEmptyPCare = findViewById(R.id.text_empty_pcare);

        banner1 = findViewById(R.id.banner_image1);

        pullRefreshLayout = findViewById(R.id.pull_refresh_layout);

        menuHome = findViewById(R.id.menu_home);
        menuCategory = findViewById(R.id.menu_category);
        menuStore = findViewById(R.id.menu_store);
        menuProfile = findViewById(R.id.menu_profile);
        cartBtn = findViewById(R.id.cart_btn);
    }

    private void initFirebase() {
        storesRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                .collection(Constants.KEY_COLLECTION_STORES);
        fruitsRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                .collection(Constants.KEY_COLLECTION_CATEGORIES)
                .document("Fruits")
                .collection(Constants.KEY_COLLECTION_PRODUCTS);
        vegetablesRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                .collection(Constants.KEY_COLLECTION_CATEGORIES)
                .document("Vegetables")
                .collection(Constants.KEY_COLLECTION_PRODUCTS);
        foodGrainsRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                .collection(Constants.KEY_COLLECTION_CATEGORIES)
                .document("Food Grains")
                .collection(Constants.KEY_COLLECTION_PRODUCTS);
        pCareRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                .collection(Constants.KEY_COLLECTION_CATEGORIES)
                .document("Personal Care")
                .collection(Constants.KEY_COLLECTION_PRODUCTS);
    }

    private void setActionOnViews() {
        storesProgressBar.setVisibility(View.VISIBLE);
        fruitsProgressBar.setVisibility(View.VISIBLE);
        vegProgressBar.setVisibility(View.VISIBLE);
        foodGrainsProgressBar.setVisibility(View.VISIBLE);
        pCareProgressBar.setVisibility(View.VISIBLE);

        userLocation.setText(String.format("%s, %s", preferenceManager.getString(Constants.KEY_LOCALITY), preferenceManager.getString(Constants.KEY_CITY)));

        editLocationBtn.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, ChooseCityActivity.class));
            CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
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
            startActivity(new Intent(MainActivity.this, StoresActivity.class));
            CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
        });

        viewAllFruitsBtn.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Fruits");
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

        banner1.setOnClickListener(v -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "");
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
            startActivity(new Intent(MainActivity.this, StoresActivity.class));
            CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
        });

        menuProfile.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
        });

        cartBtn.setOnClickListener(v -> {

        });
    }


    //Load Banners Adapter

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
                    startActivity(new Intent(MainActivity.this, StoresActivity.class));
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


    //Load Stores Adapter

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
                Glide.with(holder.storeImage.getContext()).load(model.getStoreImage()).centerCrop().into(holder.storeImage);

                holder.storeName.setText(model.getStoreName());

                if (model.isStoreStatus()) {
                    holder.storeStatus.setText("Open");
                    holder.storeStatusContainer.setCardBackgroundColor(getColor(R.color.successColor));
                } else {
                    holder.storeStatus.setText("Closed");
                    holder.storeStatusContainer.setCardBackgroundColor(getColor(R.color.errorColor));
                }

                if (model.getStoreAverageRating() == 0f) {
                    holder.storeRating.setVisibility(View.GONE);
                } else {
                    holder.storeRating.setVisibility(View.VISIBLE);
                    holder.storeRating.setText(String.valueOf(model.getStoreAverageRating()));
                }

                holder.clickListenerStore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();

                pullRefreshLayout.setRefreshing(false);
                storesProgressBar.setVisibility(View.GONE);

                if (getItemCount() == 0) {
                    illustrationEmptyStores.setVisibility(View.VISIBLE);
                    textEmptyStores.setVisibility(View.VISIBLE);
                } else {
                    illustrationEmptyStores.setVisibility(View.GONE);
                    textEmptyStores.setVisibility(View.GONE);
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
        recyclerStores.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
        recyclerStores.setAdapter(storeAdapter);
    }


    //Load Fruits Adapter

    private void loadFruits() {
        Query query = fruitsRef.orderBy(Constants.KEY_PRODUCT_NAME, Query.Direction.ASCENDING).limit(5);

        FirestoreRecyclerOptions<Product> options = new FirestoreRecyclerOptions.Builder<Product>()
                .setLifecycleOwner(MainActivity.this)
                .setQuery(query, Product.class)
                .build();

        fruitAdapter = new FirestoreRecyclerAdapter<Product, FruitViewHolder>(options) {

            @NonNull
            @Override
            public FruitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_home_product_item, parent, false);
                return new FruitViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull FruitViewHolder holder, int position, @NonNull Product model) {
                Glide.with(holder.fruitImage.getContext()).load(model.getProductImage())
                        .placeholder(R.drawable.thumbnail_product).centerCrop().into(holder.fruitImage);

                holder.fruitTypeImage.setImageResource(R.drawable.ic_veg);

                holder.fruitName.setText(model.getProductName());
                holder.fruitUnit.setText(model.getProductUnit());

                if (model.getProductRetailPrice() == model.getProductMRP()) {
                    holder.fruitMRP.setVisibility(View.GONE);
                    holder.fruitOffer.setVisibility(View.GONE);
                } else {
                    holder.fruitMRP.setText(String.format("₹ %s", model.getProductMRP()));
                    holder.fruitMRP.setPaintFlags(holder.fruitMRP.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    float offer = (float) (((model.getProductMRP() - model.getProductRetailPrice()) / model.getProductMRP()) * 100);
                    String offer_value = ((int) offer) + "% off";
                    holder.fruitOffer.setText(offer_value);
                }

                holder.fruitPrice.setText(String.format("₹ %s", model.getProductRetailPrice()));

                holder.clickListenerFruit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });

                holder.addToCartBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();

                pullRefreshLayout.setRefreshing(false);
                fruitsProgressBar.setVisibility(View.GONE);

                if (getItemCount() == 0) {
                    illustrationEmptyFruits.setVisibility(View.VISIBLE);
                    textEmptyFruits.setVisibility(View.VISIBLE);
                } else {
                    illustrationEmptyFruits.setVisibility(View.GONE);
                    textEmptyFruits.setVisibility(View.GONE);
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


    //Load Vegetables Adapter

    private void loadVegetables() {
        Query query = vegetablesRef.orderBy(Constants.KEY_PRODUCT_NAME, Query.Direction.ASCENDING).limit(5);

        FirestoreRecyclerOptions<Product> options = new FirestoreRecyclerOptions.Builder<Product>()
                .setLifecycleOwner(MainActivity.this)
                .setQuery(query, Product.class)
                .build();

        vegetableAdapter = new FirestoreRecyclerAdapter<Product, VegetableViewHolder>(options) {

            @NonNull
            @Override
            public VegetableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_home_product_item, parent, false);
                return new VegetableViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull VegetableViewHolder holder, int position, @NonNull Product model) {
                Glide.with(holder.vegetableImage.getContext()).load(model.getProductImage())
                        .placeholder(R.drawable.thumbnail_product).centerCrop().into(holder.vegetableImage);

                holder.vegetableTypeImage.setImageResource(R.drawable.ic_veg);

                holder.vegetableName.setText(model.getProductName());
                holder.vegetableUnit.setText(model.getProductUnit());

                if (model.getProductRetailPrice() == model.getProductMRP()) {
                    holder.vegetableMRP.setVisibility(View.GONE);
                    holder.vegetableOffer.setVisibility(View.GONE);
                } else {
                    holder.vegetableMRP.setText(String.format("₹ %s", model.getProductMRP()));
                    holder.vegetableMRP.setPaintFlags(holder.vegetableMRP.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    float offer = (float) (((model.getProductMRP() - model.getProductRetailPrice()) / model.getProductMRP()) * 100);
                    String offer_value = ((int) offer) + "% off";
                    holder.vegetableOffer.setText(offer_value);
                }

                holder.vegetablePrice.setText(String.format("₹ %s", model.getProductRetailPrice()));

                holder.clickListenerVegetable.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });

                holder.addToCartBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();

                pullRefreshLayout.setRefreshing(false);
                vegProgressBar.setVisibility(View.GONE);

                if (getItemCount() == 0) {
                    illustrationEmptyVeg.setVisibility(View.VISIBLE);
                    textEmptyVeg.setVisibility(View.VISIBLE);
                } else {
                    illustrationEmptyVeg.setVisibility(View.GONE);
                    textEmptyVeg.setVisibility(View.GONE);
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


    //Load FoodGrains Adapter

    private void loadFoodGrains() {
        Query query = foodGrainsRef.orderBy(Constants.KEY_PRODUCT_NAME, Query.Direction.ASCENDING).limit(5);

        FirestoreRecyclerOptions<Product> options = new FirestoreRecyclerOptions.Builder<Product>()
                .setLifecycleOwner(MainActivity.this)
                .setQuery(query, Product.class)
                .build();

        foodGrainAdapter = new FirestoreRecyclerAdapter<Product, FoodGrainViewHolder>(options) {

            @NonNull
            @Override
            public FoodGrainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_home_product_item, parent, false);
                return new FoodGrainViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull FoodGrainViewHolder holder, int position, @NonNull Product model) {
                Glide.with(holder.foodGrainImage.getContext()).load(model.getProductImage())
                        .placeholder(R.drawable.thumbnail_product).centerCrop().into(holder.foodGrainImage);

                holder.foodGrainTypeImage.setImageResource(R.drawable.ic_veg);

                holder.foodGrainName.setText(model.getProductName());
                holder.foodGrainUnit.setText(model.getProductUnit());

                if (model.getProductRetailPrice() == model.getProductMRP()) {
                    holder.foodGrainMRP.setVisibility(View.GONE);
                    holder.foodGrainOffer.setVisibility(View.GONE);
                } else {
                    holder.foodGrainMRP.setText(String.format("₹ %s", model.getProductMRP()));
                    holder.foodGrainMRP.setPaintFlags(holder.foodGrainMRP.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    float offer = (float) (((model.getProductMRP() - model.getProductRetailPrice()) / model.getProductMRP()) * 100);
                    String offer_value = ((int) offer) + "% off";
                    holder.foodGrainOffer.setText(offer_value);
                }

                holder.foodGrainPrice.setText(String.format("₹ %s", model.getProductRetailPrice()));

                holder.clickListenerFoodGrain.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });

                holder.addToCartBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();

                pullRefreshLayout.setRefreshing(false);
                foodGrainsProgressBar.setVisibility(View.GONE);

                if (getItemCount() == 0) {
                    illustrationEmptyFoodGrains.setVisibility(View.VISIBLE);
                    textEmptyFoodGrains.setVisibility(View.VISIBLE);
                } else {
                    illustrationEmptyFoodGrains.setVisibility(View.GONE);
                    textEmptyFoodGrains.setVisibility(View.GONE);
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


    //Load PersonalCare Adapter

    private void loadPersonalCareProducts() {
        Query query = pCareRef.orderBy(Constants.KEY_PRODUCT_NAME, Query.Direction.ASCENDING).limit(5);

        FirestoreRecyclerOptions<Product> options = new FirestoreRecyclerOptions.Builder<Product>()
                .setLifecycleOwner(MainActivity.this)
                .setQuery(query, Product.class)
                .build();

        personalCareAdapter = new FirestoreRecyclerAdapter<Product, PersonalCareViewHolder>(options) {

            @NonNull
            @Override
            public PersonalCareViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_home_product_item, parent, false);
                return new PersonalCareViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull PersonalCareViewHolder holder, int position, @NonNull Product model) {
                Glide.with(holder.pCareImage.getContext()).load(model.getProductImage())
                        .placeholder(R.drawable.thumbnail_product).centerCrop().into(holder.pCareImage);

                holder.pCareTypeImage.setVisibility(View.GONE);

                holder.pCareName.setText(model.getProductName());
                holder.pCareUnit.setText(model.getProductUnit());

                if (model.getProductRetailPrice() == model.getProductMRP()) {
                    holder.pCareMRP.setVisibility(View.GONE);
                    holder.pCareOffer.setVisibility(View.GONE);
                } else {
                    holder.pCareMRP.setText(String.format("₹ %s", model.getProductMRP()));
                    holder.pCareMRP.setPaintFlags(holder.pCareMRP.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    float offer = (float) (((model.getProductMRP() - model.getProductRetailPrice()) / model.getProductMRP()) * 100);
                    String offer_value = ((int) offer) + "% off";
                    holder.pCareOffer.setText(offer_value);
                }

                holder.pCarePrice.setText(String.format("₹ %s", model.getProductRetailPrice()));

                holder.clickListenerPCare.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });

                holder.addToCartBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();

                pullRefreshLayout.setRefreshing(false);
                pCareProgressBar.setVisibility(View.GONE);

                if (getItemCount() == 0) {
                    illustrationEmptyPCare.setVisibility(View.VISIBLE);
                    textEmptyPCare.setVisibility(View.VISIBLE);
                } else {
                    illustrationEmptyPCare.setVisibility(View.GONE);
                    textEmptyPCare.setVisibility(View.GONE);
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


    //StoreViewHolder

    public static class StoreViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout clickListenerStore;
        ImageView storeImage;
        CardView storeStatusContainer;
        TextView storeName, storeRating, storeStatus;

        public StoreViewHolder(@NonNull View itemView) {
            super(itemView);

            clickListenerStore = itemView.findViewById(R.id.click_listener_store);
            storeImage = itemView.findViewById(R.id.store_image);
            storeName = itemView.findViewById(R.id.store_name);
            storeRating = itemView.findViewById(R.id.store_rating);
            storeStatus = itemView.findViewById(R.id.store_status);
            storeStatusContainer = itemView.findViewById(R.id.store_status_container);
        }
    }


    //FruitViewHolder

    public static class FruitViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout clickListenerFruit, addToCartBtn;
        ImageView fruitImage, fruitTypeImage;
        TextView fruitName, fruitPrice, fruitMRP, fruitOffer,
                fruitUnit;

        public FruitViewHolder(@NonNull View itemView) {
            super(itemView);

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


    //VegetableViewHolder

    public static class VegetableViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout clickListenerVegetable, addToCartBtn;
        ImageView vegetableImage, vegetableTypeImage;
        TextView vegetableName, vegetablePrice, vegetableMRP, vegetableOffer,
                vegetableUnit;

        public VegetableViewHolder(@NonNull View itemView) {
            super(itemView);

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


    //FoodGrainViewHolder

    public static class FoodGrainViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout clickListenerFoodGrain, addToCartBtn;
        ImageView foodGrainImage, foodGrainTypeImage;
        TextView foodGrainName, foodGrainPrice, foodGrainMRP, foodGrainOffer,
                foodGrainUnit;

        public FoodGrainViewHolder(@NonNull View itemView) {
            super(itemView);

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


    //PersonalCareViewHolder

    public static class PersonalCareViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout clickListenerPCare, addToCartBtn;
        ImageView pCareImage, pCareTypeImage;
        TextView pCareName, pCarePrice, pCareMRP, pCareOffer,
                pCareUnit;

        public PersonalCareViewHolder(@NonNull View itemView) {
            super(itemView);

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
        finishAffinity();
    }
}