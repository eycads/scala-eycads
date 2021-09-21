package com.eycads.alona.account.actors

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import com.google.protobuf.timestamp.Timestamp
import com.typesafe.scalalogging.LazyLogging
import com.eycads.protocols.account.states.AccountEntityStateData
import com.eycads.protocols.user.commands.{CreateUserCompact, UserCreated}
import com.eycads.protocols.user.common.PasswordHistory
import com.eycads.protocols.user.states
import com.eycads.protocols.user.states.{AwaitCreation, Initial, Pending, Ready, UserEntityStateData}
import com.eycads.alona.sdk.actor.interface.{EntityRole, ModuleEntityTypeKey}
import com.eycads.alona.sdk.actor.{ActorResult, EventSourcedActorBase, EventSourcedActorRelation, StateKey}
import com.eycads.sdk.protocols.{ActorCommand, UserCommand, UserEvent, UserState}
import com.eycads.alona.sdk.utils.GeneratorUtils
import com.eycads.alona.sdk.utils.ProtocolsUtils.CustomOptionsOps
import com.eycads.alona.sdk.zio.Environment.EventSourcedActorEnv
import com.eycads.alona.sdk.zio.module.Logger
import zio.Layer

class UserEntity(name: String, sharding: ClusterSharding)
  extends EventSourcedActorBase[UserCommand, UserEvent, UserState, UserEntityStateData]
    with EventSourcedActorRelation[UserCommand, UserEvent, UserState, UserEntityStateData]
    with LazyLogging {

  import UserEntity._

  override def settings: EventSourcedActorSettings =
    EventSourcedActorSettings(
      name,
      Initial.defaultInstance,
      logger
    )

  override def relationSettings: EventSourcedActorRelationSettings =
    EventSourcedActorRelationSettings(
      sharding,
      Initial.defaultInstance,
      entityTypeKeyRefMap =
        Map(
          AccountEntityStateData.typeId -> ModuleEntityTypeKey.accountTypeKey,
          UserEntityStateData.typeId -> ModuleEntityTypeKey.userTypeKey
        ),
      createConfig = CreationTimeRelationshipConfig(
        defaultParentTypeId = AccountEntityStateData.typeId,
        creationStateKey = initialStateKey,
        awaitCreationStateKey = awaitCreationStateKey,
        readyStateKey = pendingStateKey,
        creationStep,
        AwaitCreation.defaultInstance,
        Ready.defaultInstance
      )
    )

  private def creationStep: CreationStep =
    CreationStep(
      creationHandler = {
        case (ActorCommand(meta, cmd: CreateUserCompact), stateData) => {
          val event = UserCreated(
            id = cmd.id,
            userName = cmd.userName,
            firstName = cmd.firstName,
            lastName = cmd.lastName,
            email = cmd.email,
            number = cmd.number,
            birthDate = cmd.birthDate,
            gender = cmd.gender,
            passwordHash = cmd.passwordHash,
            passwordHistory = PasswordHistory(
              timestamp = GeneratorUtils.generateOptionTimestamp,
              passwordHash = cmd.passwordHash
            ),
            creationMode = cmd.creationMode,
          )
          for {
            _ <- Logger.info("Create User Compact")
          } yield ActorResult
            .withEvents(event)
            .withResponse(event, cmd.replyTo)
        }
      },
      creationApplyEvents = {
        case (event: UserCreated, sd) =>
          Ready(sd.update(
            _.id := event.id,
            _.userName := event.userName,
            _.firstName := event.firstName,
            _.lastName := event.lastName,
            _.optionalMiddleName := event.middleName,
            _.optionalNamePrefix := event.namePrefix,
            _.optionalNameSuffix := event.nameSuffix,
            _.optionalEmail := event.email,
            _.optionalNumber := event.number,
            _.birthDate := event.birthDate,
            _.gender := event.gender,
            _.passwordHash := event.passwordHash,
            _.passwordHistory := Seq(event.passwordHistory),
            _.creationMode := event.creationMode
          ))
      }
    )

  override def handlers(implicit ctx: ActorContextC[UserCommand]): Map[StateKey[UserState], CommandHandlerFunctionLongZIO] =
    Map.empty

  override def applyEvents: Map[StateKey[UserState], ApplyEventsFunctionLongZIO] =
    Map.empty

}

object UserEntity {
  val initialStateKey: StateKey[Initial] = StateKey(Initial.defaultInstance)
  val awaitCreationStateKey: StateKey[AwaitCreation] = StateKey(AwaitCreation.defaultInstance)
  val pendingStateKey: StateKey[Pending] = StateKey(Pending.defaultInstance)
  val readyStateKey: StateKey[Ready] = StateKey(Ready.defaultInstance)

  def init(
    system: ActorSystem[_],
    cs: ClusterSharding,
  )(implicit customLayer: Layer[Any, EventSourcedActorEnv]):
  ActorRef[ShardingEnvelope[ActorCommand[UserCommand]]] = {
    cs.init(Entity(ModuleEntityTypeKey.userTypeKey){
      entityContext =>
        new UserEntity("UserEntity", cs).behavior(system)
    }.withRole(EntityRole.UserEntityRole.toString))
  }
}
