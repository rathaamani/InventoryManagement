package com.mani.domain;

import java.io.Serializable;

public class InventoryState implements Serializable {
    public final String productId;
    public int quantity;
    public int reserved;
    public int sold;

    public InventoryState(String productId, int initalQuantity) {
        this.productId = productId;
        this.quantity = initalQuantity;
        this.reserved = 0;
        this.sold = 0;
    }
    // calculate how many units are available to buy
    public int available(){
        return quantity - reserved;
    }
    // check if an order cam be fulfilled
    public boolean canOrder(int orderQuantity){
        return available() >=  orderQuantity;
    }
    //Process a reservation
    public void reserve(int orderQuantity) {
        if (canOrder(orderQuantity)) {
            this.reserved += orderQuantity;
        } else {
            throw new IllegalStateException("Insufficient stock for Product: " + productId);
        }
    }
    // Complete an Order: moves from reserved to sold
    public void confirmSale(int orderQuantity){
        if(orderQuantity <= this.reserved) {
            this.reserved -= orderQuantity;
            this.quantity -= orderQuantity;
            this.sold += orderQuantity;
        }else{
            throw new IllegalStateException("Cannot confirm more than what was reserved");
        }
    }

    // Cancel a Reservation: releases reserved stock back to available
    public void cancelReservation(int orderQuantity){
        if(orderQuantity <= this.reserved) {
            this.reserved -= orderQuantity;
        } else {
            throw new IllegalStateException("Cannot cancel more than what was reserved");
        }
    }
}
