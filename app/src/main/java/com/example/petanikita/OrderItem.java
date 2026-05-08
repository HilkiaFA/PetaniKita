package com.example.petanikita;

public class OrderItem {
    private int orderId;
    private String orderDate;
    private String status;
    private double totalAmount;
    private String shippingAddress;

    public OrderItem(int orderId, String orderDate, String status, double totalAmount, String shippingAddress) {
        this.orderId = orderId;
        this.orderDate = orderDate;
        this.status = status;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
    }

    public int getOrderId() { return orderId; }
    public String getOrderDate() { return orderDate; }
    public String getStatus() { return status; }
    public double getTotalAmount() { return totalAmount; }
    public String getShippingAddress() { return shippingAddress; }
}