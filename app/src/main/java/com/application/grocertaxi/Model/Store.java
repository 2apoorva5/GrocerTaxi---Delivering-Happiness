package com.application.grocertaxi.Model;

import com.google.firebase.Timestamp;

public class Store {
    private String storeID, storeName, storeOwner, storeEmail, storeMobile, storeLocation, storeAddress,
            storeTiming, storeImage, storeSearchKeyword;
    private Timestamp storeTimestamp;
    private boolean storeStatus;
    private double storeLatitude, storeLongitude, storeMinimumOrderValue, storeAverageRating;

    public Store() {
    }

    public Store(String storeID, String storeName, String storeOwner, String storeEmail, String storeMobile,
                 String storeLocation, String storeAddress, String storeTiming, String storeImage,
                 String storeSearchKeyword, Timestamp storeTimestamp, boolean storeStatus, double storeLatitude,
                 double storeLongitude, double storeMinimumOrderValue, double storeAverageRating) {
        this.storeID = storeID;
        this.storeName = storeName;
        this.storeOwner = storeOwner;
        this.storeEmail = storeEmail;
        this.storeMobile = storeMobile;
        this.storeLocation = storeLocation;
        this.storeAddress = storeAddress;
        this.storeTiming = storeTiming;
        this.storeImage = storeImage;
        this.storeSearchKeyword = storeSearchKeyword;
        this.storeTimestamp = storeTimestamp;
        this.storeStatus = storeStatus;
        this.storeLatitude = storeLatitude;
        this.storeLongitude = storeLongitude;
        this.storeMinimumOrderValue = storeMinimumOrderValue;
        this.storeAverageRating = storeAverageRating;
    }

    public String getStoreID() {
        return storeID;
    }

    public void setStoreID(String storeID) {
        this.storeID = storeID;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getStoreOwner() {
        return storeOwner;
    }

    public void setStoreOwner(String storeOwner) {
        this.storeOwner = storeOwner;
    }

    public String getStoreEmail() {
        return storeEmail;
    }

    public void setStoreEmail(String storeEmail) {
        this.storeEmail = storeEmail;
    }

    public String getStoreMobile() {
        return storeMobile;
    }

    public void setStoreMobile(String storeMobile) {
        this.storeMobile = storeMobile;
    }

    public String getStoreLocation() {
        return storeLocation;
    }

    public void setStoreLocation(String storeLocation) {
        this.storeLocation = storeLocation;
    }

    public String getStoreAddress() {
        return storeAddress;
    }

    public void setStoreAddress(String storeAddress) {
        this.storeAddress = storeAddress;
    }

    public String getStoreTiming() {
        return storeTiming;
    }

    public void setStoreTiming(String storeTiming) {
        this.storeTiming = storeTiming;
    }

    public String getStoreImage() {
        return storeImage;
    }

    public void setStoreImage(String storeImage) {
        this.storeImage = storeImage;
    }

    public String getStoreSearchKeyword() {
        return storeSearchKeyword;
    }

    public void setStoreSearchKeyword(String storeSearchKeyword) {
        this.storeSearchKeyword = storeSearchKeyword;
    }

    public Timestamp getStoreTimestamp() {
        return storeTimestamp;
    }

    public void setStoreTimestamp(Timestamp storeTimestamp) {
        this.storeTimestamp = storeTimestamp;
    }

    public boolean isStoreStatus() {
        return storeStatus;
    }

    public void setStoreStatus(boolean storeStatus) {
        this.storeStatus = storeStatus;
    }

    public double getStoreLatitude() {
        return storeLatitude;
    }

    public void setStoreLatitude(double storeLatitude) {
        this.storeLatitude = storeLatitude;
    }

    public double getStoreLongitude() {
        return storeLongitude;
    }

    public void setStoreLongitude(double storeLongitude) {
        this.storeLongitude = storeLongitude;
    }

    public double getStoreMinimumOrderValue() {
        return storeMinimumOrderValue;
    }

    public void setStoreMinimumOrderValue(double storeMinimumOrderValue) {
        this.storeMinimumOrderValue = storeMinimumOrderValue;
    }

    public double getStoreAverageRating() {
        return storeAverageRating;
    }

    public void setStoreAverageRating(double storeAverageRating) {
        this.storeAverageRating = storeAverageRating;
    }
}
