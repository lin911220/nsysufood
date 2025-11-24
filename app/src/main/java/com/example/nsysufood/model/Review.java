package com.example.nsysufood.model;

public class Review {
    private String reviewId;
    private String userId;
    private String userName; // 顯示名稱 (例如 email 前綴)
    private float rating;    // 星星數 (1.0 - 5.0)
    private String comment;  // 留言內容
    private long timestamp;  // 留言時間

    public Review() { } // Firebase 需要空建構子

    public Review(String reviewId, String userId, String userName, float rating, String comment, long timestamp) {
        this.reviewId = reviewId;
        this.userId = userId;
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    public String getReviewId() { return reviewId; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public float getRating() { return rating; }
    public String getComment() { return comment; }
    public long getTimestamp() { return timestamp; }
}