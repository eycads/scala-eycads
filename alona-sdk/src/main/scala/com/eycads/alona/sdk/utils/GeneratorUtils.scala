package com.eycads.alona.sdk.utils

import com.google.protobuf.timestamp.Timestamp

import java.util.UUID

object GeneratorUtils {

  def generateOptionTimestamp: Option[Timestamp] = {
    Some(generateTimestamp(System.currentTimeMillis()))
  }

  def generateTimestamp: Timestamp = {
    generateTimestamp(System.currentTimeMillis())
  }

  def generateTimestamp(millis: Long): Timestamp = {
    val seconds = millis/1000
    val nanos = ((millis % 1000) * 1000000).toInt
    Timestamp(seconds, nanos)
  }

  def generateUUID: UUID = {
    UUID.randomUUID()
  }

  def generateUUIDString: String = {
    generateUUID.toString
  }

}
