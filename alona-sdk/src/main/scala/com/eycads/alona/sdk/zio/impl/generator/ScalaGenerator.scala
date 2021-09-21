package com.eycads.alona.sdk.zio.impl.generator

import com.eycads.alona.sdk.utils.GeneratorUtils
import com.eycads.alona.sdk.zio.module.Generator
import com.eycads.alona.sdk.zio.module.Generator.Generator
import com.eycads.alona.sdk.zio.types.ErrorTypes.{GeneratorError, ZioError}
import com.google.protobuf.timestamp.Timestamp

import zio.{Layer, ZIO, ZLayer}

import java.util.UUID

class ScalaGenerator extends Generator.Service {

  override def generateUUID: ZIO[Generator, ZioError, UUID] = {
    ZIO.effect(GeneratorUtils.generateUUID)
      .mapError(x => GeneratorError(x))
  }

  //TODO: Remove dash
  override def generateUUIDString: ZIO[Generator, ZioError, String] = {
    ZIO.effect(GeneratorUtils.generateUUIDString)
      .mapError(x => GeneratorError(x))
  }

  override def generateTimestamp: ZIO[Generator, ZioError, Timestamp] = {
    ZIO.effect(GeneratorUtils.generateTimestamp)
      .mapError(x => GeneratorError(x))
  }

  override def generateTimestamp(millis: Long): ZIO[Generator, ZioError, Timestamp] = {
    ZIO.effect(GeneratorUtils.generateTimestamp(millis))
      .mapError(x => GeneratorError(x))
  }

}

object ScalaGenerator {
  def createLive: Layer[ZioError, Generator] = ZLayer.succeed(new ScalaGenerator)
}
