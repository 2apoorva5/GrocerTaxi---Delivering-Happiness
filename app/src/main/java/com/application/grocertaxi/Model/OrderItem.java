package com.application.grocertaxi.Model;

import com.google.firebase.Timestamp;

public class OrderItem {
    private String orderItemID, orderItemProductID, orderItemProductStoreID, orderItemProductStoreName, orderItemProductCategory,
            orderItemProductImage, orderItemProductName, orderItemProductUnit;
    private long orderItemProductQuantity;
    private double orderItemProductMRP, orderItemProductRetailPrice;
    private Timestamp orderItemTimestamp;

    public OrderItem() {
    }

    public OrderItem(String orderItemID, String orderItemProductID, String orderItemProductStoreID,
                     String orderItemProductStoreName, String orderItemProductCategory,
                     String orderItemProductImage, String orderItemProductName, String orderItemProductUnit,
                     long orderItemProductQuantity, double orderItemProductMRP, double orderItemProductRetailPrice,
                     Timestamp orderItemTimestamp) {
        this.orderItemID = orderItemID;
        this.orderItemProductID = orderItemProductID;
        this.orderItemProductStoreID = orderItemProductStoreID;
        this.orderItemProductStoreName = orderItemProductStoreName;
        this.orderItemProductCategory = orderItemProductCategory;
        this.orderItemProductImage = orderItemProductImage;
        this.orderItemProductName = orderItemProductName;
        this.orderItemProductUnit = orderItemProductUnit;
        this.orderItemProductQuantity = orderItemProductQuantity;
        this.orderItemProductMRP = orderItemProductMRP;
        this.orderItemProductRetailPrice = orderItemProductRetailPrice;
        this.orderItemTimestamp = orderItemTimestamp;
    }

    public String getOrderItemID() {
        return orderItemID;
    }

    public void setOrderItemID(String orderItemID) {
        this.orderItemID = orderItemID;
    }

    public String getOrderItemProductID() {
        return orderItemProductID;
    }

    public void setOrderItemProductID(String orderItemProductID) {
        this.orderItemProductID = orderItemProductID;
    }

    public String getOrderItemProductStoreID() {
        return orderItemProductStoreID;
    }

    public void setOrderItemProductStoreID(String orderItemProductStoreID) {
        this.orderItemProductStoreID = orderItemProductStoreID;
    }

    public String getOrderItemProductStoreName() {
        return orderItemProductStoreName;
    }

    public void setOrderItemProductStoreName(String orderItemProductStoreName) {
        this.orderItemProductStoreName = orderItemProductStoreName;
    }

    public String getOrderItemProductCategory() {
        return orderItemProductCategory;
    }

    public void setOrderItemProductCategory(String orderItemProductCategory) {
        this.orderItemProductCategory = orderItemProductCategory;
    }

    public String getOrderItemProductImage() {
        return orderItemProductImage;
    }

    public void setOrderItemProductImage(String orderItemProductImage) {
        this.orderItemProductImage = orderItemProductImage;
    }

    public String getOrderItemProductName() {
        return orderItemProductName;
    }

    public void setOrderItemProductName(String orderItemProductName) {
        this.orderItemProductName = orderItemProductName;
    }

    public String getOrderItemProductUnit() {
        return orderItemProductUnit;
    }

    public void setOrderItemProductUnit(String orderItemProductUnit) {
        this.orderItemProductUnit = orderItemProductUnit;
    }

    public long getOrderItemProductQuantity() {
        return orderItemProductQuantity;
    }

    public void setOrderItemProductQuantity(long orderItemProductQuantity) {
        this.orderItemProductQuantity = orderItemProductQuantity;
    }

    public double getOrderItemProductMRP() {
        return orderItemProductMRP;
    }

    public void setOrderItemProductMRP(double orderItemProductMRP) {
        this.orderItemProductMRP = orderItemProductMRP;
    }

    public double getOrderItemProductRetailPrice() {
        return orderItemProductRetailPrice;
    }

    public void setOrderItemProductRetailPrice(double orderItemProductRetailPrice) {
        this.orderItemProductRetailPrice = orderItemProductRetailPrice;
    }

    public Timestamp getOrderItemTimestamp() {
        return orderItemTimestamp;
    }

    public void setOrderItemTimestamp(Timestamp orderItemTimestamp) {
        this.orderItemTimestamp = orderItemTimestamp;
    }
}
