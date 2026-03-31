package com.mani.domain;

import java.io.Serializable;
public class Product implements Serializable{
    public final String productId;
    public final String name;
    public final double price;
    public final String category;

    public Product(String productId, String name,double price, String category){
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.category = category;
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }
}
