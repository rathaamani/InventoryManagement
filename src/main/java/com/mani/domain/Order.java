package com.mani.domain;

public class Order {
    public final String orderId;
    public final String productId;
    public final int quantity;
    public final long timestamp;
    public final String status;
    public final double totalPrice;

    public Order(String orderId, String productId, int quantity, long timestamp, String status, double totalPrice) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.timestamp = timestamp;
        this.status = status;
        this.totalPrice = totalPrice;
    }

}
