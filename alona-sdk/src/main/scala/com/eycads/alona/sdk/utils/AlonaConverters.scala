package com.eycads.alona.sdk.utils

import com.google.protobuf.ByteString
import com.typesafe.config.Config

import java.nio.{ByteBuffer, ByteOrder}
import java.util.Properties
import scala.collection.JavaConverters._

object AlonaConverters {

  implicit class ConfigOps(value: Config) {
    def toProperties: Properties = {
      val properties = new Properties()
      value.entrySet().asScala.foreach{ entry =>
        properties.put(entry.getKey, entry.getValue.unwrapped())
      }
      properties
    }
  }

  implicit class IntOps(value: Int) {
    def toByteArray(size: Int): Array[Byte] = {
      ByteBuffer.allocate(size).putInt(value).order(ByteOrder.nativeOrder).array()
    }

    def toByteString(size: Int): ByteString = {
      ByteString.copyFrom(ByteBuffer.allocate(size).putInt(value).order(ByteOrder.nativeOrder).array())
    }
  }

  implicit class ByteArrayOps(value: Array[Byte]) {
    def concatWith4ByteLength(sec: Array[Byte]): Array[Byte] = {
      value.length.toByteArray(4) ++ value ++ sec.length.toByteArray(4) ++ sec
    }

    def concatWith8ByteLength(sec: Array[Byte]): Array[Byte] = {
      value.length.toByteArray(8) ++ value ++ sec.length.toByteArray(8) ++ sec
    }

    def splitIntLength: (Int, Array[Byte]) = {
      val (f, s) = value.splitAt(4)
      val intValue = f.toInt
      (intValue, s)
    }

    def toInt: Int = {
      ByteBuffer.wrap(value).getInt
    }
  }

}
