package com.application.grocertaxi.Model;

public class Review {
    private String reviewID, comment, byUserID, byUserName;
    private double rating;

    public Review() {
    }

    public Review(String reviewID, String comment, String byUserID, String byUserName, double rating) {
        this.reviewID = reviewID;
        this.comment = comment;
        this.byUserID = byUserID;
        this.byUserName = byUserName;
        this.rating = rating;
    }

    public String getReviewID() {
        return reviewID;
    }

    public void setReviewID(String reviewID) {
        this.reviewID = reviewID;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getByUserID() {
        return byUserID;
    }

    public void setByUserID(String byUserID) {
        this.byUserID = byUserID;
    }

    public String getByUserName() {
        return byUserName;
    }

    public void setByUserName(String byUserName) {
        this.byUserName = byUserName;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }
}
