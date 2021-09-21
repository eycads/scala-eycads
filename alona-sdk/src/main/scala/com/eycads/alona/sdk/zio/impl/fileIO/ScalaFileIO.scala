package com.eycads.alona.sdk.zio.impl.fileIO

import com.eycads.alona.sdk.zio.module.FileIO
import com.eycads.alona.sdk.zio.module.FileIO.FileIO
import zio.{Has, Layer, Task, ZIO, ZLayer}
import scala.io.{BufferedSource, Codec, Source}

case class ScalaFileIO() extends FileIO.Service {

  // TODO: Error handling if filePath do not exist
  override def readFileToString(filePath: String): ZIO[FileIO, Throwable, String] = {
    for {
//      res <- Task.fail(new IllegalArgumentException("Fail"))
      source: BufferedSource <- Task.effect(Source.fromResource(filePath, getClass.getClassLoader)(Codec.UTF8))
      x <- Task.effect(source.reader())
      res <- Task.effect(source.mkString)
      _ <- Task.effect(source.close())
    } yield res
  }

  override def isFileExists(filePath: String): ZIO[FileIO, Throwable, String] = {
    for {
      res <- Task.effect(scala.reflect.io.File(filePath).exists)
    } yield "Success"
  }

}

object ScalaFileIO {

  val live: Layer[Throwable, Has[FileIO.Service]] = ZLayer.succeed[FileIO.Service](ScalaFileIO())

  def createLive(): Layer[Throwable, FileIO] = {
    live
  }

}
