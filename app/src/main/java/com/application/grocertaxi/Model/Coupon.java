package com.application.grocertaxi.Model;

public class Coupon {
    private String code, description;
    private long discountPercent;

    public Coupon() {
    }

    public Coupon(String code, String description, long discountPercent) {
        this.code = code;
        this.description = description;
        this.discountPercent = discountPercent;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(long discountPercent) {
        this.discountPercent = discountPercent;
    }
}
