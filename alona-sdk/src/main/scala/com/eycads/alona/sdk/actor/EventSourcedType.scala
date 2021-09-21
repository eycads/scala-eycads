package com.eycads.alona.sdk.actor

import akka.actor.typed.scaladsl.{ActorContext, StashBuffer, TimerScheduler}
import akka.persistence.typed.scaladsl.Effect
import com.eycads.alona.sdk.zio.Environment.EventSourcedActorEnv
import com.eycads.alona.sdk.zio.types.ErrorTypes.ZioError
import com.eycads.sdk.protocols.{ActorCommand, StateWithStateDataOnly}
import com.eycads.alona.sdk.zio.Environment.EventSourcedActorEnv
import com.eycads.alona.sdk.zio.module.Logger
import com.eycads.alona.sdk.zio.types.ErrorTypes.{EventSourcedActorError, ZioError}
import scalapb.GeneratedMessage
import zio.ZIO

trait EventSourcedType[C, E, S, SD] {

  type StashBufferC = StashBuffer[C]
  type TimerSchedulerC = TimerScheduler[C]

  type ActorCommandHandler = (S, C) => Effect[E, S]
  type ActorEventHandler = (S, E) => S
  type CommandHandler = S => CommandHandlerFunction
  type CommandHandlerFunction = PartialFunction[(C, SD), ActorResult[E]]

  type ZIOEnv = EventSourcedActorEnv with Logger.Logger

  type CommandHandlerZIO = PartialFunction[S, CommandHandlerFunctionLongZIO]
  type CommandHandlerFunctionLongZIO =
    PartialFunction[(C, SD, S, ActorContext[C], StashBufferC, TimerSchedulerC), ZIO[ZIOEnv, ZioError, ActorResult[E]]]
  type CommandHandlerFunctionZIO =
    PartialFunction[(C, SD), ZIO[ZIOEnv, ZioError, ActorResult[E]]]
  type CommandHandlerFunctionBehaviorZIO =
    PartialFunction[(C, SD, StashBufferC, TimerSchedulerC), ZIO[ZIOEnv, ZioError, ActorResult[E]]]

  type ApplyEventsZIO = PartialFunction[S, ApplyEventsFunctionLongZIO]
  type ApplyEventsFunctionLongZIO = PartialFunction[(E, SD, S), S]
  type ApplyEventsFunctionStateDataOnlyZIO = PartialFunction[(E, SD, StateWithStateDataOnly[SD]), S]
  type ApplyEventsFunctionZIO = PartialFunction[(E, SD), S]

}
