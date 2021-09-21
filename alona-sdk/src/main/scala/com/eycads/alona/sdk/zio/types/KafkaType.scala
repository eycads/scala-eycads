package com.eycads.alona.sdk.zio.types

import com.eycads.sdk.protocols.KafkaMessage
import org.apache.kafka.clients.consumer.{Consumer, ConsumerRecord}
import org.apache.kafka.clients.producer.{Producer, ProducerRecord, RecordMetadata}

object KafkaType {
  type KafkaIProducer[K, V] = Producer[K, V]
  type KafkaIConsumer[K, V] = Consumer[K, V]
  type KafkaProducerEvent = KafkaIProducer[String, KafkaMessage]
  type KafkaIRecord[K, V] = ProducerRecord[K, V]
  type KafkaIConsRecord[K, V] = ConsumerRecord[K, V]
  type KafkaIRecordMetadata = RecordMetadata
}
