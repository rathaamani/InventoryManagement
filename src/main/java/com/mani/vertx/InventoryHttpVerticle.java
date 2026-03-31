package com.mani.vertx;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import com.mani.akka.messages.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

public class InventoryHttpVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(InventoryHttpVerticle.class);
    private final ActorSystem<Void> system;
    private final ClusterSharding sharding;
    private final EntityTypeKey<InventoryCommand> typeKey;
    private final Duration timeout = Duration.ofSeconds(10);
    // Simulated Shared Dataset (Read-Only Catalog)
    private static final java.util.Map<String, String> CATALOG = java.util.Map.of(
            "LAPTOP-001", "Dell XPS 13 - $999.99",
            "PHONE-001", "iPhone 15 - $999.99",
            "TABLET-001", "Samsung Tablet - $299.99",
            "MOUSE-001", "Wireless Mouse - $29.99",
            "KEYBOARD-001", "Mechanical Keyboard - $99.99");

    private final int httpPort;

    public InventoryHttpVerticle(ActorSystem<Void> system, int httpPort) {
        this.system = system;
        this.sharding = ClusterSharding.get(system);
        this.typeKey = EntityTypeKey.create(InventoryCommand.class, "ProductInventory");
        this.httpPort = httpPort;
    }

    public void start(Promise<Void> startPromise) {
        // create Vertx Router to Handle URL endpoints
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());
        router.get("/health").handler(this::onHealthCheck);
        router.get("/inventory/:productId").handler(this::onCheckStock);
        router.get("/products/:productId").handler(this::onGetDetails);
        router.get("/products").handler(this::onListProducts);
        router.post("/orders").handler(this::onCreateOrder);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(httpPort)
                .onSuccess(server -> startPromise.complete())
                .onFailure(startPromise::fail);
    }

    private void onHealthCheck(RoutingContext context) {
        JsonObject response = new JsonObject()
                .put("Status", "healthy")
                .put("System", "Inventory-management")
                .put("timestamp", System.currentTimeMillis());

        context.response()
                .putHeader("content-type", "application/json")
                .end(response.encodePrettily());
    }

    private void onCheckStock(RoutingContext context) {
        // Extract the product ID from the URL (e.g., /inventory/LAPTOP-001)
        String productId = context.pathParam("productId");
        // 1. Find the exact Actor for this product using Sharding
        EntityRef<InventoryCommand> entityRef = sharding.entityRefFor(typeKey, productId);

        // 2. The Ask Pattern: Send a message to the Actor and wait for a response
        CompletionStage<InventoryCommand.StockResponse> reply = AskPattern.ask(
                entityRef,
                replyTo -> new CheckStockCommand(replyTo),
                timeout,
                system.scheduler());
        // 3. When the Actor replies, send the JSON back to the user
        reply.whenComplete((response, failure) -> {
            if (response != null) {
                JsonObject json = new JsonObject()
                        .put("productId", response.productId)
                        .put("available", response.available)
                        .put("reserved", response.reserved);

                context.response()
                        .putHeader("content-type", "application/json")
                        .end(json.encodePrettily());
            } else {
                context.response().setStatusCode(500).end("Actor timeout or failure");
            }
        });
    }

    private void onGetDetails(RoutingContext context) {
        String productId = context.pathParam("productId");
        String details = CATALOG.get(productId);

        if (details != null) {
            JsonObject json = new JsonObject()
                    .put("productId", productId)
                    .put("details", details);
            context.response()
                    .putHeader("content-type", "application/json")
                    .end(json.encodePrettily());
        } else {
            context.response().setStatusCode(404).end("Product not found: " + productId);
        }
    }

    private void onListProducts(RoutingContext context) {
        JsonObject json = new JsonObject();
        CATALOG.forEach(json::put);
        context.response()
                .putHeader("content-type", "application/json")
                .end(json.encodePrettily());
    }

    private void onCreateOrder(RoutingContext context) {
        JsonObject body = context.body().asJsonObject();
        String productId = body.getString("productId");
        int quantity = body.getInteger("quantity", 1);
        String orderId = "ORDER-" + System.currentTimeMillis();

        EntityRef<InventoryCommand> entityRef = sharding.entityRefFor(typeKey, productId);

        CompletionStage<InventoryCommand.ReservationResponse> reply = AskPattern.ask(
                entityRef,
                replyTo -> new InventoryCommand.ReserveStockCommand(orderId, quantity, replyTo),
                timeout,
                system.scheduler());
        reply.whenComplete((response, failure) -> {
            if (response != null) {
                JsonObject json = new JsonObject()
                        .put("orderId", response.orderId)
                        .put("success", response.success)
                        .put("message", response.message);

                context.response()
                        .setStatusCode(response.success ? 200 : 400)
                        .putHeader("content-type", "application/json")
                        .end(json.encodePrettily());
            } else {
                context.response().setStatusCode(500).end("Order processing failed");
            }
        });
    }
}
