package com.eycads.alona.sdk.zio

import com.eycads.alona.sdk.zio.impl.kafka.ApacheKafka
import com.eycads.alona.sdk.zio.module.Actor.Actor
import com.eycads.alona.sdk.zio.module.Generator.Generator
import com.eycads.alona.sdk.zio.module.Hash.Hash
import com.eycads.alona.sdk.zio.module.Logger.Logger
import com.eycads.alona.sdk.zio.module.Kafka

object Environment {
  type ServerEnv = Kafka

  val serverEnvRun: ApacheKafka.type = ApacheKafka

  type EventSourcedActorEnv = Actor
  type ApiEnv = Actor
    with Generator
    with Hash
    with Logger

}
