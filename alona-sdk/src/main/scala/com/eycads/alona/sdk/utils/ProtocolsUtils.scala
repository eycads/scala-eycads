package com.eycads.alona.sdk.utils

import com.eycads.alona.sdk.common.Types.TypeId
import com.eycads.customOptions.alona_options.AlonaOptionsProto
import com.eycads.sdk.protocols.ScalaPbManifest
import com.google.protobuf.ByteString
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}
import com.eycads.alona.sdk.utils.AlonaConverters.{ByteArrayOps, IntOps}

import scala.util.{Failure, Success, Try}

object ProtocolsUtils {

  implicit class CustomOptionsOps(companion: GeneratedMessageCompanion[_]) {
    def typeId: TypeId = getTypeId(companion)
    def name: String = ScalaPbManifest.getName(getTypeId(companion))
  }

  implicit class GeneratedMessageOps(message: GeneratedMessage) {
    def serializeToBytes(typeId: TypeId): Array[Byte] = {
      convertMessageToBytes(message, typeId)
    }

    def serializeToByteString: ByteString = {
      convertMessageToByteString(message)
    }
  }

  implicit class ByteStringOps(bs: ByteString) {
    def deserializedToMessage: GeneratedMessage = {
      deserializedByteStringToMessage(bs)
    }

    def deserializedToTryMessage: Try[GeneratedMessage] = {
      val (typeId, messageBytes): (Int, Array[Byte]) = splitTypeIdByteString(bs)
      ScalaPbManifest.toMessageCreationFunction(typeId)(messageBytes)
    }
  }

  implicit class ByteArrayMessageOps(ab: Array[Byte]) {
    def deserializedToMessage: GeneratedMessage = {
      val (typeId, messageBytes): (Int, Array[Byte]) = ab.splitIntLength
      val message = toProtoInstance(messageBytes, ScalaPbManifest.toMessageCreationFunction(typeId)) match {
        case x: GeneratedMessage => x
      }
      message
    }

    def deserializedToTryMessage: Try[GeneratedMessage] = {
      val (typeId, messageBytes): (Int, Array[Byte]) = ab.splitIntLength
      ScalaPbManifest.toMessageCreationFunction(typeId)(messageBytes)
    }
  }

  def deserializedByteStringToMessage(bs: ByteString): GeneratedMessage = {
    val (typeId, messageBytes): (Int, Array[Byte]) = splitTypeIdByteString(bs)
    val message = toProtoInstance(messageBytes, ScalaPbManifest.toMessageCreationFunction(typeId)) match {
      case x: GeneratedMessage => x
    }
    message
  }

  def splitTypeIdByteString(bs: ByteString): (Int, Array[Byte]) = {
    val typeId = bs.substring(0, 4).toByteArray.toInt
    val messageBytes = bs.substring(4).toByteArray
    (typeId, messageBytes)
  }

  def convertMessageToBytes[M <: GeneratedMessage](message: M, typeId: TypeId): Array[Byte] = {
    val contentBytes = message.toByteArray
    typeId.toByteArray(4) ++ contentBytes
  }

  def convertMessageToByteString[M <: GeneratedMessage](message: M): ByteString = {
    val typeId = getTypeId(message)
    convertMessageToByteString(message, typeId)
  }

  def convertMessageToByteStringOption[M <: GeneratedMessage](message: M): Option[ByteString] = {
    for {
      typeId <- getTypeIdOption(message)
    } yield convertMessageToByteString(message, typeId)
  }

  def convertMessageToByteString[M <: GeneratedMessage](message: M, typeId: TypeId): ByteString = {
    val contentBytes = message.toByteString
    typeId.toByteString(4) concat contentBytes
  }

  def getTypeId[M <: GeneratedMessage](message: M): TypeId = {
    getTypeId(message.companion)
  }

  def getTypeId(companion: GeneratedMessageCompanion[_]): TypeId = {
    companion
      .scalaDescriptor
      .getOptions
      .extension(AlonaOptionsProto.alonaMessage)
      .fold(-1){x => x.typeId.getOrElse(-1)}
  }

  def getTypeIdOption[M <: GeneratedMessage](message: M): Option[TypeId] = {
    getTypeIdOption(message.companion)
  }

  def getTypeIdOption(companion: GeneratedMessageCompanion[_]): Option[TypeId] = {
    companion
      .scalaDescriptor
      .getOptions
      .extension(AlonaOptionsProto.alonaMessage)
      .flatMap(_.typeId)
  }

  def toProtoInstance[T](value: Array[Byte], func: Array[Byte] => Try[T]): T = {
    val genMessage: T = func(value) match {
      case Success(a) => a
      case Failure(e) => throw new Exception(e)
    }
    genMessage
  }

  def main(args: Array[String]): Unit = {
//    val createAccount = CreateAccount("alona", 1949854505, Some("alonaSome"))
//    val typeIdOption = convertMessageToByteStringOption(createAccount)
//    println("typeIdOption: " + typeIdOption)

//    val createAccount = CreateAccount("alona", 1949854505, Some("alonaSome"))
//    val arrayBytes = createAccount.serializeToBytes(CreateAccount.typeId)
//    println("arrayBytes: " + arrayBytes)
//    val message: GeneratedMessage = arrayBytes.deserializedToMessage
//    println("message: " + message)

//    val byteString = ByteString.copyFrom(convertMessageToBytes(createAccount, CreateAccount.typeId))
//    println("byteString: " + byteString)
//    val message2: GeneratedMessage = byteString.toByteArray.deserializedToMessage
//    println("message2: " + message2)

//    val byteString = convertMessageToByteString(createAccount, CreateAccount.typeId)
//    println("byteString: " + byteString)
//    val message2: GeneratedMessage = deserializedByteStringToMessage(byteString)
//    println("message2: " + message2)
  }

}
