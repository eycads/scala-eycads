package com.eycads.alona.sdk.actor

import akka.actor.typed.ActorRefResolver
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import com.eycads.protocols.sdk.relation.{ChildRelationCreated, CreationStatus, CreationTimeRelationshipData, EntityRelationData}
import com.eycads.protocols.sdk.relation.commands.{ChildCreationRequest, CreationAcknowledgementResponse, GetRelationData}
import com.eycads.protocols.sdk.relation.events.{CreationParentAckMarked, CreationRequestSent, CreationRequestSuccessful, CreationSkipAckSuccessful, RelationDataRetrieved}
import com.eycads.alona.sdk.common.Types.TypeId
import com.eycads.sdk.protocols.{ActorCommand, CommandWithReplyTo, CreateCommand, Event, RelationCommand, ScalaPbManifest, StateData, StateWithData, WithStateData}
import com.eycads.alona.sdk.utils.ProtocolsUtils
import com.eycads.alona.sdk.utils.ProtocolsUtils.ByteStringOps
import com.eycads.alona.sdk.zio.module.{Actor, Logger}
import com.eycads.alona.sdk.zio.types.ErrorTypes.{EventSourcedActorError, ZioError}
import scalapb.GeneratedMessage
import zio.{IO, ZIO}

// TODO: Question - is it possible to create the current traits before you can add new trait
trait EventSourcedActorRelation[C <: CommandWithReplyTo, E <: Event, S <: StateWithData, SD <: StateData] {
  _: EventSourcedActorBase[C, E, S, SD] =>

  type CreateCommandHandlerPF = PartialFunction[(ActorCommand[GeneratedMessage], SD), ZIO[ZIOEnv, ZioError, ActorResult[Event]]]

  case class CreationStep(
    creationHandler: CreateCommandHandlerPF,
    creationApplyEvents: ApplyEventsFunctionZIO
  )

  case class CreationTimeRelationshipConfig(
    defaultParentTypeId: TypeId,
    creationStateKey: StateKey[S],
    awaitCreationStateKey: StateKey[S],
    readyStateKey: StateKey[S],
    creationStep: CreationStep,
    awaitCreationState: WithStateData[SD],
    readyState: WithStateData[SD]
  )

  case class EventSourcedActorRelationSettings(
    sharding: ClusterSharding,
    newInitial: S,
    entityTypeKeyRefMap: Map[TypeId, EntityTypeKey[ActorCommand[RelationCommand]]],
    createConfig: CreationTimeRelationshipConfig,
  )

  def relationSettings: EventSourcedActorRelationSettings
  override val relationInitialState: Option[S] = Some(relationSettings.newInitial)

  //TODO: AwaitCreation must stash incoming messages, because in here we are still processing it to be Ready.
  // It might be good if we add verify the parent if it is already confirmed
  def relationHandlers: Map[StateKey[S], CommandHandlerFunctionLongZIO] =
    Map(
      relationSettings.createConfig.readyStateKey ->
        relationParentHandler.orElse(relationGetHandlers),
      relationSettings.createConfig.awaitCreationStateKey ->
        relationChildHandler.orElse(relationGetHandlers)
    )

  def relationChildHandler: CommandHandlerFunctionLongZIO = {
    case (ActorCommand(meta, cmd: CreationAcknowledgementResponse), sd, state, ctx, _, _) =>
      val event = CreationRequestSuccessful(
        id = cmd.id,
        childType = cmd.childType,
        parentType = cmd.parentType,
        parentId = cmd.parentId,
        parentDescription = cmd.parentDescription,
        creationStatus = CreationStatus.SUCCESS
      )
      for {
        _ <- Logger.info(s"Acknowledgement confirmation received from parent ${cmd.parentDescription} - ${cmd.parentId}")
        cData <- IO.fromOption(state.relationData.creationTimeRelationshipData)
          .mapError(_.createError("Missing creation time relationship data"))
        createCommand <- ZIO.fromTry(cData.creationCommand.deserializedToTryMessage)
          .mapError(_.createError)
        actorCommand = ActorCommand.create("account-id", createCommand)
        res <- relationSettings.createConfig.creationStep.creationHandler((actorCommand, sd))
      } yield res.appendEvents(event)
  }

