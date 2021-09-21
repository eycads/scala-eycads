package com.eycads.alona.sdk.actor

import com.eycads.sdk.protocols.{Event, Result, StateWithData}
import zio.ZIO

//case class ActorResultPayload(
//  response: Option[Event] = None,
//  events: Seq[Event] = Nil,
//  replyTo: Option[String] = None
//)
//
//trait ActorResult[E] {
//  def payload: ActorResultPayload
//  def withResponse(event: Event, replyTo: String): ActorResultPayload = {
//    payload.copy(response = Some(event), replyTo = Some(replyTo))
//  }
//  def withEvents(events: List[Event]): ActorResultPayload = {
//    payload.copy(events = events)
//  }
//}
//
//object ActorResult {
//  def apply[E](req: ActorResultPayload): ActorResult[E] = new ActorResult[E] {
//    override def payload: ActorResultPayload = req
//  }
//  def none[E](): ActorResult[E] = new ActorResult[E] {
//    override def payload: ActorResultPayload = ActorResultPayload()
//  }
//  def withResponse[E](event: Event, replyTo: String): ActorResult[E] =
//    apply(ActorResultPayload(response = Some(event), replyTo = Some(replyTo)))
//  def withEvents[E](events: List[Event]): ActorResult[E] =
//    apply(ActorResultPayload(events = events))
//}

case class ActorResult[+E](
  replyEvent: Option[E] = None,
  events: Seq[E] = Nil,
  replyTo: Option[String] = None
) extends Result {
  def withResponse[EE >: E](replyEvent: EE, replyTo: Option[String]): ActorResult[EE] =
    copy(replyEvent = Some(replyEvent), replyTo = replyTo)
  def withEvents[EE >: E](events: List[EE]): ActorResult[EE] =
    copy(events = events)
  def withEvents[EE >: E](events: EE*): ActorResult[EE] =
    copy(events = events.toList)
  def appendEvents[EE >: E](addedEvents: List[EE]): ActorResult[EE] =
    copy(events = addedEvents ++ events)
  def appendEvents[EE >: E](addedEvents: EE*): ActorResult[EE] =
    copy(events = addedEvents.toSeq ++ events)
}

object ActorResult {
  def empty[E]: ActorResult[E] = ActorResult[E](None, Nil, None)
  def withResponse[E](event: E, replyTo: Option[String]): ActorResult[E] =
    ActorResult[E](replyEvent = Some(event), replyTo = replyTo)
  def withEvents[E](events: List[E]): ActorResult[E] = ActorResult[E](events = events)
  def withEvents[E](events: E*): ActorResult[E] = ActorResult[E](events = events.toList)
}

