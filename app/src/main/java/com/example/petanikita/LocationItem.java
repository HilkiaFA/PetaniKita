package com.example.petanikita;

public class LocationItem {
    private int id;
    private String name;

    public LocationItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return name;
    }
}