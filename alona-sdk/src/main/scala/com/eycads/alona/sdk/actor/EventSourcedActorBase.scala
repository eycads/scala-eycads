package com.eycads.alona.sdk.actor

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, ActorRefResolver, ActorSystem, BackoffSupervisorStrategy, Behavior, SupervisorStrategy}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EffectBuilder, EventSourcedBehavior, RetentionCriteria, SnapshotCountRetentionCriteria}
import com.eycads.alona.sdk.error.Errors.EmptyReplyTo
import com.eycads.sdk.protocols.{ActorCommand, Command, Event, StateData, StateWithData, StateWithStateDataOnly}
import com.eycads.alona.sdk.zio.Environment.EventSourcedActorEnv
import com.eycads.alona.sdk.zio.impl.logger.ScalaLogger
import com.eycads.alona.sdk.zio.module.Actor.Actor
import com.eycads.alona.sdk.zio.module.Logger.Logger
import com.eycads.alona.sdk.zio.module.{Actor, Logger}
import com.eycads.alona.sdk.zio.types.ErrorTypes.{EventSourcedActorError, ZioError}
import zio.{IO, Layer, RIO, Runtime, ZIO}

import scala.concurrent.duration.DurationInt
import scala.collection.mutable

trait EventSourcedActorBase[C <: Command, E <: Event, S <: StateWithData, SD <: StateData]
    extends EventSourcedType[ActorCommand[C], Event, StateWithData, SD]
    with ActorBase {

  type ActorContextC[CC] = ActorContext[ActorCommand[CC]]

  /**
   * @param name - name of the Entity
   * @param initialState - initial StateData of the Entity
   * @param onPersistStrategy - EventSourcedBehavior.onPersistFailure - in case overriding the default
   * @param retentionCriteria - EventSourcedBehavior.withRetention - in case overriding the default
   */
  case class EventSourcedActorSettings(
    name: String,
    initialState: S,
    logger: com.typesafe.scalalogging.Logger,
    onPersistStrategy: Option[BackoffSupervisorStrategy] = None,
    retentionCriteria: Option[SnapshotCountRetentionCriteria] = None
  )

  val relationInitialState: Option[S] = None
  private var handlersMap: mutable.Map[String, CommandHandlerFunctionLongZIO] = mutable.Map.empty
  private var applyEventsMap: mutable.Map[String, ApplyEventsFunctionLongZIO] = mutable.Map.empty

  def settings: EventSourcedActorSettings
  def handlers(implicit ctx: ActorContextC[C]): Map[StateKey[S], CommandHandlerFunctionLongZIO]
  def applyEvents: Map[StateKey[S], ApplyEventsFunctionLongZIO]

  val runtime: Runtime[zio.ZEnv] = Runtime.default

  def behavior(system: ActorSystem[Nothing])(implicit customLayer: Layer[Any, EventSourcedActorEnv]): Behavior[ActorCommand[C]] = {
    Behaviors.setup { ctx =>
      Behaviors.withStash(1000) { stash =>
        Behaviors.withTimers { timers =>
          implicit val actorContext: ActorContextC[C] = ctx
          implicit val timerScheduler: TimerSchedulerC = timers
          implicit val stashBuffer: StashBufferC = stash
          implicit val zioEnvLayer: Layer[Any, ZIOEnv] = customLayer ++ ScalaLogger.createLive(settings.logger)
          handlers.foreach{ case (k, v) => when(k)(v) }
          applyEvents.foreach{ case (k, v) => applyEventsAt(k)(v) }
          eventSourcedBehavior
        }
      }
    }
  }

  /**
   * when - Append PartialFunction to a specified state and if it non-existing,
   * will add it to mutable handlersMap
   *
   * @param state
   * @param handlerFunction
   */
  final def when(state: StateKey[S])(handlerFunction: CommandHandlerFunctionLongZIO): Unit = {
    val key = state.key
    if (handlersMap.isDefinedAt(key)){
      handlersMap(key) = handlersMap(key).orElse(handlerFunction)
    } else {
      handlersMap = handlersMap ++ Map(key -> handlerFunction)
    }
  }

  private final def whenMini(state: StateKey[S])(handlerFunction: CommandHandlerFunctionZIO): Unit = {
    val key = state.key
    if (handlersMap.isDefinedAt(key)){
      handlersMap(key) = handlersMap(key).orElse(convertCommandHandlerMini(handlerFunction))
    } else {
      handlersMap = handlersMap ++ Map(key -> convertCommandHandlerMini(handlerFunction))
    }
  }

  final def convertCommandHandlerMini(mini: CommandHandlerFunctionZIO): CommandHandlerFunctionLongZIO = {
    case (c, sd, state, ctx, stash, timer) if mini.isDefinedAt(c, sd) => mini(c, sd)
  }

  final def convertCommandHandlerBehavior(behavior: CommandHandlerFunctionBehaviorZIO): CommandHandlerFunctionLongZIO = {
    case (c, sd, state, ctx, stash, timer) if behavior.isDefinedAt(c, sd, stash, timer) =>
      behavior(c, sd, stash, timer)
  }

  /**
   * registerCommandHandler - Register new map of handlers to mutable handlersMap
   *
   * @param newHandler
   */
  final def registerCommandHandler(newHandler: Map[StateKey[S], CommandHandlerFunctionLongZIO]): Unit = {
    newHandler.foreach{ case (k, v) => when(k)(v) }
  }

  /**
   * applyEventsAt - Append PartialFunction to a specified state and if it is non-existing,
   * it will add to mutable applyEventsMap
   *
   * @param state
   * @param applyEventFunction
   */
  final def applyEventsAt(state: StateKey[S])(applyEventFunction: ApplyEventsFunctionLongZIO): Unit = {
    val key = state.key
    if (applyEventsMap.isDefinedAt(key)){
      applyEventsMap(key) = applyEventsMap(key).orElse(applyEventFunction)
    } else {
      applyEventsMap = applyEventsMap ++ Map(key -> applyEventFunction)
    }
  }

  private final def applyEventsMiniAt(state: StateKey[S])(applyEventFunction: ApplyEventsFunctionZIO): Unit = {
    val key = state.key
    if (applyEventsMap.isDefinedAt(key)){
      applyEventsMap(key) = applyEventsMap(key).orElse(convertApplyEventMini(applyEventFunction))
    } else {
      applyEventsMap = applyEventsMap ++ Map(key -> convertApplyEventMini(applyEventFunction))
    }
  }

  final def convertApplyEventMini(mini: ApplyEventsFunctionZIO): ApplyEventsFunctionLongZIO = {
    case (e, sd, state) if mini.isDefinedAt(e, sd) => mini(e, sd).withRelationData(state.relationData)
  }

  final def convertApplyEventStateDataOnly(stateDataOnly: ApplyEventsFunctionStateDataOnlyZIO): ApplyEventsFunctionLongZIO = {
    case (e, sd, state: StateWithStateDataOnly[SD]) if stateDataOnly.isDefinedAt((e, sd, state)) => stateDataOnly((e, sd, state))
  }

  /**
   * registerApplyEvents - Register new map of applyEvents to mutable applyEventsMap
   *
   * @param newApplyEvents
   */
  final def registerApplyEvents(newApplyEvents: Map[StateKey[S], ApplyEventsFunctionLongZIO]): Unit = {
    newApplyEvents.foreach { case (k, v) => applyEventsAt(k)(v)}
  }

  /**
   * onCommandHandlerFunctionUnhandled - default handler if commands are unhandled
   * PS: can be overridden in the entity
   *
   * @param state
   * @param cmd
   * @return
   */
  def onCommandHandlerFunctionUnhandled(state: StateWithData, cmd: ActorCommand[C]): ZIO[ZIOEnv, ZioError, ActorResult[E]] = {
    for {
      _ <- Logger.error(s"Unhandled Command - $cmd is not handled on state ${state.getClass}")
    } yield ActorResult.empty
  }

  /**
   * onApplyEventsFunctionUnhandled - default handler if events are unhandled
   * PS: can be overridden in the entity
   *
   * @param event
   * @param fallbackState
   * @return
   */
  def onApplyEventsFunctionUnhandled(event: Event, fallbackState: StateWithData): StateWithData = {
    println(s"Unhandled Event - $event is not handled on state ${fallbackState.getClass}")
    fallbackState
  }

  /**
   * eventSourcedBehavior - Event Source Behavior for the entity
   *
   * @param ctx
   * @param customLayer
   * @return
   */
  private def eventSourcedBehavior(
    implicit ctx: ActorContextC[C],
    customLayer: Layer[Any, ZIOEnv],
    stash: StashBufferC,
    timer: TimerSchedulerC
  ): EventSourcedBehavior[ActorCommand[C], Event, StateWithData] = {
    EventSourcedBehavior(
      persistenceId = PersistenceId.ofUniqueId(s"${settings.name}-${ctx.self.path.name}"),
      emptyState = relationInitialState.getOrElse(settings.initialState),
      commandHandler = commandHandler,
      eventHandler = eventHandler
    ).onPersistFailure(
      settings.onPersistStrategy.fold(
        SupervisorStrategy.restartWithBackoff(minBackoff = 10.seconds, maxBackoff = 60.seconds, randomFactor = 0.1)
      )(strategy => strategy)
    ).withRetention(
      settings.retentionCriteria.fold(
        RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 5).withDeleteEventsOnSnapshot
      )(strategy => strategy)
    )
    // TODO: add handler for recovery failure on snapshot
    //  akka.persistence.typed.internal.JournalFailureException: Exception during recovery from snapshot.
  }

  private def commandHandler(
    implicit ctx: ActorContextC[C],
    customLayer: Layer[Any, ZIOEnv],
    stash: StashBufferC,
    timer: TimerSchedulerC
  ): ActorCommandHandler = {
    (stateInput, commandInput) =>
      val commandDescZIO: ZIO[ZIOEnv, ZioError, ActorResult[Event]] = stateInput.stateData match {
        case sd: SD =>
          val cmdFunc: CommandHandlerFunctionLongZIO = handlersMap(StateKey(stateInput).key)
          if (cmdFunc.isDefinedAt((commandInput, sd, stateInput, ctx, stash, timer))){
            cmdFunc((commandInput, sd, stateInput, ctx, stash, timer))
          } else onCommandHandlerFunctionUnhandled(stateInput, commandInput)
      }
      val withEffectPostProcessingZIO = for {
        _ <- Logger.info(s"Command: ${commandInput.toString} on state ${stateInput.getClass}")
        actorResult <- commandDescZIO
        events = actorResult.events.toList
        canReply = actorResult.replyEvent.isDefined && actorResult.replyTo.isDefined
        _ <- Logger.debug("Reply: " + canReply)
        _ <- ZIO.when(canReply){
          for {
            replyEvent <- IO.fromOption(actorResult.replyEvent)
            replyTo <- ZIO.fromOption(actorResult.replyTo)
            // TODO: much better validation approach
            _ <- ZIO.when(replyTo == "") {
              ZIO.fail(EmptyReplyTo())
            }
            actRef: ActorRef[Any] = ActorRefResolver(ctx.system).resolveActorRef(replyTo)
            _ <- ZIO.whenCase(commandInput.meta.isStatusReply) {
              case true =>
                for {
                  _ <- Logger.info(s"Replying with status to $replyTo - ${replyEvent.toString}")
                  _ <- Actor.tell(actRef, StatusReply.success(replyEvent))
                } yield ()
              case _ =>
                for {
                  _ <- Logger.info(s"Replying to $replyTo - ${replyEvent.toString}")
                  _ <- Actor.tell(actRef, replyEvent)
                } yield ()
            }
          } yield ()
        }
        effectPersist <- ZIO.effect(Effect.persist[Event, StateWithData](events))
      } yield effectPersist
      val effectStrategy = withEffectPostProcessingZIO.foldM(
        err => for {
          //TODO: reply to the actor if it had failed to avoid the api to wait on timeout
          _ <- Logger.error(s"Effect Strategy Error: $err")
          _ <- ZIO.whenCase(err){
            case EventSourcedActorError(throwable, Some(replyTo)) => {
              val actRef: ActorRef[Any] = ActorRefResolver(ctx.system).resolveActorRef(replyTo)
              for {
                _ <- ZIO.whenCase(commandInput.meta.isStatusReply) {
                  case true => {
                    for {
                      _ <- Logger.info(s"Replying with error status to $replyTo")
                      _ <- Actor.tell(actRef, StatusReply.error(throwable))
                    } yield ()
                  }
                  case _ => Logger.error("Not ask with status, caller will timeout in a few moment! Please use `askWithStatus`")
                }
              } yield ()
            }
          }
          res <- ZIO.succeed(Effect.none[Event, StateWithData])
        } yield res,
        success => for {
          _ <- ZIO.when(success.events.nonEmpty) {
            ZIO.foreach(success.events){
              event => Logger.info("Raising Event: " + event)
            }
          }
          res <- ZIO.succeed(success)
        } yield res
      )
      runtime.unsafeRun(effectStrategy.provideCustomLayer(customLayer))
  }

  private def eventHandler: ActorEventHandler = {
    (stateInput, eventInput) => {
      stateInput.stateData match {
        case sd: SD =>
          //TODO: this will throw an error if key is not found on the map, this must not cause
          // any runtime exception. We must need to handle it.
          val applyFunc: ApplyEventsFunctionLongZIO = applyEventsMap(StateKey(stateInput).key)
          if (applyFunc.isDefinedAt((eventInput, sd, stateInput))) {
            applyFunc(eventInput, sd, stateInput)
          } else onApplyEventsFunctionUnhandled(eventInput, stateInput)
      }

//      val applyFunc: ApplyEventsFunctionZIO = applyEventsMap(StateKey(stateInput).key)
//      if (applyFunc.isDefinedAt((eventInput, stateInput))) {
//        applyFunc(eventInput, stateInput)
//      } else onApplyEventsFunctionUnhandled(eventInput, stateInput)

    }
  }

  def createError(str: String): EventSourcedActorError =
    EventSourcedActorError(
      new Throwable(str)
    )

  def createError(str: String, replyTo: Option[String]): EventSourcedActorError =
    EventSourcedActorError(
      new Throwable(str),
      replyTo
    )

  // TODO: Question - is it okay to put implicit class and def in a trait
