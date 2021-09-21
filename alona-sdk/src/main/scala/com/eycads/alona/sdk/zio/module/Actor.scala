package com.eycads.alona.sdk.zio.module

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.pattern.StatusReply
import akka.util.Timeout
import com.eycads.alona.sdk.zio.types.ErrorTypes.ZioError
import com.eycads.sdk.protocols.{ActorCommand, CommandWithReplyTo, Event, RelationCommand}
import zio.{Has, ZIO}

object Actor {

  type Actor = Has[Actor.Service]

  type NegPosCommand = RelationCommand

  trait Service {
    def ask[M](target: ActorRef[ShardingEnvelope[M]], entityId: String, message: M)(implicit timeout: Timeout):
      ZIO[Actor, ZioError, Event]
    def askWithStatus[M >: NegPosCommand, E](
      target: ActorRef[ShardingEnvelope[ActorCommand[M]]],
      accountId: String,
      entityId: String,
      message: CommandWithReplyTo
    )(implicit timeout: Timeout): ZIO[Actor, ZioError, E]
    def askWithStatusF[M, E](
      target: ActorRef[ShardingEnvelope[ActorCommand[M]]],
      statusToMessage: ActorRef[StatusReply[E]] => ShardingEnvelope[ActorCommand[M]]
    )(implicit timeout: Timeout): ZIO[Actor, ZioError, E]
    def tell[M](target: ActorRef[ShardingEnvelope[M]], entityId: String, message: M): ZIO[Actor, Throwable, Unit]
    def tell[M](target: ActorRef[M], message: M): ZIO[Actor, ZioError, Unit]
    def tell[M](target: EntityRef[M], entityId: String, message: M): ZIO[Actor, Throwable, Unit]
  }

  def ask[M](target: ActorRef[ShardingEnvelope[M]], entityId: String, message: M)(implicit timeout: Timeout):
      ZIO[Actor, ZioError, Event] = {
    ZIO.accessM(_.get.ask(target, entityId, message))
  }

  def askWithStatus[M >: NegPosCommand, E](
    target: ActorRef[ShardingEnvelope[ActorCommand[M]]],
    accountId: String,
    entityId: String,
    message: CommandWithReplyTo
  )(implicit timeout: Timeout): ZIO[Actor, ZioError, E] = {
    ZIO.accessM(_.get.askWithStatus(target, accountId, entityId, message))
  }

  def askWithStatusF[M, E](
    target: ActorRef[ShardingEnvelope[ActorCommand[M]]],
    statusToMessage: ActorRef[StatusReply[E]] => ShardingEnvelope[ActorCommand[M]]
  )(implicit timeout: Timeout): ZIO[Actor, ZioError, E] = {
    ZIO.accessM(_.get.askWithStatusF(target, statusToMessage))
  }

  def tell[M](target: ActorRef[ShardingEnvelope[M]], entityId: String, message: M): ZIO[Actor, Throwable, Unit] = {
    ZIO.accessM(_.get.tell(target, entityId, message))
  }

  def tell[M](target: ActorRef[M], message: M): ZIO[Actor, ZioError, Unit] = {
    ZIO.accessM(_.get.tell(target, message))
  }

  def tell[M](target: EntityRef[M], entityId: String, message: M): ZIO[Actor, Throwable, Unit] = {
    ZIO.accessM(_.get.tell(target, entityId, message))
  }

}
