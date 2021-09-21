package com.eycads.sdk.protocols

import akka.cluster.sharding.typed.ShardingEnvelope

import scala.util.Random

case class ApiMeta(accountId: String, userId: String) extends Metadata
case class ActorMeta(
  requestId: String,
  parentChain: String,
  chainId: String,
  accountId: String,
  isStatusReply: Boolean
) extends Metadata

case class ApiRequest(meta: ApiMeta, payload: Request) extends AlonaMessage[Request]
case class ActorCommand[+C](meta: ActorMeta, payload: C) extends AlonaMessage[C]
case class ActorEvent(meta: ActorMeta, payload: Event) extends AlonaMessage[Event]

object ActorCommand {

  def create[C](
    accountId: String,
    payload: C,
    isStatusReply: Boolean
  ): ActorCommand[C] = {
    val parentChain = "***"
    val chainId = Random.alphanumeric.take(8).mkString
    ActorCommand(
      meta = ActorMeta(
        java.util.UUID.randomUUID.toString,
        parentChain,
        chainId,
        accountId,
        isStatusReply = isStatusReply
      ),
      payload = payload
    )
  }

  def create[C](
    accountId: String,
    payload: C,
  ): ActorCommand[C] = {
    create(accountId, payload, isStatusReply = false)
  }

  def createCreateCommand(accountId: String, payload: CreateCommand): ActorCommand[CreateCommand] = {
    create(accountId, payload, isStatusReply = false)
  }

  def createShardedMessage[C](
    accountId: String,
    entityId: String,
    payload: C,
    isStatusReply: Boolean
  ): ShardingEnvelope[ActorCommand[C]] = {
    ShardingEnvelope(
      entityId,
      create(accountId, payload, isStatusReply)
    )
  }

}

