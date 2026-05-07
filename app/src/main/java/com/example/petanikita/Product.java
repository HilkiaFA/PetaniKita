package com.example.petanikita;

public class Product {
    private int id;
    private String name;
    private String farmName;
    private double price;
    private int stock;

    public Product(int id, String name, String farmName, double price, int stock) {
        this.id = id;
        this.name = name;
        this.farmName = farmName;
        this.price = price;
        this.stock = stock;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getFarmName() { return farmName; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
}