  def relationParentHandler: CommandHandlerFunctionLongZIO = {
    case (ActorCommand(meta, cmd: ChildCreationRequest), sd, state, ctx, _, _) =>
      for {
        _ <- Logger.info(s"Received child creation request from child ${cmd.childType} - ${cmd.childId}")
        typeId <- ZIO.fromOption(ProtocolsUtils.getTypeIdOption(sd))
          .mapError(_.createError("this TypeId is missing"))
        desc <- ZIO.fromOption(ScalaPbManifest.getOptionName(typeId))
          .mapError(_.createError("missing TypeId in ScalaPbManifest.getOptionName"))
        _ <- ZIO.when(!cmd.skipAcknowledgement && cmd.replyTo.isEmpty){
          ZIO.fail(createError("replyTo is empty in the command"))
        }
        _ <- ZIO.when(!cmd.skipAcknowledgement && cmd.replyTo.isDefined) {
          val response = CreationAcknowledgementResponse(
            id = cmd.childId,
            parentType = typeId,
            parentId = cmd.id,
            parentDescription = desc,
            childType = cmd.childType,
            replyTo = None
          )
          val actRef = ActorRefResolver(ctx.system).resolveActorRef(cmd.replyTo.get)
          for {
            _ <- Logger.info(s"Response to child: " + response)
            _ <- Actor.tell(actRef, ActorCommand.create("accountId", response))
          } yield ()
        }
        event = CreationParentAckMarked(
          id = cmd.id,
          childType = cmd.childType,
          childId = cmd.childId,
          childDescription = cmd.childDescription,
          isAcknowledged = !cmd.skipAcknowledgement
        )
      } yield ActorResult.withEvents(event)
  }

  def relationGetHandlers: CommandHandlerFunctionLongZIO = {
    case (ActorCommand(meta, cmd: GetRelationData), sd, state, ctx, _, _) =>
      for {
        _ <- Logger.info("Creation time relation data: " + state.relationData)
        data <- ZIO.fromOption(state.relationData.creationTimeRelationshipData)
          .mapError(_.createError("There is no current relation data"))
        event = RelationDataRetrieved(
          parentType = data.parentType,
          parentId = data.parentId,
          parentDescription = data.parentDescription,
          creationStatus = data.creationStatus,
          isRogueEntity = data.isRogueEntity,
          childMap = data.childMap
        )
      } yield ActorResult.withResponse(event, cmd.replyTo)
  }

  registerCommandHandler(relationHandlers)

  when(relationSettings.createConfig.creationStateKey) {
    case (ActorCommand(meta, cmd), sd, state, ctx, _, _)
        if !relationSettings.createConfig.creationStep.creationHandler.isDefinedAt((ActorCommand(meta, cmd), sd)) => {
      for {
        _ <- Logger.error(s"Command: $cmd is not handled in the create handler.")
      } yield ActorResult.empty
    }
    case (actorCommand @ ActorCommand(meta, cmd: CreateCommand), sd, state, ctx, _, _) if cmd.skipParentConfirmation => {
      val (parentType, parentId) = getParentDetails(cmd)
      for {
        parentDesc <- ZIO.fromOption(ScalaPbManifest.getOptionName(parentType))
          .mapError(_.createError("missing TypeId in ScalaPbManifest.getOptionName"))
        _ <- ZIO.when(parentId.isEmpty){
          ZIO.fail(createError("Please provide ownerId or parentRelation.parentId"))
        }
        _ <- Logger.warn(s"Skip creation request, will directly create the entity - " + cmd.id)
        event = CreationSkipAckSuccessful(
          cmd.id,
          parentType,
          parentId,
          parentDesc
        )
        res <- relationSettings.createConfig.creationStep.creationHandler((actorCommand, sd))
      } yield res.appendEvents(event)
    }
    case (ActorCommand(meta, cmd: CreateCommand), sd, state, ctx, _, _) if !cmd.skipParentConfirmation => {
      val childType = ProtocolsUtils.getTypeId(sd)
      val (parentType, parentId) = cmd.parentRelationOverride
        .fold((relationSettings.createConfig.defaultParentTypeId, cmd.ownerId)) {
          rel => (rel.parentType, rel.parentId)
        }
      for {
        parentDesc <- ZIO.fromOption(ScalaPbManifest.getOptionName(parentType))
          .mapError(_.createError("missing TypeId in ScalaPbManifest.getOptionName"))
        childDesc <- ZIO.fromOption(ScalaPbManifest.getOptionName(childType))
          .mapError(_.createError("missing TypeId in ScalaPbManifest.getOptionName"))
        _ <- ZIO.when(parentId.isEmpty){
          ZIO.fail(createError("Please provide ownerId or parentRelation.parentId"))
        }
        replyToActorRef = ActorRefResolver(ctx.system).toSerializationFormat(ctx.self)
        parentRequest = ChildCreationRequest(
          id = parentId,
          childType = childType,
          childDescription = childDesc,
          childId = cmd.id,
          replyTo = Some(replyToActorRef)
        )
        _ <- ZIO.when(!relationSettings.entityTypeKeyRefMap.isDefinedAt(parentType)){
          ZIO.fail(createError("TypeId does not exist on the map"))
        }
        parentEntityRef = relationSettings.sharding.entityRefFor(
          relationSettings.entityTypeKeyRefMap(parentType),
          parentId
        )
        _ <- Logger.info("Save create command to ByteString: " + cmd)
        // TODO: ProtocolsUtils.getTypeId(cmd) handler if it will not get the correct typeId
        arrayBytesMessage <- ZIO.fromOption(ProtocolsUtils.convertMessageToByteStringOption(cmd))
          .mapError(_.createError("missing TypeId in ScalaPbManifest.convertMessageToByteStringOption"))
        _ <- Actor.tell(parentEntityRef, parentId, ActorCommand.create("accountId", parentRequest))
          .mapError(_.createError)
        _ <- Logger.info("Tell creation request to parent: " + parentRequest)
        event = CreationRequestSent(
          id = cmd.id,
          parentType = parentType,
          parentId = parentId,
          parentDescription = parentDesc,
          creationCommand = arrayBytesMessage)
      } yield ActorResult.withEvents(event)
    }
    case (ActorCommand(meta, cmd), sd, state, ctx, _, _) =>
      for {
        _ <- Logger.error(s"Command: $cmd is not a create command, please create the entity first.")
      } yield ActorResult.empty
  }

