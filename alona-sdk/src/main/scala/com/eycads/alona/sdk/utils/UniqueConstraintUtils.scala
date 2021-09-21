package com.eycads.alona.sdk.utils

import akka.actor.typed.{ActorRef, ActorRefResolver, ActorSystem}
import akka.cluster.sharding.typed.ShardingEnvelope
import com.eycads.protocols.sdk.uniqueConstraint.commands.{AddUniqueConstraint, RemoveUniqueConstraint}
import com.eycads.protocols.sdk.{ShardingDetails, ShardingStrategy}
import com.eycads.sdk.protocols.{ActorCommand, UniqueConstraintCommand}
import com.eycads.alona.sdk.actor.common.UniqueConstraints.UniqueConstraints

object UniqueConstraintUtils {

  def getShardingDetails(ss: ShardingStrategy, value: String): Option[ShardingDetails] = {
    Some(
      ShardingDetails(
        shardingStrategy = ss,
        shardingValue = getShardingValue(ss, value)
      )
    )
  }

  //TODO: Add Validation - starts_with must not be empty, contact_number must have valid format
  def getShardingValue(ss: ShardingStrategy, value: String): String = {
    ss match {
      case ShardingStrategy.ID => value
      case ShardingStrategy.STARTS_WITH =>
        value.headOption.fold("")(_.toString)
      case ShardingStrategy.CONTACT_NUMBER =>
        val xs = value.split("-")
        s"${xs(0)}-${xs(1)}"
    }
  }

  def createAddUniqueConstraint[E, AS](
    value: String,
    ownerId: String,
    ref: ActorRef[E],
    uc: UniqueConstraints,
    isStatusReply: Boolean = false,
  )(implicit as: ActorSystem[AS]): ShardingEnvelope[ActorCommand[UniqueConstraintCommand]] = {
    val replyTo = ActorRefResolver(as).toSerializationFormat(ref)
    val shardId = generateShardId(value, ownerId, uc)
    ActorCommand.createShardedMessage(
      "accountId",
      shardId,
      AddUniqueConstraint(
        id = shardId,
        ownerId = ownerId,
        code = uc.code,
        value = value,
        replyTo = Some(replyTo)
      ),
      isStatusReply
    )
  }

  //https://stackoverflow.com/questions/10927218/scala-strange-compile-error-multiple-overloaded-alternatives-of-a-method-defi
  def createAddUniqueConstraintF[E, AS](
    value: String,
    ownerId: String,
    uc: UniqueConstraints,
    isStatusReply: Boolean = false,
  )(implicit as: ActorSystem[AS]): ActorRef[E] => ShardingEnvelope[ActorCommand[UniqueConstraintCommand]] = ( actRef: ActorRef[E]) => {
    createAddUniqueConstraint(value, ownerId, actRef, uc, isStatusReply)
  }

  def createRemoveUniqueConstraint[E, AS](
    value: String,
    ownerId: String,
    ref: ActorRef[E],
    uc: UniqueConstraints,
    isStatusReply: Boolean = false,
  )(implicit as: ActorSystem[AS]): ShardingEnvelope[ActorCommand[UniqueConstraintCommand]] = {
    val replyTo = ActorRefResolver(as).toSerializationFormat(ref)
    val shardId = generateShardId(value, ownerId, uc)
    ActorCommand.createShardedMessage(
      "accountId",
      shardId,
      RemoveUniqueConstraint(
        id = shardId,
        ownerId = ownerId,
        code = uc.code,
        value = value,
        replyTo = Some(replyTo)
      ),
      isStatusReply
    )
  }

  def generateShardId(value: String, ownerId: String, uc: UniqueConstraints): String = {
    uc.shardingStrategy match {
      case ShardingStrategy.ID => value
      case ShardingStrategy.STARTS_WITH =>
        val shardingValue = getShardingValue(uc.shardingStrategy, value)
        s"${uc.code}/$shardingValue/${ownerId}"
    }
  }

  def main(args: Array[String]): Unit = {
    val shardId = getShardingValue(ShardingStrategy.CONTACT_NUMBER, "+63-949-8545058")
    println(shardId)
  }

}
