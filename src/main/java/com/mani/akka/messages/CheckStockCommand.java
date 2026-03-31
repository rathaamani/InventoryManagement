package com.mani.akka.messages;

import akka.actor.typed.ActorRef;

public class CheckStockCommand implements InventoryCommand {

    public final ActorRef<InventoryCommand.StockResponse> replyTo;

    public CheckStockCommand(ActorRef<InventoryCommand.StockResponse> replyTo) {
        this.replyTo = replyTo;
    }
}
