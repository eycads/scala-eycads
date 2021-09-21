package com.eycads.alona.sdk.zio.types

import com.eycads.sdk.protocols.ErrorWithThrowable

object ErrorTypes {

  trait ZioError extends ErrorWithThrowable

  trait AkkaActorError extends ZioError
  case class AskError(throwable: Throwable) extends AkkaActorError {
    override def toThrowable: Throwable = throwable
  }
  case class TellError(throwable: Throwable) extends AkkaActorError {
    override def toThrowable: Throwable = throwable
  }
  case class GeneratorError(throwable: Throwable) extends ZioError {
    override def toThrowable: Throwable = throwable
  }

  case class EventSourcedActorError(
    throwable: Throwable,
    replyTo: Option[String] = None
  ) extends ZioError {
    override def toThrowable: Throwable = throwable
  }

}
