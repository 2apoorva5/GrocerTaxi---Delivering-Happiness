package com.application.grocertaxi;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.application.grocertaxi.Utilities.Constants;
import com.application.grocertaxi.Utilities.PreferenceManager;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import maes.tech.intentanim.CustomIntent;

public class CategoriesActivity extends AppCompatActivity {

    private ImageView backBtn, fruits, vegetables, foodGrains, dairy, bakery, beverages, dryFruits, meatBacon,
            noodlesPasta, snacks, kitchenOil, spices, sweets, babyCare, household, personalCare, petCare, stationary,
            hardware, medical, sports, menuHome, menuCategory, menuStore, menuProfile;

    private ConstraintLayout categoryFruits, categoryVegetables, categoryFoodGrains, categoryDairy, categoryBakery, categoryBeverages,
            categoryDryFruits, categoryMeatBacon, categoryNoodlesPasta, categorySnacks, categoryKitchenOil, categorySpices,
            categorySweets, categoryBabyCare, categoryHousehold, categoryPersonalCare, categoryPetCare, categoryStationary,
            categoryHardware, categoryMedical, categorySports;
    private FloatingActionButton cartBtn;

    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(CategoriesActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(CategoriesActivity.this, "fadein-to-fadeout");
            finish();
        } else if (preferenceManager.getString(Constants.KEY_CITY).equals("") ||
                preferenceManager.getString(Constants.KEY_CITY) == null ||
                preferenceManager.getString(Constants.KEY_CITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_CITY).isEmpty() ||
                preferenceManager.getString(Constants.KEY_LOCALITY).equals("") ||
                preferenceManager.getString(Constants.KEY_LOCALITY) == null ||
                preferenceManager.getString(Constants.KEY_LOCALITY).length() == 0 ||
                preferenceManager.getString(Constants.KEY_LOCALITY).isEmpty()) {
            Intent intent = new Intent(CategoriesActivity.this, ChooseCityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(CategoriesActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        initViews();
        setActionOnViews();
    }

    private void initViews() {
        backBtn = findViewById(R.id.back_btn);
        fruits = findViewById(R.id.category_fruits_img);
        vegetables = findViewById(R.id.category_vegetables_img);
        foodGrains = findViewById(R.id.category_foodgrains_img);
        dairy = findViewById(R.id.category_dairy_img);
        bakery = findViewById(R.id.category_bakery_img);
        beverages = findViewById(R.id.category_beverages_img);
        dryFruits = findViewById(R.id.category_dryfruits_img);
        meatBacon = findViewById(R.id.category_meat_img);
        noodlesPasta = findViewById(R.id.category_noodles_img);
        snacks = findViewById(R.id.category_snacks_img);
        kitchenOil = findViewById(R.id.category_oil_img);
        spices = findViewById(R.id.category_spices_img);
        sweets = findViewById(R.id.category_sweets_img);
        babyCare = findViewById(R.id.category_babycare_img);
        household = findViewById(R.id.category_household_img);
        personalCare = findViewById(R.id.category_personalcare_img);
        petCare = findViewById(R.id.category_petcare_img);
        stationary = findViewById(R.id.category_stationary_img);
        hardware = findViewById(R.id.category_hardware_img);
        medical = findViewById(R.id.category_medical_img);
        sports = findViewById(R.id.category_sports_img);

        categoryFruits = findViewById(R.id.category_fruits);
        categoryVegetables = findViewById(R.id.category_vegetables);
        categoryFoodGrains = findViewById(R.id.category_foodgrains);
        categoryDairy = findViewById(R.id.category_dairy);
        categoryBakery = findViewById(R.id.category_bakery);
        categoryBeverages = findViewById(R.id.category_beverages);
        categoryDryFruits = findViewById(R.id.category_dryfruits);
        categoryMeatBacon = findViewById(R.id.category_meat);
        categoryNoodlesPasta = findViewById(R.id.category_noodles);
        categorySnacks = findViewById(R.id.category_snacks);
        categoryKitchenOil = findViewById(R.id.category_oil);
        categorySpices = findViewById(R.id.category_spices);
        categorySweets = findViewById(R.id.category_sweets);
        categoryBabyCare = findViewById(R.id.category_babycare);
        categoryHousehold = findViewById(R.id.category_household);
        categoryPersonalCare = findViewById(R.id.category_personalcare);
        categoryPetCare = findViewById(R.id.category_petcare);
        categoryStationary = findViewById(R.id.category_stationary);
        categoryHardware = findViewById(R.id.category_hardware);
        categoryMedical = findViewById(R.id.category_medical);
        categorySports = findViewById(R.id.category_sports);

        menuHome = findViewById(R.id.menu_home);
        menuCategory = findViewById(R.id.menu_category);
        menuStore = findViewById(R.id.menu_store);
        menuProfile = findViewById(R.id.menu_profile);
        cartBtn = findViewById(R.id.cart_btn);
    }

    private void setActionOnViews() {
        backBtn.setOnClickListener(view -> onBackPressed());

        String cat_fruits = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Ffruits.png?alt=media&token=e682c6a7-16fe-47f1-b607-89b9b888b5d3";
        String cat_vegetables = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fvegetables.png?alt=media&token=5794ffa7-f88e-4477-9068-cd1d9ab4b247";
        String cat_foodgrains = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Ffoodgrains.png?alt=media&token=213bfe18-5525-4b3b-b1e0-83f55de8709e";
        String cat_dairy = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fdairy.png?alt=media&token=3623ac1a-8117-40c6-a13e-ec4be5e2518a";
        String cat_bakery = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fbakery.png?alt=media&token=786111f9-605c-4275-b0b5-901b6df68ec1";
        String cat_beverages = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fbeverages.png?alt=media&token=834a8b60-43df-4ccd-9e2f-559448c895d2";
        String cat_dryfruits = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fdry_fruits.png?alt=media&token=0e8afbf9-6cb5-42d0-ae74-e20b406b9113";
        String cat_meat = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fmeat_bacon.png?alt=media&token=b644e1af-2155-45ae-9d8f-0fc73c610997";
        String cat_noodles = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fnoodles_pasta.png?alt=media&token=e0c37743-9869-4fe9-a2f6-cd3e0ac908ad";
        String cat_snacks = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fsnacks.png?alt=media&token=e3b909e6-68bc-4dd9-bb77-092b7ec2ef7b";
        String cat_oil = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Foil.png?alt=media&token=2d22afd3-5978-4908-b8fe-8d06f9983664";
        String cat_spices = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fspices.png?alt=media&token=00b11823-bb04-4949-999f-f6860496e415";
        String cat_sweets = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fsweets.png?alt=media&token=2e4f6b55-ba63-4e90-8e86-0d3076b3e076";
        String cat_babycare = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fbaby_care.png?alt=media&token=f594f05b-92eb-433f-9cfa-fec19cf80923";
        String cat_household = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fcleaning_household.png?alt=media&token=d89ec809-bd96-42ee-920b-f8c25eefdf74";
        String cat_personalcare = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fpersonal_care.png?alt=media&token=3aa267d3-7764-47cb-ae68-f0694a4982ca";
        String cat_petcare = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fpet_care.png?alt=media&token=b5ce1d43-f34f-4008-88eb-5c81690fdc1f";
        String cat_stationary = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fstationary.png?alt=media&token=dac5dbd6-3235-42c2-9ec5-1615b4b63b44";
        String cat_hardware = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fhardware.png?alt=media&token=2a69540d-c6da-43b2-9358-564cb68a5431";
        String cat_medical = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fmedical.png?alt=media&token=83120872-249b-459a-ae93-8d29dad6bb98";
        String cat_sports = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fsports.png?alt=media&token=cd124d9c-2421-4e93-8e6a-d6e13b4933de";

        Glide.with(CategoriesActivity.this).load(cat_fruits).centerCrop().into(fruits);
        Glide.with(CategoriesActivity.this).load(cat_vegetables).centerCrop().into(vegetables);
        Glide.with(CategoriesActivity.this).load(cat_foodgrains).centerCrop().into(foodGrains);
        Glide.with(CategoriesActivity.this).load(cat_dairy).centerCrop().into(dairy);
        Glide.with(CategoriesActivity.this).load(cat_bakery).centerCrop().into(bakery);
        Glide.with(CategoriesActivity.this).load(cat_beverages).centerCrop().into(beverages);
        Glide.with(CategoriesActivity.this).load(cat_dryfruits).centerCrop().into(dryFruits);
        Glide.with(CategoriesActivity.this).load(cat_meat).centerCrop().into(meatBacon);
        Glide.with(CategoriesActivity.this).load(cat_noodles).centerCrop().into(noodlesPasta);
        Glide.with(CategoriesActivity.this).load(cat_snacks).centerCrop().into(snacks);
        Glide.with(CategoriesActivity.this).load(cat_oil).centerCrop().into(kitchenOil);
        Glide.with(CategoriesActivity.this).load(cat_spices).centerCrop().into(spices);
        Glide.with(CategoriesActivity.this).load(cat_sweets).centerCrop().into(sweets);
        Glide.with(CategoriesActivity.this).load(cat_babycare).centerCrop().into(babyCare);
        Glide.with(CategoriesActivity.this).load(cat_household).centerCrop().into(household);
        Glide.with(CategoriesActivity.this).load(cat_personalcare).centerCrop().into(personalCare);
        Glide.with(CategoriesActivity.this).load(cat_petcare).centerCrop().into(petCare);
        Glide.with(CategoriesActivity.this).load(cat_stationary).centerCrop().into(stationary);
        Glide.with(CategoriesActivity.this).load(cat_hardware).centerCrop().into(hardware);
        Glide.with(CategoriesActivity.this).load(cat_medical).centerCrop().into(medical);
        Glide.with(CategoriesActivity.this).load(cat_sports).centerCrop().into(sports);

        categoryFruits.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Fruits");
            startActivity(new Intent(CategoriesActivity.this, ProductsListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "left-to-right");

        });

        categoryVegetables.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Vegetables");
            startActivity(new Intent(CategoriesActivity.this, ProductsListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "left-to-right");

        });

        categoryFoodGrains.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Food Grains");
            startActivity(new Intent(CategoriesActivity.this, ProductsListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "left-to-right");

        });

