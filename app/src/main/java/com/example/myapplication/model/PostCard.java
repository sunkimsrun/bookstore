package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class PostCard {

    @SerializedName("userId")
    private String userId;

    @SerializedName("postId")
    private String postId;

    @SerializedName("title")
    private String title;

    @SerializedName("information")
    private String information;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("price")
    private String price;

    @SerializedName("status")
    private String status;

    @SerializedName("date")
    private String date;

    @SerializedName("time")
    private String time;

    @SerializedName("createdDate")
    private String createdDate;

    @SerializedName("genre")
    private String genre;

    @SerializedName("location")
    private String location;

    public PostCard() {
    }

    public PostCard(String userId, String postId, String title, String information, String email,
                    String phone, String imageUrl, String price, String date, String time,
                    String createdDate, String status, String genre, String location) {
        this.userId = userId;
        this.postId = postId;
        this.title = title;
        this.information = information;
        this.email = email;
        this.phone = phone;
        this.imageUrl = imageUrl;
        this.price = price;
        this.date = date;
        this.time = time;
        this.createdDate = createdDate;
        this.status = status;
        this.genre = genre;
        this.location = location;
    }

    // --- Getters & Setters ---
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getInformation() { return information; }
    public void setInformation(String information) { this.information = information; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getPostTime() { return time; }
    public void setPostTime(String time) { this.time = time; }

    public String getCreatedDate() { return createdDate != null ? createdDate : "Unknown"; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    @Override
    public String toString() {
        return String.format(
                "PostCard{ imageUrl=%s, postId=%s, title=%s, information=%s, email=%s, phone=%s, price=%s, date=%s, time=%s, status=%s, createdDate=%s, genre=%s, location=%s }",
                imageUrl, postId, title, information, email, phone, price, date, time, status, getCreatedDate(), genre, location
        );
    }
}