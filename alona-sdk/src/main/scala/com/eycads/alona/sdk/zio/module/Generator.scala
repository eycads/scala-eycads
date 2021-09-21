package com.eycads.alona.sdk.zio.module

import com.eycads.alona.sdk.zio.types.ErrorTypes.ZioError
import com.google.protobuf.timestamp.Timestamp
import zio.{Has, ZIO}

import java.util.UUID

object Generator {

  type Generator = Has[Generator.Service]

  trait Service {
    def generateUUID: ZIO[Generator, ZioError, UUID]
    def generateUUIDString: ZIO[Generator, ZioError, String]
    def generateTimestamp: ZIO[Generator, ZioError, Timestamp]
    def generateTimestamp(millis: Long): ZIO[Generator, ZioError, Timestamp]
  }

  def generateUUID: ZIO[Generator, ZioError, UUID] = {
    ZIO.accessM(_.get.generateUUID)
  }

  def generateUUIDString: ZIO[Generator, ZioError, String] = {
    ZIO.accessM(_.get.generateUUIDString)
  }

  def generateTimestamp: ZIO[Generator, ZioError, Timestamp] = {
    ZIO.accessM(_.get.generateTimestamp)
  }

  def generateTimestamp(millis: Long): ZIO[Generator, ZioError, Timestamp] = {
    ZIO.accessM(_.get.generateTimestamp(millis))
  }

}
