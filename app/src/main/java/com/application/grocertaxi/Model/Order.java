package com.application.grocertaxi.Model;

import com.google.firebase.Timestamp;

public class Order {
    private String orderID, orderByUserID, orderCouponApplied, orderByUserName, orderFromStoreID, orderFromStoreName,
            orderCustomerName, orderCustomerMobile, orderDeliveryLocation, orderDeliveryAddress, orderPaymentMode, orderInstructions,
            orderStatus, orderPlacedTime, orderCompletionTime, orderCancellationTime;
    private long orderNoOfItems;
    private double orderDeliveryLatitude, orderDeliveryLongitude, orderTotalMRP, orderTotalRetailPrice, orderCouponDiscount,
            orderTotalDiscount, orderDeliveryCharges, orderTipAmount, orderSubTotal, orderConvenienceFee, orderTotalPayable;
    private Timestamp orderTimestamp;

    public Order() {
    }

    public Order(String orderID, String orderByUserID, String orderCouponApplied, String orderByUserName,
                 String orderFromStoreID, String orderFromStoreName, String orderCustomerName,
                 String orderCustomerMobile, String orderDeliveryLocation, String orderDeliveryAddress,
                 String orderPaymentMode, String orderInstructions, String orderStatus, String orderPlacedTime,
                 String orderCompletionTime, String orderCancellationTime, long orderNoOfItems,
                 double orderDeliveryLatitude, double orderDeliveryLongitude, double orderTotalMRP,
                 double orderTotalRetailPrice, double orderCouponDiscount, double orderTotalDiscount,
                 double orderDeliveryCharges, double orderTipAmount, double orderSubTotal, double orderConvenienceFee,
                 double orderTotalPayable, Timestamp orderTimestamp) {
        this.orderID = orderID;
        this.orderByUserID = orderByUserID;
        this.orderCouponApplied = orderCouponApplied;
        this.orderByUserName = orderByUserName;
        this.orderFromStoreID = orderFromStoreID;
        this.orderFromStoreName = orderFromStoreName;
        this.orderCustomerName = orderCustomerName;
        this.orderCustomerMobile = orderCustomerMobile;
        this.orderDeliveryLocation = orderDeliveryLocation;
        this.orderDeliveryAddress = orderDeliveryAddress;
        this.orderPaymentMode = orderPaymentMode;
        this.orderInstructions = orderInstructions;
        this.orderStatus = orderStatus;
        this.orderPlacedTime = orderPlacedTime;
        this.orderCompletionTime = orderCompletionTime;
        this.orderCancellationTime = orderCancellationTime;
        this.orderNoOfItems = orderNoOfItems;
        this.orderDeliveryLatitude = orderDeliveryLatitude;
        this.orderDeliveryLongitude = orderDeliveryLongitude;
        this.orderTotalMRP = orderTotalMRP;
        this.orderTotalRetailPrice = orderTotalRetailPrice;
        this.orderCouponDiscount = orderCouponDiscount;
        this.orderTotalDiscount = orderTotalDiscount;
        this.orderDeliveryCharges = orderDeliveryCharges;
        this.orderTipAmount = orderTipAmount;
        this.orderSubTotal = orderSubTotal;
        this.orderConvenienceFee = orderConvenienceFee;
        this.orderTotalPayable = orderTotalPayable;
        this.orderTimestamp = orderTimestamp;
    }

    public String getOrderID() {
        return orderID;
    }

    public void setOrderID(String orderID) {
        this.orderID = orderID;
    }

    public String getOrderByUserID() {
        return orderByUserID;
    }

    public void setOrderByUserID(String orderByUserID) {
        this.orderByUserID = orderByUserID;
    }

    public String getOrderCouponApplied() {
        return orderCouponApplied;
    }

    public void setOrderCouponApplied(String orderCouponApplied) {
        this.orderCouponApplied = orderCouponApplied;
    }

    public String getOrderByUserName() {
        return orderByUserName;
    }

    public void setOrderByUserName(String orderByUserName) {
        this.orderByUserName = orderByUserName;
    }

    public String getOrderFromStoreID() {
        return orderFromStoreID;
    }

    public void setOrderFromStoreID(String orderFromStoreID) {
        this.orderFromStoreID = orderFromStoreID;
    }

    public String getOrderFromStoreName() {
        return orderFromStoreName;
    }

    public void setOrderFromStoreName(String orderFromStoreName) {
        this.orderFromStoreName = orderFromStoreName;
    }

    public String getOrderCustomerName() {
        return orderCustomerName;
    }

    public void setOrderCustomerName(String orderCustomerName) {
        this.orderCustomerName = orderCustomerName;
    }

