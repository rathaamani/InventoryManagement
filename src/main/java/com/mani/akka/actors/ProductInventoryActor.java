package com.mani.akka.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import com.mani.domain.InventoryState;
import com.mani.akka.messages.InventoryCommand;

public class ProductInventoryActor extends AbstractBehavior<InventoryCommand> {

    private final String productId;
    private final InventoryState state;

    // 1. The Factory Method (How Akka creates this actor)
    public static Behavior<InventoryCommand> create(String productId) {
        return Behaviors.setup(context -> new ProductInventoryActor(context, productId));
    }

    // 2. The Constructor
    private ProductInventoryActor(ActorContext<InventoryCommand> context, String productId) {
        super(context);
        this.productId = productId;
        this.state = new InventoryState(productId, 100);
    }

    // 3. The Message Router
    @Override
    public Receive<InventoryCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(InventoryCommand.CheckStockCommand.class, this::onCheckStock)
                .onMessage(InventoryCommand.ReserveStockCommand.class, this::onReserveStock)
                .onMessage(InventoryCommand.ConfirmOrderCommand.class, this::onConfirmOrder)
                .onMessage(InventoryCommand.CancelOrderCommand.class, this::onCancelOrder)
                .onMessage(InventoryCommand.GetInventoryCommand.class, this::onGetInventory)
                .build();
    }

    // 4. Check Stock
    private Behavior<InventoryCommand> onCheckStock(InventoryCommand.CheckStockCommand command) {
        command.replyTo.tell(new InventoryCommand.StockResponse(productId, state.available(), state.reserved));
        return this;
    }

    // 5. Reserve Stock (place an order)
    private Behavior<InventoryCommand> onReserveStock(InventoryCommand.ReserveStockCommand command) {
        if (state.canOrder(command.quantity)) {
            state.reserve(command.quantity);
            command.replyTo.tell(
                    new InventoryCommand.ReservationResponse(command.orderId, true, "Stock Reserved successfully"));
        } else {
            command.replyTo.tell(new InventoryCommand.ReservationResponse(command.orderId, false,
                    "Insufficient stock: " + state.available() + " available"));
        }
        return this;
    }

    // 6. Confirm an Order (moves reserved stock to sold)
    private Behavior<InventoryCommand> onConfirmOrder(InventoryCommand.ConfirmOrderCommand command) {
        state.confirmSale(command.quantity);
        return this;
    }

    // 7. Cancel an Order (releases reserved stock back to available)
    private Behavior<InventoryCommand> onCancelOrder(InventoryCommand.CancelOrderCommand command) {
        state.reserved -= command.quantity;
        return this;
    }

    // 8. Get full inventory state
    private Behavior<InventoryCommand> onGetInventory(InventoryCommand.GetInventoryCommand command) {
        command.replyTo.tell(new InventoryCommand.InventoryStateResponse(
                productId,
                state.quantity,
                state.reserved,
                state.sold));
        return this;
    }
}
