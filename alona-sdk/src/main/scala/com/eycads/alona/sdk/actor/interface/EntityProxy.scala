package com.eycads.alona.sdk.actor.interface

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import com.eycads.sdk.protocols.{AccountCommand, ActorCommand, UserCommand}

object EntityProxy {

  private def createProxy[C](
    system: ActorSystem[_],
    cs: ClusterSharding,
    typeKey: EntityTypeKey[C],
    role: String
  ): ActorRef[ShardingEnvelope[C]] = {
    cs.init(Entity(typeKey){ctx => Behaviors.empty}.withRole(role))
  }

  def initAccountEntityProxy(system: ActorSystem[_], cs: ClusterSharding): ActorRef[ShardingEnvelope[ActorCommand[AccountCommand]]] =
    createProxy(system, cs, ModuleEntityTypeKey.accountTypeKey, EntityRole.AccountEntityRole.toString)

  def initUserEntityProxy(system: ActorSystem[_], cs: ClusterSharding): ActorRef[ShardingEnvelope[ActorCommand[UserCommand]]] =
    createProxy(system, cs, ModuleEntityTypeKey.userTypeKey, EntityRole.UserEntityRole.toString)

}
