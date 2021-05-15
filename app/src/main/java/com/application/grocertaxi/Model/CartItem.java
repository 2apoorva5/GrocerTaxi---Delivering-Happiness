package com.application.grocertaxi.Model;

import com.google.firebase.Timestamp;

public class CartItem {
    private String cartItemID, cartItemProductID, cartItemProductStoreID, cartItemProductStoreName,
            cartItemProductCategory, cartItemProductImage, cartItemProductName, cartItemProductUnit;
    private long cartItemProductOffer, cartItemProductQuantity;
    private double cartItemProductMRP, cartItemProductRetailPrice;
    private Timestamp cartItemTimestamp;

    public CartItem() {
    }

    public CartItem(String cartItemID, String cartItemProductID, String cartItemProductStoreID,
                    String cartItemProductStoreName, String cartItemProductCategory,
                    String cartItemProductImage, String cartItemProductName, String cartItemProductUnit,
                    long cartItemProductOffer, long cartItemProductQuantity, double cartItemProductMRP,
                    double cartItemProductRetailPrice, Timestamp cartItemTimestamp) {
        this.cartItemID = cartItemID;
        this.cartItemProductID = cartItemProductID;
        this.cartItemProductStoreID = cartItemProductStoreID;
        this.cartItemProductStoreName = cartItemProductStoreName;
        this.cartItemProductCategory = cartItemProductCategory;
        this.cartItemProductImage = cartItemProductImage;
        this.cartItemProductName = cartItemProductName;
        this.cartItemProductUnit = cartItemProductUnit;
        this.cartItemProductOffer = cartItemProductOffer;
        this.cartItemProductQuantity = cartItemProductQuantity;
        this.cartItemProductMRP = cartItemProductMRP;
        this.cartItemProductRetailPrice = cartItemProductRetailPrice;
        this.cartItemTimestamp = cartItemTimestamp;
    }

    public String getCartItemID() {
        return cartItemID;
    }

    public void setCartItemID(String cartItemID) {
        this.cartItemID = cartItemID;
    }

    public String getCartItemProductID() {
        return cartItemProductID;
    }

    public void setCartItemProductID(String cartItemProductID) {
        this.cartItemProductID = cartItemProductID;
    }

    public String getCartItemProductStoreID() {
        return cartItemProductStoreID;
    }

    public void setCartItemProductStoreID(String cartItemProductStoreID) {
        this.cartItemProductStoreID = cartItemProductStoreID;
    }

    public String getCartItemProductStoreName() {
        return cartItemProductStoreName;
    }

    public void setCartItemProductStoreName(String cartItemProductStoreName) {
        this.cartItemProductStoreName = cartItemProductStoreName;
    }

    public String getCartItemProductCategory() {
        return cartItemProductCategory;
    }

    public void setCartItemProductCategory(String cartItemProductCategory) {
        this.cartItemProductCategory = cartItemProductCategory;
    }

    public String getCartItemProductImage() {
        return cartItemProductImage;
    }

    public void setCartItemProductImage(String cartItemProductImage) {
        this.cartItemProductImage = cartItemProductImage;
    }

    public String getCartItemProductName() {
        return cartItemProductName;
    }

    public void setCartItemProductName(String cartItemProductName) {
        this.cartItemProductName = cartItemProductName;
    }

    public String getCartItemProductUnit() {
        return cartItemProductUnit;
    }

    public void setCartItemProductUnit(String cartItemProductUnit) {
        this.cartItemProductUnit = cartItemProductUnit;
    }

    public long getCartItemProductOffer() {
        return cartItemProductOffer;
    }

    public void setCartItemProductOffer(long cartItemProductOffer) {
        this.cartItemProductOffer = cartItemProductOffer;
    }

    public long getCartItemProductQuantity() {
        return cartItemProductQuantity;
    }

    public void setCartItemProductQuantity(long cartItemProductQuantity) {
        this.cartItemProductQuantity = cartItemProductQuantity;
    }

    public double getCartItemProductMRP() {
        return cartItemProductMRP;
    }

    public void setCartItemProductMRP(double cartItemProductMRP) {
        this.cartItemProductMRP = cartItemProductMRP;
    }

    public double getCartItemProductRetailPrice() {
        return cartItemProductRetailPrice;
    }

    public void setCartItemProductRetailPrice(double cartItemProductRetailPrice) {
        this.cartItemProductRetailPrice = cartItemProductRetailPrice;
    }

    public Timestamp getCartItemTimestamp() {
        return cartItemTimestamp;
    }

    public void setCartItemTimestamp(Timestamp cartItemTimestamp) {
        this.cartItemTimestamp = cartItemTimestamp;
    }
}