        categoryDairy.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Dairy Items");
            startActivity(new Intent(CategoriesActivity.this, ProductsListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "left-to-right");

        });

        categoryBakery.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Bakery Items");
            startActivity(new Intent(CategoriesActivity.this, ProductsListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "left-to-right");

        });

        categoryBeverages.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Beverages");
            startActivity(new Intent(CategoriesActivity.this, ProductsListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "left-to-right");

        });

        categoryDryFruits.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Dry Fruits");
            startActivity(new Intent(CategoriesActivity.this, ProductsListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "left-to-right");

        });

        categoryMeatBacon.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Meat & Bacon");
            startActivity(new Intent(CategoriesActivity.this, ProductsListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "left-to-right");

        });

        categoryNoodlesPasta.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Noodles & Pasta");
            startActivity(new Intent(CategoriesActivity.this, ProductsListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "left-to-right");

        });

        categorySnacks.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Snacks");
            startActivity(new Intent(CategoriesActivity.this, ProductsListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "left-to-right");

        });

        categoryKitchenOil.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Kitchen Oil");
            startActivity(new Intent(CategoriesActivity.this, ProductsListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "left-to-right");

        });

        categorySpices.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Spices");
            startActivity(new Intent(CategoriesActivity.this, ProductsListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "left-to-right");

        });

        categorySweets.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Sweets");
            startActivity(new Intent(CategoriesActivity.this, ProductsListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "left-to-right");

        });

        categoryBabyCare.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Baby Care");
            startActivity(new Intent(CategoriesActivity.this, ProductsListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "left-to-right");

        });

        categoryHousehold.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Household");
            startActivity(new Intent(CategoriesActivity.this, ProductsListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "left-to-right");

        });

        categoryPersonalCare.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Personal Care");
            startActivity(new Intent(CategoriesActivity.this, ProductsListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "left-to-right");

        });

        categoryPetCare.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Pet Care");
            startActivity(new Intent(CategoriesActivity.this, ProductsListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "left-to-right");

        });

        categoryStationary.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Stationary");
            startActivity(new Intent(CategoriesActivity.this, ProductsListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "left-to-right");

        });

        categoryHardware.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Hardware");
            startActivity(new Intent(CategoriesActivity.this, ProductsListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "left-to-right");

        });

        categoryMedical.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Medical");
            startActivity(new Intent(CategoriesActivity.this, ProductsListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "left-to-right");

        });

        categorySports.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Sports");
            startActivity(new Intent(CategoriesActivity.this, ProductsListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "left-to-right");

        });

        menuHome.setOnClickListener(view -> {
            startActivity(new Intent(CategoriesActivity.this, MainActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "fadein-to-fadeout");
            finish();
        });

        menuCategory.setOnClickListener(view -> {
            return;
        });

        menuStore.setOnClickListener(view -> {
            startActivity(new Intent(CategoriesActivity.this, StoresListActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "fadein-to-fadeout");
        });

        menuProfile.setOnClickListener(view -> {
            startActivity(new Intent(CategoriesActivity.this, ProfileActivity.class));
            CustomIntent.customType(CategoriesActivity.this, "fadein-to-fadeout");
        });

        cartBtn.setOnClickListener(v -> {

        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void finish() {
        super.finish();
        CustomIntent.customType(CategoriesActivity.this, "fadein-to-fadeout");
    }
}