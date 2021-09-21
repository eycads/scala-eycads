package com.eycads.alona.sdk.zio.impl.hash

import com.eycads.alona.sdk.enums.HashConfig
import com.eycads.alona.sdk.enums.HashConfig.HashConfig
import com.eycads.alona.sdk.zio.module.Hash
import com.eycads.alona.sdk.zio.module.Hash.Hash
import zio.{Layer, Task, ZIO, ZLayer}

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PBKDF2Hash(hashConfig: HashConfig) extends Hash.Service {

  override def hashValue(value: String): ZIO[Hash, Throwable, String] = {
    for {
      (saltBase64, hashBase64, iter) <- Task.effect {
        //TODO: checking of value and hashconfig
        val random = new SecureRandom()
        val salt = new Array[Byte](hashConfig.saltByteSize)
        random.nextBytes(salt)
        val hash = getPbkdf2Hash(value, salt, hashConfig)
        val saltBase64 = Base64.getEncoder.encodeToString(salt)
        val hashBase64 = Base64.getEncoder.encodeToString(hash)
        (saltBase64, hashBase64, hashConfig.iterations)
      }
      res <- Task.effect(s"${hashConfig.configName}:$iter:$hashBase64:$saltBase64")
    } yield res
  }

  override def checkValue(newValue: String, valueHash: String): ZIO[Hash, Throwable, Boolean] = {
    for {
      (configName, hash, salt) <- Task.effect {
        valueHash.split(":") match {
          case Array(configName, iter, hashBase64, saltBase64) =>
            val salt = Base64.getDecoder.decode(saltBase64)
            val hash = Base64.getDecoder.decode(hashBase64)
            (configName, hash, salt)
          case _ => throw new Throwable("Invalid Hash Format")
        }
      }
      valueHashConfig <- ZIO.fromOption(HashConfig.getHashConfigByName(configName))
        .mapError(x => new Throwable("Algorithm not supported"))
      res <- Task.effect {
        val newHash = getPbkdf2Hash(newValue, salt, valueHashConfig)
        newHash.sameElements(hash)
      }
    } yield res
  }

  private def getPbkdf2Hash(value: String, salt: Array[Byte], config: HashConfig): Array[Byte] = {
    val keySpec = new PBEKeySpec(value.toCharArray, salt, config.iterations, config.keyLength)
    val keyFactory = SecretKeyFactory.getInstance(config.algorithm)
    keyFactory.generateSecret(keySpec).getEncoded
  }

}

object PBKDF2Hash {
  def createLive(hashConfig: HashConfig): Layer[Throwable, Hash] = ZLayer.succeed(new PBKDF2Hash(hashConfig))
}
