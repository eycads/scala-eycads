package com.eycads.alona.sdk.actor.common

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import com.typesafe.scalalogging.LazyLogging
import com.eycads.protocols.sdk.uniqueConstraint.commands.{AddUniqueConstraint, RemoveUniqueConstraint}
import com.eycads.protocols.sdk.uniqueConstraint.events.{UniqueConstraintAdded, UniqueConstraintInit, UniqueConstraintRemoved}
import com.eycads.protocols.sdk.uniqueConstraint.states.{Initial, Ready, UniqueConstraintStateData}
import com.eycads.alona.sdk.actor.{ActorResult, EventSourcedActorBase, StateKey}
import com.eycads.alona.sdk.actor.common.UniqueConstraints.UniqueConstraints
import com.eycads.alona.sdk.actor.interface.ModuleEntityTypeKey
import com.eycads.sdk.protocols.{ActorCommand, Event, UniqueConstraintCommand, UniqueConstraintEvent, UniqueConstraintState}
import com.eycads.alona.sdk.utils.UniqueConstraintUtils
import com.eycads.alona.sdk.zio.Environment.EventSourcedActorEnv
import com.eycads.alona.sdk.zio.module.Logger
import com.eycads.alona.sdk.zio.types.ErrorTypes.EventSourcedActorError
import com.eycads.protocols.sdk.ShardingStrategy
import zio.{Layer, ZIO}

class UniqueConstraintActor(name: String, shardingStrategy: ShardingStrategy)
  extends EventSourcedActorBase[UniqueConstraintCommand, UniqueConstraintEvent, UniqueConstraintState, UniqueConstraintStateData]
    with LazyLogging {

  import UniqueConstraintActor._

  override def settings: EventSourcedActorSettings =
    EventSourcedActorSettings(
      name,
      Initial.defaultInstance,
      logger
    )

  override def handlers(implicit ctx: ActorContextC[UniqueConstraintCommand]):
      Map[StateKey[UniqueConstraintState], CommandHandlerFunctionLongZIO] = Map(
    initialStateKey -> initialHandler,
    readyStateKey -> readyHandler
  )

  def initialHandler: CommandHandlerFunctionZIO = {
    case (ActorCommand(meta, cmd: AddUniqueConstraint), sd) => {
      // TODO: Add validation
      val event = UniqueConstraintInit(
        id = cmd.id,
        ownerId = cmd.ownerId,
        code = cmd.code,
        value = cmd.value,
        shardingDetails = UniqueConstraintUtils.getShardingDetails(shardingStrategy, cmd.value)
      )
      val response =  UniqueConstraintAdded(
        id = cmd.id,
        value = cmd.value
      )
      ActorResult
        .withEvents(event)
        .withResponse(response, cmd.replyTo)
    }
  }

  def readyHandler: CommandHandlerFunctionZIO = {
    case (ActorCommand(meta, cmd: AddUniqueConstraint), sd) => {
      val event = UniqueConstraintAdded(
        id = cmd.id,
        value = cmd.value
      )
      for {
        _ <- Logger.info("Add Unique Constraint")
        shardingDetails <- ZIO.fromOption(UniqueConstraintUtils.getShardingDetails(shardingStrategy, cmd.value))
          .mapError(_.createError("No sharding details being configured"))
        shardingValue = UniqueConstraintUtils.getShardingValue(shardingStrategy, cmd.value)
        // TODO: Add validation on initialized sharding details
        _ <- ZIO.when(!shardingValue.equals(shardingDetails.shardingValue)){
          ZIO.fail(EventSourcedActorError(new Throwable(s"Incorrect sharding details value: $shardingValue, expected ${shardingDetails.shardingValue}")))
        }
        _ <- ZIO.when(sd.values(cmd.value)){
          ZIO.fail(createError(s"Value '${cmd.value}' already exist", cmd.replyTo))
        }
      } yield ActorResult
        .withEvents(event)
        .withResponse(event, cmd.replyTo)
    }
    case (ActorCommand(meta, cmd: RemoveUniqueConstraint), sd) => {
      val event = UniqueConstraintRemoved(
        id = cmd.id,
        value = cmd.value
      )
      for {
        _ <- Logger.info("Remove Unique Constraint")
        _ <- ZIO.when(checkUniqueDetails(sd)(cmd.ownerId, cmd.code)){
          ZIO.fail(createError(s"Inconsistent Uniqueness Details: ${sd.ownerId} - ${sd.code}"))
        }
        _ <- ZIO.when(!sd.values(cmd.value)){
          ZIO.fail(createError(s"Value '${cmd.value}' does not exist", cmd.replyTo))
        }
      } yield ActorResult
        .withEvents(event)
        .withResponse(event, cmd.replyTo)
    }
  }

  private def checkUniqueDetails(sd: UniqueConstraintStateData)(ownerId: String, code: String): Boolean = {
    !sd.ownerId.equals(ownerId) && !sd.code.equals(code)
  }

  override def applyEvents: Map[StateKey[UniqueConstraintState], ApplyEventsFunctionLongZIO] = Map(
    initialStateKey ->
      {
        case (event: UniqueConstraintInit, sd, state) =>
          Ready(
            sd.update(
              _.id := event.id,
              _.ownerId := event.ownerId,
              _.code := event.code,
              _.values := sd.values + event.value,
              _.optionalShardingDetails := event.shardingDetails
            )
          )
      },
    readyStateKey -> readyApplyEvents
  )

  def readyApplyEvents: ApplyEventsFunctionStateDataOnlyZIO = {
    case (event: UniqueConstraintAdded, sd, state) =>
      state.withStateData(sd.update(
        _.values := sd.values + event.value
      ))
    case (event: UniqueConstraintRemoved, sd, state) =>
      state.withStateData(sd.update(
        _.values := sd.values - event.value
      ))
  }

}

object UniqueConstraintActor {
  val initialStateKey: StateKey[Initial] = StateKey(Initial.defaultInstance)
  val readyStateKey: StateKey[Ready] = StateKey(Ready.defaultInstance)

  def init(
    system: ActorSystem[_],
    cs: ClusterSharding,
    uniqueConstraints: UniqueConstraints,
  )(implicit customLayer: Layer[Any, EventSourcedActorEnv]): ActorRef[ShardingEnvelope[ActorCommand[UniqueConstraintCommand]]] = {
    cs.init(Entity(ModuleEntityTypeKey.uniqueConstraintTypeKey){
      entityContext =>
        new UniqueConstraintActor(uniqueConstraints.actorName, uniqueConstraints.shardingStrategy)
          .behavior(system)
    }.withRole(uniqueConstraints.role.toString))
  }
}
