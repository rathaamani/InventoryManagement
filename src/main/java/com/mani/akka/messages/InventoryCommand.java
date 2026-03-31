package com.mani.akka.messages;

import java.io.Serializable;
import akka.actor.typed.ActorRef;

// The Parent Interface
public interface InventoryCommand extends Serializable {

    // Ask the akka for current stock
    public static class CheckStockCommand implements InventoryCommand {
        public final ActorRef<StockResponse> replyTo; // Added final

        public CheckStockCommand(ActorRef<StockResponse> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static class StockResponse implements Serializable {
        public final String productId;
        public final int available;
        public final int reserved;
        public final int sold;

        public StockResponse(String productId, int available, int reserved, int sold) {
            this.productId = productId;
            this.available = available;
            this.reserved = reserved;
            this.sold = sold;
        }
    }

    public static class AddStockCommand implements InventoryCommand {
        public final int quantity;
        public final ActorRef<StockResponse> replyTo;

        public AddStockCommand(int quantity, ActorRef<StockResponse> replyTo) {
            this.quantity = quantity;
            this.replyTo = replyTo;
        }
    }

    public static class ReserveStockCommand implements InventoryCommand {
        public final String orderId;
        public final int quantity;
        public final ActorRef<ReservationResponse> replyTo;

        public ReserveStockCommand(String orderId, int quantity, ActorRef<ReservationResponse> replyTo) {
            this.orderId = orderId;
            this.quantity = quantity;
            this.replyTo = replyTo;
        }
    }

    public static class ReservationResponse implements Serializable {
        public final String orderId;
        public final boolean success;
        public final String message;

        public ReservationResponse(String orderId, boolean success, String message) {
            this.orderId = orderId;
            this.success = success;
            this.message = message;
        }
    }

    public static class ConfirmOrderCommand implements InventoryCommand {
        public final String orderId;
        public final int quantity;

        public ConfirmOrderCommand(String orderId, int quantity) {
            this.orderId = orderId;
            this.quantity = quantity;
        }
    }

    public static class CancelOrderCommand implements InventoryCommand {
        public final String orderId;
        public final int quantity;

        public CancelOrderCommand(String orderId, int quantity) {
            this.orderId = orderId;
            this.quantity = quantity;
        }
    }

    public static class GetInventoryCommand implements InventoryCommand {
        public final ActorRef<InventoryStateResponse> replyTo;

        public GetInventoryCommand(ActorRef<InventoryStateResponse> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static class InventoryStateResponse implements Serializable {
        public final String productId;
        public final int quantity;
        public final int reserved;
        public final int sold;

        public InventoryStateResponse(String productId, int quantity, int reserved, int sold) {
            this.productId = productId;
            this.quantity = quantity;
            this.reserved = reserved;
            this.sold = sold;
        }
    }
}