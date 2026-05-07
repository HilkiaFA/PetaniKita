package com.example.petanikita;

public class CartItem {
    private int cartItemId;
    private int productId;
    private String productName;
    private double price;
    private int quantity;
    private double subTotal;
    private String farmName;

    public CartItem(int cartItemId, int productId, String productName, double price, int quantity, double subTotal, String farmName) {
        this.cartItemId = cartItemId;
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.subTotal = subTotal;
        this.farmName = farmName;
    }

    public int getCartItemId() { return cartItemId; }
    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getSubTotal() { return subTotal; }
    public String getFarmName() { return farmName; }
}