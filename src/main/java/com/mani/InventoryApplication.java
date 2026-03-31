package com.mani;

import akka.actor.typed.ActorSystem;
import com.mani.vertx.InventoryHttpVerticle;
import io.vertx.core.Vertx;

public class InventoryApplication {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println(
                    "Usage: java -cp target/InventoryManagement-1.0-SNAPSHOT.jar com.mani.InventoryApplication <akka-port> <http-port>");
            System.exit(1);
        }

        String akkaPort = args[0];
        int httpPort = Integer.parseInt(args[1]);

        // Bypass Maven Shade plugin dropping the conf file by strictly reading it from
        // disk
        java.io.File confFile = new java.io.File("src/main/resources/inventory.conf");
        com.typesafe.config.Config fileConfig = com.typesafe.config.ConfigFactory.parseFile(confFile);

        com.typesafe.config.Config customConfig = com.typesafe.config.ConfigFactory.parseString(
                "akka.remote.artery.canonical.hostname = \"127.0.0.1\"\n" +
                        "akka.remote.artery.canonical.port = " + akkaPort + "\n")
                .withFallback(fileConfig).withFallback(com.typesafe.config.ConfigFactory.load());

        ActorSystem<Void> system = ActorSystem.create(com.mani.akka.actors.InventoryGuardian.create(),
                "InventoryCluster", customConfig);
        Vertx vertx = Vertx.vertx();

        vertx.deployVerticle(new InventoryHttpVerticle(system, httpPort))
                .onSuccess(id -> {
                    System.out.println("\n════════════════════════════════════════════════════");
                    System.out.println("Inventory Management System Started");
                    System.out.println("Node: " + system.address());
                    System.out.println("HTTP Server natively bound to port: " + httpPort);
                    // printUsage(httpPort);
                    System.out.println("════════════════════════════════════════════════════\n");
                })
                .onFailure(err -> {
                    System.out.println("Failed to start HTTP Server" + err.getMessage());
                    system.terminate();
                });
    }


}
