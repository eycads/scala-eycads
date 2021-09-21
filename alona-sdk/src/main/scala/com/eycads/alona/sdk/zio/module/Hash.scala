package com.eycads.alona.sdk.zio.module

import com.eycads.alona.sdk.enums.HashConfig
import com.eycads.alona.sdk.zio.impl.hash.PBKDF2Hash
import zio.{Has, Runtime, ZIO}

object Hash {

  type Hash = Has[Hash.Service]

  trait Service {
    def hashValue(value: String): ZIO[Hash, Throwable, String]
    def checkValue(value: String, valueHash: String): ZIO[Hash, Throwable, Boolean]
  }

  def hashValue(value: String): ZIO[Hash, Throwable, String] =
    ZIO.accessM(_.get.hashValue(value))

  def checkValue(value: String, valueHash: String): ZIO[Hash, Throwable, Boolean] =
    ZIO.accessM(_.get.checkValue(value, valueHash))

  def main(args: Array[String]): Unit = {
    val runtime = Runtime.default
    val liveHash = PBKDF2Hash.createLive(HashConfig.PBKDF2v1)

    val a = runtime.unsafeRun(hashValue("Alonaij1@").provideCustomLayer(liveHash))
    println(a)

    val b = runtime.unsafeRun(checkValue("Alonaij1@", a).provideCustomLayer(liveHash))
    println(b)
    val c = runtime.unsafeRun(checkValue("alonaij1@", a).provideCustomLayer(liveHash))
    println(c)
    val d = runtime.unsafeRun(checkValue("Alonaij2!", a).provideCustomLayer(liveHash))
    println(d)
  }
}