    public String getOrderCustomerMobile() {
        return orderCustomerMobile;
    }

    public void setOrderCustomerMobile(String orderCustomerMobile) {
        this.orderCustomerMobile = orderCustomerMobile;
    }

    public String getOrderDeliveryLocation() {
        return orderDeliveryLocation;
    }

    public void setOrderDeliveryLocation(String orderDeliveryLocation) {
        this.orderDeliveryLocation = orderDeliveryLocation;
    }

    public String getOrderDeliveryAddress() {
        return orderDeliveryAddress;
    }

    public void setOrderDeliveryAddress(String orderDeliveryAddress) {
        this.orderDeliveryAddress = orderDeliveryAddress;
    }

    public String getOrderPaymentMode() {
        return orderPaymentMode;
    }

    public void setOrderPaymentMode(String orderPaymentMode) {
        this.orderPaymentMode = orderPaymentMode;
    }

    public String getOrderInstructions() {
        return orderInstructions;
    }

    public void setOrderInstructions(String orderInstructions) {
        this.orderInstructions = orderInstructions;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getOrderPlacedTime() {
        return orderPlacedTime;
    }

    public void setOrderPlacedTime(String orderPlacedTime) {
        this.orderPlacedTime = orderPlacedTime;
    }

    public String getOrderCompletionTime() {
        return orderCompletionTime;
    }

    public void setOrderCompletionTime(String orderCompletionTime) {
        this.orderCompletionTime = orderCompletionTime;
    }

    public String getOrderCancellationTime() {
        return orderCancellationTime;
    }

    public void setOrderCancellationTime(String orderCancellationTime) {
        this.orderCancellationTime = orderCancellationTime;
    }

    public long getOrderNoOfItems() {
        return orderNoOfItems;
    }

    public void setOrderNoOfItems(long orderNoOfItems) {
        this.orderNoOfItems = orderNoOfItems;
    }

    public double getOrderDeliveryLatitude() {
        return orderDeliveryLatitude;
    }

    public void setOrderDeliveryLatitude(double orderDeliveryLatitude) {
        this.orderDeliveryLatitude = orderDeliveryLatitude;
    }

    public double getOrderDeliveryLongitude() {
        return orderDeliveryLongitude;
    }

    public void setOrderDeliveryLongitude(double orderDeliveryLongitude) {
        this.orderDeliveryLongitude = orderDeliveryLongitude;
    }

    public double getOrderTotalMRP() {
        return orderTotalMRP;
    }

    public void setOrderTotalMRP(double orderTotalMRP) {
        this.orderTotalMRP = orderTotalMRP;
    }

    public double getOrderTotalRetailPrice() {
        return orderTotalRetailPrice;
    }

    public void setOrderTotalRetailPrice(double orderTotalRetailPrice) {
        this.orderTotalRetailPrice = orderTotalRetailPrice;
    }

    public double getOrderCouponDiscount() {
        return orderCouponDiscount;
    }

    public void setOrderCouponDiscount(double orderCouponDiscount) {
        this.orderCouponDiscount = orderCouponDiscount;
    }

    public double getOrderTotalDiscount() {
        return orderTotalDiscount;
    }

    public void setOrderTotalDiscount(double orderTotalDiscount) {
        this.orderTotalDiscount = orderTotalDiscount;
    }

    public double getOrderDeliveryCharges() {
        return orderDeliveryCharges;
    }

    public void setOrderDeliveryCharges(double orderDeliveryCharges) {
        this.orderDeliveryCharges = orderDeliveryCharges;
    }

    public double getOrderTipAmount() {
        return orderTipAmount;
    }

    public void setOrderTipAmount(double orderTipAmount) {
        this.orderTipAmount = orderTipAmount;
    }

    public double getOrderSubTotal() {
        return orderSubTotal;
    }

    public void setOrderSubTotal(double orderSubTotal) {
        this.orderSubTotal = orderSubTotal;
    }

    public double getOrderConvenienceFee() {
        return orderConvenienceFee;
    }

    public void setOrderConvenienceFee(double orderConvenienceFee) {
        this.orderConvenienceFee = orderConvenienceFee;
    }

    public double getOrderTotalPayable() {
        return orderTotalPayable;
    }

    public void setOrderTotalPayable(double orderTotalPayable) {
        this.orderTotalPayable = orderTotalPayable;
    }

    public Timestamp getOrderTimestamp() {
        return orderTimestamp;
    }

    public void setOrderTimestamp(Timestamp orderTimestamp) {
        this.orderTimestamp = orderTimestamp;
    }
}
