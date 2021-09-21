package com.eycads.sdk.protocols

import com.eycads.protocols.sdk.relation.{EntityRelationData, ParentRelationOverride}
import scalapb.GeneratedMessage

trait MessageProtocol

trait Command extends MessageProtocol
trait Event extends MessageProtocol with GeneratedMessage
trait EventNormal extends MessageProtocol
trait State extends MessageProtocol
trait Request extends MessageProtocol
trait Response extends MessageProtocol
trait Result extends MessageProtocol
trait Metadata extends MessageProtocol
trait StateData extends MessageProtocol with GeneratedMessage
trait RelationData extends MessageProtocol
trait Error extends MessageProtocol
trait Companion

trait StateWithData extends State {
  val stateData: StateData
  val relationData: EntityRelationData
  def withRelationData(__v: EntityRelationData): StateWithData
}
trait StateWithStateDataOnly[SD] extends StateWithData {
  val relationData: EntityRelationData = EntityRelationData.defaultInstance
  def withRelationData(__v: EntityRelationData): StateWithStateDataOnly[SD] = this
  def withStateData(__v: SD): StateWithData
}
trait WithStateData[SD] {
  def withStateData(__v: SD): StateWithData
}


trait ErrorWithThrowable extends Error{
  def toThrowable: Throwable
}
trait CommandWithReplyTo extends Command with GeneratedMessage {
  val replyTo: Option[String]
  def withReplyTo(__val: String): CommandWithReplyTo
}
trait CreateCommand extends Command with GeneratedMessage {
  val id: String
  val ownerId: String
  val skipParentConfirmation: Boolean
  val parentRelationOverride: Option[ParentRelationOverride]
}

trait AlonaMessage[+M] {
  val meta: Metadata
  val payload: M
}
trait StateCompanion extends Companion

//trait ActorCommand extends Command
trait ServerReply extends Command
trait GuardianCommand extends Command

trait StreamProcessorResponse extends Response

trait StreamProcessorCommand extends Command

trait LoggerEvent extends Event

trait TypeTag extends GeneratedMessage {
  val tag: Int
}

trait KafkaMessage extends Serializable {
  def typeTag: TypeTag
  def content: GeneratedMessage
}

case class KafkaEvent(typeTag: TypeTag, content: Event) extends KafkaMessage
case class KafkaLogMessage(typeTag: TypeTag, content: LoggerEvent) extends KafkaMessage


