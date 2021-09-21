package com.eycads.alona.sdk.zio.module

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.alpakka.s3._
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{IOResult, Materializer}
import akka.util.ByteString
import akka.{Done, NotUsed}
import com.eycads.alona.sdk.zio.impl.objectstorage.AlpakkaS3
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, AwsCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.regions.providers.AwsRegionProvider
import zio.{Has, Layer, Runtime, Task, ZIO}

import java.nio.file.Paths
import scala.concurrent.{ExecutionContextExecutor, Future}

object ObjectStorage {

  type ObjectStorage = Has[ObjectStorage.Service]
  case class FileInfo(length: Long)

  trait Service {
    def download(bucket: String, key: String)(implicit mat: Materializer): ZIO[ObjectStorage, Throwable, (Source[ByteString, NotUsed], ObjectMetadata)]
    def multipartUpload(bucket: String, path: String, key: String): ZIO[ObjectStorage, Throwable, Sink[ByteString, Future[Done]]]
  }

  def download(bucket: String, key: String)(implicit mat: Materializer): ZIO[ObjectStorage, Throwable, (Source[ByteString, NotUsed], ObjectMetadata)] = {
    ZIO.accessM(_.get.download(bucket, key))
  }

  def multipartUpload(bucket: String, path: String, key: String): ZIO[ObjectStorage, Throwable, Sink[ByteString, Future[Done]]] = {
    ZIO.accessM(_.get.multipartUpload(bucket, path, key))
  }

}

