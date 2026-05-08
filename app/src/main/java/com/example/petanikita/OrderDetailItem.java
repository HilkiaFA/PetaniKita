package com.example.petanikita;

public class OrderDetailItem {
    private int productId;
    private String productName;
    private int quantity;
    private double price;
    private double subTotal;

    public OrderDetailItem(int productId, String productName, int quantity, double price, double subTotal) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
        this.subTotal = subTotal;
    }

    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public double getSubTotal() { return subTotal; }
}