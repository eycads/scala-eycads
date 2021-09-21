package com.eycads.alona.sdk.enums

object HashConfig extends Enumeration {
  type HashConfig = HashConfigInternalValue

  case class HashConfigInternalValue(
    configName: String,
    algorithm: String,
    iterations: Int,
    saltByteSize: Int,
    keyLength: Int
  ) extends Val(configName)

  val PBKDF2v1: HashConfig =
    HashConfigInternalValue(
      "PBKDF2v1",
      "PBKDF2WithHmacSHA256",
      10000,
      32,
      256
    )

  def getHashConfigByName: String => Option[HashConfig] = {
    case PBKDF2v1.configName => Some(PBKDF2v1)
    case _ => None
  }

}
