package com.eycads.alona.sdk.actor.interface

object EntityRole extends Enumeration {
  type EntityRole = Value

  val AccountEntityRole: EntityRole.Value = Value("AccountEntityRole")
  val UserEntityRole: EntityRole.Value = Value("UserEntityRole")

  val UniqueAccountNameActorRole: Value = Value("UniqueAccountNameActorRole")
  val UniqueUserNameActorRole: Value = Value("UniqueUserNameActorRole")
  val UniqueUserEmailActorRole: Value = Value("UniqueUserEmailActorRole")
  val UniqueUserNumberActorRole: Value = Value("UniqueUserNumberActorRole")
}
