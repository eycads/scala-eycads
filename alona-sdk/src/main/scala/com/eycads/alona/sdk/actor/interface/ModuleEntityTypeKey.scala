package com.eycads.alona.sdk.actor.interface

import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import com.eycads.sdk.protocols.{AccountCommand, ActorCommand, Command, RelationCommand, UniqueConstraintCommand, UserCommand}

object ModuleEntityTypeKey {

  val accountTypeKey: EntityTypeKey[ActorCommand[AccountCommand]] =
    EntityTypeKey[ActorCommand[AccountCommand]]("AccountEntity")
  val userTypeKey: EntityTypeKey[ActorCommand[UserCommand]] =
    EntityTypeKey[ActorCommand[UserCommand]]("UserEntity")

  val uniqueConstraintTypeKey: EntityTypeKey[ActorCommand[UniqueConstraintCommand]] =
    EntityTypeKey[ActorCommand[UniqueConstraintCommand]]("UniqueConstraintActor")

  val testTypeKey: EntityTypeKey[ActorCommand[UserCommand]] =
    EntityTypeKey[ActorCommand[UserCommand]]("UserEntity")

  val entityTypeKeyMap: Map[Int, EntityTypeKey[ActorCommand[RelationCommand]]] =
    Map(
      1 -> accountTypeKey,
      2 -> testTypeKey
    )

}
