package com.eycads.alona.sdk.zio.impl.actor

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ActorRef, ActorRefResolver, Scheduler}
import akka.actor.typed.scaladsl.AskPattern._
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.pattern.StatusReply
import akka.util.Timeout
import com.eycads.sdk.protocols.{ActorCommand, CommandWithReplyTo, Event, RelationCommand}
import com.eycads.alona.sdk.zio.module.Actor
import com.eycads.alona.sdk.zio.module.Actor.{Actor, NegPosCommand}
import com.eycads.alona.sdk.zio.types.ErrorTypes.{AskError, TellError, ZioError}
import zio.{IO, Layer, RIO, ZIO, ZLayer}

class AkkaActor[AC](context: ActorContext[AC])(implicit timeout: Timeout, scheduler: Scheduler) extends Actor.Service {

//  implicit class CommandOps[C <: CommandWithReplyTo](cmd: C) {
//    def wew[CC <: C](actRef: String): CC = cmd.withReplyTo(actRef)
//  }

  override def ask[M](
    target: ActorRef[ShardingEnvelope[M]],
    entityId: String,
    message: M
  )(implicit timeout: Timeout): ZIO[Actor, ZioError, Event] = {
    for {
      // TODO: check if context would matter since this context is from server
      futureResponse <- IO.effectTotal(target.ask[Event](ref => {
//        AkkaActor.messageWithReplyTo(entityId, message, ref, context)\
        ShardingEnvelope(entityId, message)
      }))
      res <- RIO.fromFuture(implicit ec => futureResponse).mapError(err => AskError(err))
    } yield res
  }

  override def askWithStatus[M >: NegPosCommand, E](
    target: ActorRef[ShardingEnvelope[ActorCommand[M]]],
    accountId: String,
    entityId: String,
    message: CommandWithReplyTo
  )(implicit timeout: Timeout): ZIO[Actor, ZioError, E] = {
    val targetUpcast: ActorRef[ShardingEnvelope[ActorCommand[CommandWithReplyTo]]] = target.unsafeUpcast
    for {
      futureResponse <- IO.effectTotal(targetUpcast.askWithStatus[E](ref => {
        val actRef = ActorRefResolver(context.system).toSerializationFormat(ref)
        ActorCommand.createShardedMessage(
          accountId,
          entityId,
          message.withReplyTo(actRef),
          isStatusReply = true
        )
      }))
      res <- RIO.fromFuture(implicit ec => futureResponse).mapError(err => AskError(err))
    } yield res
  }

  override def askWithStatusF[M, E](
    target: ActorRef[ShardingEnvelope[ActorCommand[M]]],
    statusToMessage: ActorRef[StatusReply[E]] => ShardingEnvelope[ActorCommand[M]]
  )(implicit timeout: Timeout): ZIO[Actor, ZioError, E] = {
    for {
      futureResponse <- IO.effectTotal(
        target.askWithStatus[E](ref => {
          statusToMessage(ref)
        })
      )
      res <- RIO.fromFuture(implicit ec => futureResponse).mapError(err => AskError(err))
    } yield res
  }

  override def tell[M](target: ActorRef[ShardingEnvelope[M]], entityId: String, message: M): ZIO[Actor, Throwable, Unit] = {
    for {
      res <- IO.effect(target.tell(ShardingEnvelope(entityId, message)))
        .mapError(err => TellError(err).toThrowable)
    } yield res
  }

  override def tell[M](target: ActorRef[M], message: M): ZIO[Actor, ZioError, Unit] = {
    for {
      res <- IO.effect(target.tell(message))
        .mapError(err => TellError(err))
    } yield res
  }

  override def tell[M](target: EntityRef[M], entityId: String, message: M): ZIO[Actor, Throwable, Unit] = {
    for {
      res <- IO.effect(target.tell(message))
        .mapError(err => TellError(err).toThrowable)
    } yield res
  }

}

object AkkaActor {

  def messageWithReplyTo[M, ARM, AC](message: M, ref: ActorRef[ARM], context: ActorContext[AC]): CommandWithReplyTo = {
    val actRef = ActorRefResolver(context.system).toSerializationFormat(ref)
    message match {
      case cmd: CommandWithReplyTo => cmd.withReplyTo(actRef)
    }
  }

  private def live[AC](context: ActorContext[AC])(implicit timeout: Timeout, scheduler: Scheduler): Layer[ZioError, Actor] =
    ZLayer.succeed[Actor.Service](new AkkaActor(context))

  def createLive[AC](context: ActorContext[AC])(implicit timeout: Timeout, scheduler: Scheduler): Layer[ZioError, Actor] =
    live(context)

}
