package com.eycads.alona.account.actors

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import com.eycads.protocols.account.commands.{CreateAccount, GetAccountDetails, UpdateAccount}
import com.eycads.protocols.account.events.{AccountCreated, AccountDetailsRetrieved, AccountDetailsUpdated}
import com.eycads.alona.sdk.actor.{ActorResult, EventSourcedActorBase, EventSourcedActorRelation, StateKey}
import com.eycads.sdk.protocols.{AccountCommand, AccountEvent, AccountState, ActorCommand}
import com.typesafe.scalalogging.LazyLogging
import com.eycads.protocols.account.states.{AccountEntityStateData, AwaitCreation, AwaitRelation, Deleted, DeletionInProgress, Initial, Ready}
import com.eycads.alona.sdk.actor.interface.{EntityProxy, EntityRole, ModuleEntityTypeKey}
import com.eycads.alona.sdk.utils.ProtocolsUtils.CustomOptionsOps
import com.eycads.alona.sdk.zio.Environment.EventSourcedActorEnv
import com.eycads.alona.sdk.zio.impl.actor.AkkaActor
import com.eycads.alona.sdk.zio.impl.logger.ScalaLogger
import zio.Layer

class AccountEntity(name: String, sharding: ClusterSharding)
  extends EventSourcedActorBase[AccountCommand, AccountEvent, AccountState, AccountEntityStateData]
    with EventSourcedActorRelation[AccountCommand, AccountEvent, AccountState, AccountEntityStateData]
    with LazyLogging {

  import AccountEntity._

  override def settings: EventSourcedActorSettings =
    EventSourcedActorSettings(
      name,
      AwaitCreation.defaultInstance,
      logger
    )

  override def relationSettings: EventSourcedActorRelationSettings =
    EventSourcedActorRelationSettings(
      sharding,
      Initial.defaultInstance,
      entityTypeKeyRefMap =
        Map(
          AccountEntityStateData.typeId -> ModuleEntityTypeKey.accountTypeKey
        ),
      createConfig = CreationTimeRelationshipConfig(
        defaultParentTypeId = AccountEntityStateData.typeId,
        creationStateKey = initialStateKey,
        awaitCreationStateKey = awaitCreationStateKey,
        readyStateKey = readyStateKey,
        creationStep,
        AwaitCreation.defaultInstance,
        Ready.defaultInstance
      )
    )

  override def handlers(implicit ctx: ActorContextC[AccountCommand]):
      Map[StateKey[AccountState], CommandHandlerFunctionLongZIO] = Map {
    readyStateKey -> readyHandler
  }

  private def creationStep: CreationStep =
    CreationStep(
      creationHandler = {
        case (ActorCommand(meta, cmd: CreateAccount), stateData) => {
          // TODO: Spawn child actors and see if it is really the child of the entity, not the server itself because the
          //  context provided to the Actor module is from server which is ActorContext[Nothing]
          logger.info("CreateAccount")
          val event = AccountCreated(cmd.id, cmd.code)
          ActorResult
            .withEvents(event)
            .withResponse(event, cmd.replyTo)
        }
      },
      creationApplyEvents = {
        case (event: AccountCreated, sd) =>
          Ready(AccountEntityStateData(event.id, event.code))
      }
    )

  private def readyHandler(implicit ctx: ActorContextC[AccountCommand]): CommandHandlerFunctionZIO = {
    case (ActorCommand(meta, cmd: GetAccountDetails), stateData) => {
      val event = AccountDetailsRetrieved(stateData.id, stateData.code)
      ActorResult
        .withResponse(event, cmd.replyTo)
    }
  }

  override def applyEvents: Map[StateKey[AccountState], ApplyEventsFunctionLongZIO] = Map(
    initialStateKey ->
      {
        case (event: AccountDetailsUpdated, sd, state) =>
          AwaitCreation(AccountEntityStateData(event.id, event.code))
      }
  )
}

object AccountEntity {
  val initialStateKey: StateKey[Initial] = StateKey(Initial.defaultInstance)
  val awaitCreationStateKey: StateKey[AwaitCreation] = StateKey(AwaitCreation.defaultInstance)
  val readyStateKey: StateKey[Ready] = StateKey(Ready.defaultInstance)
  val awaitRelationStateKey: StateKey[AwaitRelation] = StateKey(AwaitRelation.defaultInstance)
  val deletionInProgressStateKey: StateKey[DeletionInProgress] = StateKey(DeletionInProgress.defaultInstance)
  val deletedStateKey: StateKey[Deleted] = StateKey(Deleted.defaultInstance)

  // TODO: Determine the best error data-type for Layer instead of using Any
  def init(
    system: ActorSystem[_],
    cs: ClusterSharding,
  )(implicit customLayer: Layer[Any, EventSourcedActorEnv]):
      ActorRef[ShardingEnvelope[ActorCommand[AccountCommand]]] = {
    cs.init(Entity(ModuleEntityTypeKey.accountTypeKey){
      entityContext =>
        new AccountEntity("AccountEntity", cs).behavior(system)
    }.withRole(EntityRole.AccountEntityRole.toString))
  }

}
