package com.eycads.alona.sdk.zio.impl.kafka

import com.eycads.alona.sdk.zio.module.Kafka
import com.eycads.alona.sdk.zio.module.Kafka.Service
import com.eycads.alona.sdk.zio.types.KafkaType.{KafkaIProducer, KafkaIRecord, KafkaIRecordMetadata}
import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.common.errors.ProducerFencedException
import zio.Task

import scala.concurrent.Promise
import scala.util.{Failure, Success, Try}


trait ApacheKafka extends Kafka {

  def kafka[K, V](implicit kafkaProducer: KafkaIProducer[K, V]): Kafka.Service[K, V] = new Service[K, V] {

    override def send(kafkaIRecord: KafkaIRecord[K, V]): Task[KafkaIRecordMetadata] = {
      for {
        promise <- Task(Promise[KafkaIRecordMetadata]())
        _ <- Task(kafkaProducer.send(kafkaIRecord, producerCallback(promise)))
        res <- Task.fromFuture(implicit ec => promise.future)
      } yield res
    }

    override def sendTransaction(kafkaIRecord: KafkaIRecord[K, V]): Task[java.util.concurrent.Future[KafkaIRecordMetadata]] = {
      for {
        _ <- Task(kafkaProducer.beginTransaction())
        res <- Task(kafkaProducer.send(kafkaIRecord))
      } yield res
//      val promise = Promise[KafkaIRecordMetadata]()
//      kafkaProducer.beginTransaction()
//      println("xxxxxxxx")
//      kafkaProducer.send(kafkaIRecord, producerCallback(promise))
//      kafkaProducer.commitTransaction()
//      Task.fromFuture(implicit ec => promise.future)
    }

    override def commitTransaction: Task[Unit] = {
      Task(kafkaProducer.commitTransaction())
    }

  }

  private def producerCallback(promise: Promise[KafkaIRecordMetadata]): Callback =
    producerCallback(result => promise.complete(result))

  private def producerCallback(callback: Try[KafkaIRecordMetadata] => Unit): Callback =
    new Callback {
      override def onCompletion(metadata: KafkaIRecordMetadata, exception: Exception): Unit = {
        val result =
          if (exception == null) Success(metadata)
          else Failure(exception)
        callback(result)
      }
    }
}

object ApacheKafka extends ApacheKafka
