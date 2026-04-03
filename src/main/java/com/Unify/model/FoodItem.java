package com.Unify.model;

public class FoodItem {
    private int id;
    private int canteenId;
    private String canteenName; // Useful for the UI table
    private String name;
    private double price;
    private int availableQty;
    private int cartQuantity = 0;

    public FoodItem() {}

    public int getCartQuantity() { return cartQuantity; }
    public void setCartQuantity(int cartQuantity) { this.cartQuantity = cartQuantity; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCanteenId() { return canteenId; }
    public void setCanteenId(int canteenId) { this.canteenId = canteenId; }
    public String getCanteenName() { return canteenName; }
    public void setCanteenName(String canteenName) { this.canteenName = canteenName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getAvailableQty() { return availableQty; }
    public void setAvailableQty(int availableQty) { this.availableQty = availableQty; }
}