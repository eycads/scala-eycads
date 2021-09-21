package com.eycads.alona.sdk.zio.impl.objectstorage

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.{Attributes, Materializer}
import akka.stream.alpakka.s3.{ApiVersion, MemoryBufferType, ObjectMetadata, S3Attributes, S3Ext, S3Settings}
import akka.stream.alpakka.s3.scaladsl.S3
import akka.{Done, NotUsed}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import com.amazonaws.services.s3.AmazonS3
import com.eycads.alona.sdk.zio.module.ObjectStorage
import com.eycads.alona.sdk.zio.module.ObjectStorage.{FileInfo, ObjectStorage}
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, AwsCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.regions.providers.AwsRegionProvider
import zio.{Layer, Task, ZIO, ZLayer}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

case class AlpakkaS3(settings: S3Settings)(implicit executionContext: ExecutionContext) extends ObjectStorage.Service {

  private val attributes: Attributes = S3Attributes.settings(settings)

  def download(bucket: String, key: String)(implicit mat: Materializer): ZIO[ObjectStorage, Throwable, (Source[ByteString, NotUsed], ObjectMetadata)] = {
//    val a: Source[Option[(Source[ByteString, NotUsed], ObjectMetadata)], NotUsed] =
//      S3.download(bucket, key)
//        .withAttributes(attributes)
//    val b: Source[Option[(Source[ByteString, NotUsed], ObjectMetadata)], NotUsed] =
//      a.take(1L)
//    val c: Future[List[(Source[ByteString, NotUsed], FileInfo)]] =
//      b.runFold(List.empty[(Source[ByteString, NotUsed], FileInfo)])({
//        case (s, Some((source, meta))) => (source -> FileInfo(meta.contentLength)) :: s
//        case (s, _) => s
//      })
//    val d: Future[Option[(Source[ByteString, NotUsed], FileInfo)]] =
//      c.map(_.headOption)
    val s3DownloadData =
      S3.download(bucket, key)
        .withAttributes(attributes)
        .take(1L)
        .runFold(List.empty[(Source[ByteString, NotUsed], ObjectMetadata)])({
          case (s, Some((source, meta))) => (source -> meta) :: s
          case (s, _) => s
        })
        .map(_.headOption)

    for {
      res <- ZIO.fromFuture(ec => s3DownloadData).some
        .mapError(ex => ex.fold(new Throwable("No Element"))(ex => new Throwable(ex)))
    } yield res
  }

  override def multipartUpload(bucket: String, path: String, key: String): ZIO[ObjectStorage, Throwable, Sink[ByteString, Future[Done]]] = {
    val s3MultipartUpload = S3.multipartUpload(bucket, s"$path/$key")
      .withAttributes(attributes)
      .mapMaterializedValue(uploadFinish => {
        uploadFinish.map(result => {
          Done
        })
      })

    for {
      res <- ZIO.effect(s3MultipartUpload)
    } yield res
  }
}

object AlpakkaS3 {

  private def live(settings: S3Settings)(implicit executionContext: ExecutionContext): Layer[Throwable, ObjectStorage] =
    ZLayer.succeed[ObjectStorage.Service](AlpakkaS3(settings))

  def createLive(settings: S3Settings)(implicit executionContext: ExecutionContext): Layer[Throwable, ObjectStorage] =
    live(settings)

  implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](Behaviors.empty, "HttpClientModuleTest")
  implicit val ec: ExecutionContextExecutor = system.executionContext

  def main(args: Array[String]): Unit = {
    import akka.actor.typed.scaladsl.adapter._
    val s3Host      = "http://alona-audio-files.s3.eu-west-2.amazonaws.com/"
    val s3AccessKey = "xxxxx"
    val s3SecretKey = "xxxxxxx"
    val s3Region    = "eu-west-2"
    val credentialsProvider: AwsCredentialsProvider = new AwsCredentialsProvider {
      override def resolveCredentials(): AwsBasicCredentials = AwsBasicCredentials.create(s3AccessKey, s3SecretKey)
    }
    val regionProvider: AwsRegionProvider = new AwsRegionProvider {
      override def getRegion: Region = Region.of(s3Region)
    }
    val settings: S3Settings = S3Ext(system.toClassic).settings
      .withEndpointUrl(s3Host)
      .withBufferType(MemoryBufferType)
      .withCredentialsProvider(credentialsProvider)
      .withListBucketApiVersion(ApiVersion.ListBucketVersion2)
      .withS3RegionProvider(regionProvider)
    val attributes: Attributes = S3Attributes.settings(settings)

    val a: Source[Option[(Source[ByteString, NotUsed], ObjectMetadata)], NotUsed] =
      S3.download("alona-audio-files", "00177488e83946d39940eb35d9cb41b1/8000.wav")
        .withAttributes(attributes)
    val b: Source[Option[(Source[ByteString, NotUsed], ObjectMetadata)], NotUsed] =
      a.take(1L)
    val c: Future[List[(Source[ByteString, NotUsed], FileInfo)]] =
      b.runFold(List.empty[(Source[ByteString, NotUsed], FileInfo)])({
        case (s, Some((source, meta))) => (source -> FileInfo(meta.contentLength)) :: s
        case (s, _)                    => s
      })
    val d: Future[Option[(Source[ByteString, NotUsed], FileInfo)]] =
      c.map(_.headOption)

//    d.onComplete {
//      case Success(opt) => println("opt: " + opt)
//      case Failure(ex) => println("ex: " + ex)
//    }

    val e: Sink[ByteString, Future[Done]] =
      S3.multipartUpload("alona-audio-file", "alona/alona.wav")
        .withAttributes(attributes)
        .mapMaterializedValue(uploadFinish => {
          uploadFinish.map(result => {
            Done
          })
        })

    d.onComplete {
      case Success(Some(opt)) => {
        println("opt: " + opt)
        opt._1.toMat(e)(Keep.left).run()
      }
      case Failure(ex) => println("ex: " + ex)
    }

  }
}
