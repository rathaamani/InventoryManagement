package com.mani.akka.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import com.mani.akka.messages.InventoryCommand;

public class InventoryGuardian {

    public static Behavior<Void> create(){
        return Behaviors.setup(context -> {
            context.spawn(InventoryClusterMonitor.create(),"Cluster-monitor");

            EntityTypeKey<InventoryCommand> typeKey = EntityTypeKey.create(InventoryCommand.class, "ProductInventory");

            ClusterSharding.get(context.getSystem()).init(
                    Entity.of(typeKey, entityContext ->
                            ProductInventoryActor.create(entityContext.getEntityId())
                    )
            );
            return Behaviors.empty();
        });
    }
}
