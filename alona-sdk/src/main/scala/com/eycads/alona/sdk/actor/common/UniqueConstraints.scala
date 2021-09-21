package com.eycads.alona.sdk.actor.common

import com.eycads.protocols.sdk.ShardingStrategy
import com.eycads.alona.sdk.actor.interface.EntityRole
import com.eycads.alona.sdk.actor.interface.EntityRole.EntityRole

object UniqueConstraints extends Enumeration {

  type UniqueConstraints = UniqueConstraintsInternalValue

  case class UniqueConstraintsInternalValue(
    code: String,
    actorName: String,
    shardingStrategy: ShardingStrategy,
    role: EntityRole
  ) extends Val(code)

  val ACCOUNT_NAME: UniqueConstraints =
    UniqueConstraintsInternalValue(
      "ACCOUNT_NAME",
      "UniqueAccountNameActor",
      ShardingStrategy.STARTS_WITH,
      EntityRole.UniqueAccountNameActorRole
    )

  val USER_NAME: UniqueConstraints =
    UniqueConstraintsInternalValue(
      "USER_NAME",
      "UniqueUserNameActor",
      ShardingStrategy.STARTS_WITH,
      EntityRole.UniqueUserNameActorRole
    )

  val USER_EMAIL: UniqueConstraints =
    UniqueConstraintsInternalValue(
      "USER_EMAIL",
      "UniqueUserEmailActor",
      ShardingStrategy.STARTS_WITH,
      EntityRole.UniqueUserEmailActorRole
    )

  val USER_NUMBER: UniqueConstraints =
    UniqueConstraintsInternalValue(
      "USER_NUMBER",
      "UniqueUserNumberActor",
      ShardingStrategy.CONTACT_NUMBER,
      EntityRole.UniqueUserNumberActorRole
    )
}
