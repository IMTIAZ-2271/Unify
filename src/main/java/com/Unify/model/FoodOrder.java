package com.Unify.model;
import java.time.LocalDateTime;

public class FoodOrder {
    private int id;
    private int userId;
    private int foodId;
    private String orderBatchId;
    private String foodName;
    private String canteenName;
    private String status;
    private LocalDateTime orderTime;
    private String userName;

    public FoodOrder() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getFoodId() { return foodId; }
    public void setFoodId(int foodId) { this.foodId = foodId; }
    public String getOrderBatchId() { return orderBatchId; }
    public void setOrderBatchId(String orderBatchId) { this.orderBatchId = orderBatchId; }
    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    public String getCanteenName() { return canteenName; }
    public void setCanteenName(String canteenName) { this.canteenName = canteenName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getOrderTime() { return orderTime; }
    public void setOrderTime(LocalDateTime orderTime) { this.orderTime = orderTime; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
}
