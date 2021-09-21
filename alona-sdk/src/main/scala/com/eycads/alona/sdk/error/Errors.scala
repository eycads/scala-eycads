package com.eycads.alona.sdk.error

import com.eycads.sdk.protocols.Error

object Errors {

  trait AlonaError extends Error {
    val code: String
    val message: String
  }

  case class EmptyReplyTo(code: String = "DTS00001", message: String = "Empty Reply To") extends AlonaError

}
