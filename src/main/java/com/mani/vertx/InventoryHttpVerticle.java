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
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.core.http.HttpMethod;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import io.vertx.core.json.JsonArray;

public class InventoryHttpVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(InventoryHttpVerticle.class);
    private final ActorSystem<Void> system;
    private final ClusterSharding sharding;
    private final EntityTypeKey<InventoryCommand> typeKey;
    private final Duration timeout = Duration.ofSeconds(10);
    // Mutable Shared Dataset (Read-Write Catalog)
    private static final java.util.Map<String, String> CATALOG = new ConcurrentHashMap<>(java.util.Map.of(
            "LAPTOP-001", "Dell XPS 13 - $999.99",
            "PHONE-001", "iPhone 15 - $999.99",
            "TABLET-001", "Samsung Tablet - $299.99",
            "MOUSE-001", "Wireless Mouse - $29.99",
            "KEYBOARD-001", "Mechanical Keyboard - $99.99"));

    // In-memory order log for historical record
    private static final List<JsonObject> ORDERS_LOG = new CopyOnWriteArrayList<>();

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
        router.route().handler(CorsHandler.create()
                .addOrigin("http://localhost:4200") // Explicitly allow Angular's port
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.POST)
                .allowedMethod(HttpMethod.OPTIONS)
                .allowedHeader("Access-Control-Request-Method")
                .allowedHeader("Access-Control-Allow-Credentials")
                .allowedHeader("Access-Control-Allow-Origin")
                .allowedHeader("Access-Control-Allow-Headers")
                .allowedHeader("Content-Type"));

        router.route().handler(BodyHandler.create());
        router.get("/health").handler(this::onHealthCheck);
        router.get("/inventory/:productId").handler(this::onCheckStock);
        router.get("/products/:productId").handler(this::onGetDetails);
        router.get("/products").handler(this::onListProducts);
        router.post("/products").handler(this::onAddProduct);
        router.post("/orders").handler(this::onCreateOrder);
        router.get("/orders").handler(this::onListOrders);

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
                replyTo -> new InventoryCommand.CheckStockCommand(replyTo),
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
        JsonArray array = new JsonArray();
        CATALOG.forEach((id, details) -> {
            array.add(new JsonObject()
                    .put("productId", id)
                    .put("details", details));
        });
        context.response()
                .putHeader("content-type", "application/json")
                .end(array.encodePrettily());
    }

    private void onAddProduct(RoutingContext context) {
        JsonObject body = context.body().asJsonObject();
        if (body == null) {
            context.response().setStatusCode(400).end("Missing JSON body");
            return;
        }

        String productId = body.getString("productId");
        String details = body.getString("details");
        int initialStock = body.getInteger("initialStock", 100);

        if (productId == null || details == null) {
            context.response().setStatusCode(400).end("Missing productId or details");
            return;
        }

        // Add to catalog
        CATALOG.put(productId, details);

        // Initialize Stock in Akka - We can do this lazily or explicitly
        // Here we just trigger an "AddStock" with 0 or the initial stock to ensure the actor is warmed up
        EntityRef<InventoryCommand> entityRef = sharding.entityRefFor(typeKey, productId);
        entityRef.tell(new InventoryCommand.AddStockCommand(initialStock, null));

        context.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json")
                .end(new JsonObject()
                        .put("status", "success")
                        .put("message", "Product added and stock initialized")
                        .put("productId", productId)
                        .encodePrettily());
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
                        .put("message", response.message)
                        .put("productId", productId)
                        .put("quantity", quantity)
                        .put("timestamp", System.currentTimeMillis());

                if (response.success) {
                    ORDERS_LOG.add(json); // Store successful order in memory
                }

                context.response()
                        .setStatusCode(response.success ? 200 : 400)
                        .putHeader("content-type", "application/json")
                        .end(json.encodePrettily());
            } else {
                log.error("Failed to create order for {}: {}", productId, (failure != null ? failure.getMessage() : "Timeout"));
                context.response().setStatusCode(500).end("Order processing failed: " + (failure != null ? failure.getMessage() : "Actor timeout"));
            }
        });
    }

    private void onListOrders(RoutingContext context) {
        JsonArray array = new JsonArray(ORDERS_LOG);
        context.response()
                .putHeader("content-type", "application/json")
                .end(array.encodePrettily());
    }
}
