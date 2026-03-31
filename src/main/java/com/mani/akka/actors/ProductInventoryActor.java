package com.mani.akka.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.javadsl.CommandHandler;
import akka.persistence.typed.javadsl.EventHandler;
import akka.persistence.typed.javadsl.EventSourcedBehavior;
import akka.persistence.typed.javadsl.Effect;
import com.mani.akka.messages.InventoryCommand;

public class ProductInventoryActor extends
        EventSourcedBehavior<InventoryCommand, ProductInventoryActor.Event, ProductInventoryActor.State> {

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

    // --- 2. Events ---
    public interface Event {
    }

    public static final class StockReserved implements Event {
        public final int quantity;

        public StockReserved(int quantity) {
            this.quantity = quantity;
        }
    }

    public static final class StockReplenished implements Event {
        public final int quantity;

        public StockReplenished(int quantity) {
            this.quantity = quantity;
        }
    }

    public static final class OrderConfirmed implements Event {
        public final int quantity;

        public OrderConfirmed(int quantity) {
            this.quantity = quantity;
        }
    }

    public static final class OrderCancelled implements Event {
        public final int quantity;

        public OrderCancelled(int quantity) {
            this.quantity = quantity;
        }
    }

    // --- 3. Factory Setup ---
    public static Behavior<InventoryCommand> create(String productId) {
        return create(productId, 100); // Default initial stock for development
    }

    public static Behavior<InventoryCommand> create(String productId, int initialStock) {
        return new ProductInventoryActor(
                PersistenceId.of("ProductInventory", productId),
                productId,
                initialStock);
    }

    private final String productId;
    private final int initialStock;

    private ProductInventoryActor(PersistenceId persistenceId, String productId, int initialStock) {
        super(persistenceId);
        this.productId = productId;
        this.initialStock = initialStock;
    }

    @Override
    public State emptyState() {
        return new State(productId, initialStock, 0, 0);
    }

    // --- 4. Command Handlers ---
    @Override
    public CommandHandler<InventoryCommand, Event, State> commandHandler() {
        return newCommandHandlerBuilder()
                .forAnyState()
                .onCommand(InventoryCommand.CheckStockCommand.class, this::onCheckStock)
                .onCommand(InventoryCommand.ReserveStockCommand.class, this::onReserveStock)
                .onCommand(InventoryCommand.AddStockCommand.class, this::onRestock)
                .onCommand(InventoryCommand.ConfirmOrderCommand.class, this::onConfirmOrder)
                .onCommand(InventoryCommand.CancelOrderCommand.class, this::onCancelOrder)
                .build();
    }

    private Effect<Event, State> onCheckStock(State state, InventoryCommand.CheckStockCommand command) {
        command.replyTo.tell(new InventoryCommand.StockResponse(state.productId, state.available, state.reserved, state.sold));
        return Effect().none();
    }

    private Effect<Event, State> onReserveStock(State state, InventoryCommand.ReserveStockCommand command) {
        if (state.available >= command.quantity) {
            StockReserved event = new StockReserved(command.quantity);
            return Effect().persist(event).thenRun(() -> {
                command.replyTo.tell(new InventoryCommand.ReservationResponse(command.orderId, true, "Successfully reserved " + command.quantity + " items."));
            });
        } else {
            command.replyTo.tell(new InventoryCommand.ReservationResponse(command.orderId, false, "Insufficient stock. Only " + state.available + " left."));
            return Effect().none();
        }
    }

    private Effect<Event, State> onRestock(State state, InventoryCommand.AddStockCommand command) {
        StockReplenished event = new StockReplenished(command.quantity);
        return Effect().persist(event).thenRun(() -> {
            if (command.replyTo != null) {
                command.replyTo.tell(new InventoryCommand.StockResponse(state.productId, state.available + command.quantity, state.reserved, state.sold));
            }
        });
    }

    private Effect<Event, State> onConfirmOrder(State state, InventoryCommand.ConfirmOrderCommand command) {
        return Effect().persist(new OrderConfirmed(command.quantity));
    }

    private Effect<Event, State> onCancelOrder(State state, InventoryCommand.CancelOrderCommand command) {
        return Effect().persist(new OrderCancelled(command.quantity));
    }

    // --- 5. Event Handlers ---
    @Override
    public EventHandler<State, Event> eventHandler() {
        return newEventHandlerBuilder()
                .forAnyState()
                .onEvent(StockReserved.class, (state, event) -> {
                    state.available -= event.quantity;
                    state.reserved += event.quantity;
                    return state;
                })
                .onEvent(StockReplenished.class, (state, event) -> {
                    state.available += event.quantity;
                    return state;
                })
                .onEvent(OrderConfirmed.class, (state, event) -> {
                    state.reserved -= event.quantity;
                    state.sold += event.quantity;
                    return state;
                })
                .onEvent(OrderCancelled.class, (state, event) -> {
                    state.reserved -= event.quantity;
                    state.available += event.quantity;
                    return state;
                })
                .build();
    }
}