  val relationApplyEvents: Map[StateKey[S], ApplyEventsFunctionLongZIO] = Map(
    relationSettings.createConfig.creationStateKey ->
      convertApplyEventMini(relationSettings.createConfig.creationStep.creationApplyEvents),
    relationSettings.createConfig.awaitCreationStateKey ->
      convertApplyEventMini(relationSettings.createConfig.creationStep.creationApplyEvents)
        .orElse(relationChildApplyEvent),
    relationSettings.createConfig.readyStateKey -> relationParentApplyEvent
  )

  def relationChildApplyEvent: ApplyEventsFunctionLongZIO = {
    case (event: CreationRequestSuccessful, stateData, state) => {
      val updatedRelationData = state.relationData.update(
        _.creationTimeRelationshipData :=
          state.relationData.getCreationTimeRelationshipData.update(
            _.creationStatus := CreationStatus.SUCCESS
          )
      )
      state.withRelationData(updatedRelationData)
    }
  }

  def relationParentApplyEvent: ApplyEventsFunctionLongZIO = {
    case (event: CreationParentAckMarked, stateData, state) =>
      val mapValue = Map(
        event.childId ->
          ChildRelationCreated(
            childType = event.childType,
            childId = event.childId,
            childDescription = event.childDescription,
            childRelationStatus = CreationStatus.SUCCESS,
            isAcknowledged = event.isAcknowledged
          )
        )
      val updatedRelationData = state.relationData.update(
        _.creationTimeRelationshipData := {
          val rData = state.relationData.getCreationTimeRelationshipData
          rData.update(
            _.childMap := rData.childMap ++ mapValue
          )
        }
      )
      state.withRelationData(updatedRelationData)
  }

  applyEventsAt(relationSettings.createConfig.creationStateKey){
    case (event: CreationRequestSent, sd, state) =>
      val updatedRelationData = state.relationData.update(
        _.creationTimeRelationshipData :=
          CreationTimeRelationshipData(
            parentType = event.parentType,
            parentId = event.parentId,
            parentDescription = event.parentDescription,
            creationCommand = event.creationCommand,
            creationStatus = CreationStatus.PENDING
          )
      )
      relationSettings.createConfig.awaitCreationState
        .withStateData(sd)
        .withRelationData(updatedRelationData)
    case (event: CreationSkipAckSuccessful, sd, state) =>
      val updatedRelationData = state.relationData.update(
        _.creationTimeRelationshipData :=
          CreationTimeRelationshipData(
            parentType = event.parentType,
            parentId = event.parentId,
            parentDescription = event.parentDescription,
            creationStatus = CreationStatus.SUCCESS,
            isRogueEntity = true
          )
      )
      state.withRelationData(updatedRelationData)
  }

  registerApplyEvents(relationApplyEvents)

  private def getParentDetails(cmd: CreateCommand): (TypeId, String) =
    cmd.parentRelationOverride
      .fold((relationSettings.createConfig.defaultParentTypeId, cmd.ownerId)) {
        rel => (rel.parentType, rel.parentId)
      }

}

object EventSourcedActorRelation {

}
