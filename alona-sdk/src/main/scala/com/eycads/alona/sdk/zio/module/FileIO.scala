package com.eycads.alona.sdk.zio.module

import zio.{Has, ZIO}

object FileIO {

  type FileIO = Has[FileIO.Service]

  trait Service {
    def isFileExists(filePath: String): ZIO[FileIO, Throwable, String]
    def readFileToString(path: String): ZIO[FileIO, Throwable, String]
  }

  def isFileExists(filePath: String): ZIO[FileIO, Throwable, String] = {
    ZIO.accessM(_.get.isFileExists(filePath))
  }

  def readFileToString(path: String): ZIO[FileIO, Throwable, String] = {
    ZIO.accessM(_.get.readFileToString(path))
  }

}
