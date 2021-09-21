package com.eycads.alona.sdk.zio.module

import com.eycads.alona.sdk.zio.types.KafkaType.{KafkaIProducer, KafkaIRecord, KafkaIRecordMetadata}
import zio.Task

trait Kafka {
  def kafka[K, V](implicit kafkaProducer: KafkaIProducer[K, V]): Kafka.Service[K, V]
}

object Kafka {
  trait Service[K, V] {
    def send(kafkaIRecord: KafkaIRecord[K, V]): Task[KafkaIRecordMetadata]
    def sendTransaction(kafkaIRecord: KafkaIRecord[K, V]): Task[java.util.concurrent.Future[KafkaIRecordMetadata]]
    def commitTransaction: Task[Unit]
  }
}
