package com.mani.akka.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.ClusterEvent;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.Subscribe;

public class InventoryClusterMonitor extends AbstractBehavior<ClusterEvent.ClusterDomainEvent> {

    public static Behavior<ClusterEvent.ClusterDomainEvent> create() {
        return  Behaviors.setup(InventoryClusterMonitor::new);
    }

    private InventoryClusterMonitor(ActorContext<ClusterEvent.ClusterDomainEvent> context){
        super(context);

    Cluster cluster = Cluster.get(context.getSystem());

    cluster.subscriptions().tell(Subscribe.create(context.getSelf(), ClusterEvent.ClusterDomainEvent.class));

    context.getLog().info("Cluster Monitor started on node: {}",cluster.selfMember().address());
    }

    @Override
    public Receive<ClusterEvent.ClusterDomainEvent> createReceive(){
        return newReceiveBuilder()
                .onMessage(ClusterEvent.MemberUp.class, this::onMemberUp)
                .onMessage(ClusterEvent.MemberRemoved.class,this::onMemberRemoved)
                .onAnyMessage(event -> Behaviors.same())
                .build();
    }

    private Behavior<ClusterEvent.ClusterDomainEvent>onMemberUp(ClusterEvent.MemberUp event){
        getContext().getLog().info("WAREHOUSE NODE ONLINE: {}", event.member().address());
        return this;
    }

    private Behavior<ClusterEvent.ClusterDomainEvent> onMemberRemoved(ClusterEvent.MemberRemoved event){
        getContext().getLog().warn(" WAREHOUSE NODE OFFLINE: {}", event.member().address());
        return this;
    }
}
