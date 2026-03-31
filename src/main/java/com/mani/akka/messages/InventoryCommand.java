package com.mani.akka.messages;

import java.io.Serializable;
import akka.actor.typed.ActorRef;

// The Parent Interface
public interface InventoryCommand extends Serializable {
    // Ask the akka for current stock
    class CheckStockCommand implements InventoryCommand{
        public ActorRef<StockResponse> replyTo;

        public CheckStockCommand(ActorRef<StockResponse> replyTo) {
            this.replyTo = replyTo;
        }
    }
    class StockResponse implements Serializable{
        public final String productId;
        public final int available;
        public final int reserved;

        public StockResponse(String productId, int available, int reserved) {
            this.productId = productId;
            this.available = available;
            this.reserved = reserved;
        }
    }

    class ReserveStockCommand implements InventoryCommand {
        public final String orderId;
        public final int quantity;
        public final ActorRef<ReservationResponse> replyTo;

        public ReserveStockCommand(String orderId, int quantity, ActorRef<ReservationResponse> replyTo) {
            this.orderId = orderId;
            this.quantity = quantity;
            this.replyTo = replyTo;
        }
    }
    class ReservationResponse implements Serializable{
        public final String orderId;
        public final boolean success;
        public final String message;

        public ReservationResponse(String orderId, boolean success, String message) {
            this.orderId = orderId;
            this.success = success;
            this.message = message;
        }
    }
    class ConfirmOrderCommand implements InventoryCommand {
        public final String orderId;
        public final int quantity;

        public ConfirmOrderCommand(String orderId, int quantity){
             this.orderId = orderId;
             this.quantity = quantity;
        }
    }
    class CancelOrderCommand implements InventoryCommand {
         public final String orderId;
         public final int quantity;

        public CancelOrderCommand(String orderId, int quantity) {
            this.orderId = orderId;
            this.quantity = quantity;
        }

    }
    // Command: Get the full detailed inventory state
    class GetInventoryCommand implements InventoryCommand {
        public final ActorRef<InventoryStateResponse> replyTo;

        public GetInventoryCommand(ActorRef<InventoryStateResponse> replyTo) {
            this.replyTo = replyTo;
        }
    }
    class InventoryStateResponse implements Serializable{
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
