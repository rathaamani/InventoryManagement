package com.mani.akka.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.mani.akka.messages.InventoryCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductInventoryActor extends AbstractBehavior<InventoryCommand> {
    private static final Logger log = LoggerFactory.getLogger(ProductInventoryActor.class);

    // --- 1. State ---
    public static class State {
        public final String productId;
        public int available;
        public int reserved;
        public int sold;

        public State(String productId, int available, int reserved, int sold) {
            this.productId = productId;
            this.available = available;
            this.reserved = reserved;
            this.sold = sold;
        }
    }

    private final String productId;
    private final State state;

    // --- 2. Factory Setup ---
    public static Behavior<InventoryCommand> create(String productId) {
        return create(productId, 100); // Default initial stock for development
    }

    public static Behavior<InventoryCommand> create(String productId, int initialStock) {
        return Behaviors.setup(context -> new ProductInventoryActor(context, productId, initialStock));
    }

    private ProductInventoryActor(ActorContext<InventoryCommand> context, String productId, int initialStock) {
        super(context);
        this.productId = productId;
        this.state = new State(productId, initialStock, 0, 0);
        log.info("Started Product Inventory Actor for: {} with initial stock: {}", productId, initialStock);
    }

    @Override
    public Receive<InventoryCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(InventoryCommand.CheckStockCommand.class, this::onCheckStock)
                .onMessage(InventoryCommand.ReserveStockCommand.class, this::onReserveStock)
                .onMessage(InventoryCommand.AddStockCommand.class, this::onRestock)
                .onMessage(InventoryCommand.ConfirmOrderCommand.class, this::onConfirmOrder)
                .onMessage(InventoryCommand.CancelOrderCommand.class, this::onCancelOrder)
                .build();
    }

    // --- 3. Message Handlers ---
    private Behavior<InventoryCommand> onCheckStock(InventoryCommand.CheckStockCommand command) {
        command.replyTo.tell(new InventoryCommand.StockResponse(state.productId, state.available, state.reserved, state.sold));
        return this;
    }

    private Behavior<InventoryCommand> onReserveStock(InventoryCommand.ReserveStockCommand command) {
        if (state.available >= command.quantity) {
            state.available -= command.quantity;
            state.reserved += command.quantity;
            log.info("Reserved {} items for product {}. New available: {}", command.quantity, productId, state.available);
            command.replyTo.tell(new InventoryCommand.ReservationResponse(command.orderId, true, "Successfully reserved " + command.quantity + " items."));
        } else {
            log.warn("Insufficient stock for product {}. Requested: {}, Available: {}", productId, command.quantity, state.available);
            command.replyTo.tell(new InventoryCommand.ReservationResponse(command.orderId, false, "Insufficient stock. Only " + state.available + " left."));
        }
        return this;
    }

    private Behavior<InventoryCommand> onRestock(InventoryCommand.AddStockCommand command) {
        state.available += command.quantity;
        log.info("Restocked {} items for product {}. New available: {}", command.quantity, productId, state.available);
        if (command.replyTo != null) {
            command.replyTo.tell(new InventoryCommand.StockResponse(state.productId, state.available, state.reserved, state.sold));
        }
        return this;
    }

    private Behavior<InventoryCommand> onConfirmOrder(InventoryCommand.ConfirmOrderCommand command) {
        state.reserved -= command.quantity;
        state.sold += command.quantity;
        log.info("Confirmed order {} for product {}. New sold: {}", command.orderId, productId, state.sold);
        return this;
    }

    private Behavior<InventoryCommand> onCancelOrder(InventoryCommand.CancelOrderCommand command) {
        state.reserved -= command.quantity;
        state.available += command.quantity;
        log.info("Cancelled order {} for product {}. New available: {}", command.orderId, productId, state.available);
        return this;
    }
}