//  implicit class ActorOps(actorResult: ActorResult[E]) {
//    def toZIO: Task[ActorResult[E]] = ZIO[ActorResult[E]](actorResult)
//  }

  implicit def toZIO(actorResult: ActorResult[Event]): ZIO[Any, EventSourcedActorError, ActorResult[Event]] = {
    ZIO[ActorResult[Event]](actorResult)
      .mapError(ex => EventSourcedActorError(ex))
  }

  implicit class OptionNothingErrorMapOps(value: Option[Nothing]) {
    def createError(str: String): EventSourcedActorError =
      EventSourcedActorError(
        new Throwable(str)
      )
  }

  implicit class ThrowableErrorMapOps(value: Throwable) {
    def createError: EventSourcedActorError =
      EventSourcedActorError(value)
  }

  implicit def commandHandlerMiniToLong(pf: CommandHandlerFunctionZIO): CommandHandlerFunctionLongZIO = {
    convertCommandHandlerMini(pf)
  }

  implicit def commandHandlerBehaviorToLong(pf: CommandHandlerFunctionBehaviorZIO): CommandHandlerFunctionLongZIO = {
    convertCommandHandlerBehavior(pf)
  }

  implicit def applyEventsMiniToLong(pf: ApplyEventsFunctionZIO): ApplyEventsFunctionLongZIO = {
    convertApplyEventMini(pf)
  }

  implicit def applyEventsStateDataOnlyToLong(pf: ApplyEventsFunctionStateDataOnlyZIO): ApplyEventsFunctionLongZIO = {
    convertApplyEventStateDataOnly(pf)
  }

}
