package com.application.grocertaxi.Model;

import com.google.firebase.Timestamp;

public class Product {
    private String productID, productStoreID, productStoreName, productCategory, productImage, productName,
            productUnit, productDescription, productBrand, productMFGDate, productSearchKeyword;
    private boolean productInStock, productIsVeg;
    private double productMRP, productRetailPrice;
    private Timestamp productTimestamp;

    public Product() {
    }

    public Product(String productID, String productStoreID, String productStoreName, String productCategory,
                   String productImage, String productName, String productUnit, String productDescription,
                   String productBrand, String productMFGDate, String productSearchKeyword, boolean productInStock,
                   boolean productIsVeg, double productMRP, double productRetailPrice, Timestamp productTimestamp) {
        this.productID = productID;
        this.productStoreID = productStoreID;
        this.productStoreName = productStoreName;
        this.productCategory = productCategory;
        this.productImage = productImage;
        this.productName = productName;
        this.productUnit = productUnit;
        this.productDescription = productDescription;
        this.productBrand = productBrand;
        this.productMFGDate = productMFGDate;
        this.productSearchKeyword = productSearchKeyword;
        this.productInStock = productInStock;
        this.productIsVeg = productIsVeg;
        this.productMRP = productMRP;
        this.productRetailPrice = productRetailPrice;
        this.productTimestamp = productTimestamp;
    }

    public String getProductID() {
        return productID;
    }

    public void setProductID(String productID) {
        this.productID = productID;
    }

    public String getProductStoreID() {
        return productStoreID;
    }

    public void setProductStoreID(String productStoreID) {
        this.productStoreID = productStoreID;
    }

    public String getProductStoreName() {
        return productStoreName;
    }

    public void setProductStoreName(String productStoreName) {
        this.productStoreName = productStoreName;
    }

    public String getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
    }

    public String getProductImage() {
        return productImage;
    }

    public void setProductImage(String productImage) {
        this.productImage = productImage;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductUnit() {
        return productUnit;
    }

    public void setProductUnit(String productUnit) {
        this.productUnit = productUnit;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public String getProductBrand() {
        return productBrand;
    }

    public void setProductBrand(String productBrand) {
        this.productBrand = productBrand;
    }

    public String getProductMFGDate() {
        return productMFGDate;
    }

    public void setProductMFGDate(String productMFGDate) {
        this.productMFGDate = productMFGDate;
    }

    public String getProductSearchKeyword() {
        return productSearchKeyword;
    }

    public void setProductSearchKeyword(String productSearchKeyword) {
        this.productSearchKeyword = productSearchKeyword;
    }

    public boolean isProductInStock() {
        return productInStock;
    }

    public void setProductInStock(boolean productInStock) {
        this.productInStock = productInStock;
    }

    public boolean isProductIsVeg() {
        return productIsVeg;
    }

    public void setProductIsVeg(boolean productIsVeg) {
        this.productIsVeg = productIsVeg;
    }

    public double getProductMRP() {
        return productMRP;
    }

    public void setProductMRP(double productMRP) {
        this.productMRP = productMRP;
    }

    public double getProductRetailPrice() {
        return productRetailPrice;
    }

    public void setProductRetailPrice(double productRetailPrice) {
        this.productRetailPrice = productRetailPrice;
    }

    public Timestamp getProductTimestamp() {
        return productTimestamp;
    }

    public void setProductTimestamp(Timestamp productTimestamp) {
        this.productTimestamp = productTimestamp;
    }
}
