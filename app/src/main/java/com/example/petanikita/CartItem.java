package com.example.petanikita;

public class CartItem {
    private int cartItemId;
    private int productId;
    private String productName;
    private double price;
    private int quantity;
    private double subTotal;
    private String farmName;
    private String imageUrl;

    public CartItem(int cartItemId, int productId, String productName, double price, int quantity, double subTotal, String farmName, String imageUrl) {
        this.cartItemId = cartItemId;
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.subTotal = subTotal;
        this.farmName = farmName;
        this.imageUrl = imageUrl;
    }

    public int getCartItemId() { return cartItemId; }
    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public double getSubTotal() { return subTotal; }
    public String getFarmName() { return farmName; }
    public String getImageUrl() { return imageUrl; }
}