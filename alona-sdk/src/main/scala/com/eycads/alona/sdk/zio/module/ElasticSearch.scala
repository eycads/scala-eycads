package com.eycads.alona.sdk.zio.module

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.HttpResponse
import com.eycads.alona.sdk.zio.impl.elasticsearch.ElasticNativeRestApi
import com.eycads.alona.sdk.zio.impl.http.AkkaHttpClient
import com.eycads.alona.sdk.zio.module.HttpClient.{HttpClient, HttpClientEnv}
import zio.{Has, Layer, Runtime, ZIO}

import scala.concurrent.ExecutionContextExecutor

object ElasticSearch {

  type ElasticSearch = Has[ElasticSearch.Service]

  type ElasticSearchEnv = ElasticSearch with HttpClientEnv

  case class ElasticSearchConfig(
      host: String,
      port: Int,
      index: String = "",
      filePath: String = ""
  )

  case class UpdateDocumentWrapper[A](
      doc: A,
      _source: Boolean = true
  )

  trait Service {
    def indexExists(index: String, config: ElasticSearchConfig): ZIO[ElasticSearchEnv, Throwable, HttpResponse]
    def createIndex(index: String, config: ElasticSearchConfig, body: String): ZIO[ElasticSearchEnv, Throwable, HttpResponse]
    def deleteIndex(index: String, config: ElasticSearchConfig): ZIO[ElasticSearchEnv, Throwable, HttpResponse]
    def createDoc(index: String, config: ElasticSearchConfig, id: String, doc: String): ZIO[ElasticSearchEnv, Throwable, HttpResponse]
    def updateDoc(index: String, config: ElasticSearchConfig, id: String, doc: String): ZIO[ElasticSearchEnv, Throwable, HttpResponse]
  }

  def indexExists(index: String, config: ElasticSearchConfig): ZIO[ElasticSearchEnv, Throwable, HttpResponse] = {
    ZIO.accessM(_.get.indexExists(index, config))
  }

  def createIndex(index: String, config: ElasticSearchConfig, body: String): ZIO[ElasticSearchEnv, Throwable, HttpResponse] = {
    ZIO.accessM(_.get.createIndex(index, config, body))
  }

  def deleteIndex(index: String, config: ElasticSearchConfig): ZIO[ElasticSearchEnv, Throwable, HttpResponse] = {
    ZIO.accessM(_.get.deleteIndex(index, config))
  }

  def createDoc(index: String, config: ElasticSearchConfig, id: String, doc: String):
      ZIO[ElasticSearchEnv, Throwable, HttpResponse] = {
    ZIO.accessM(_.get.createDoc(index, config, id, doc))
  }

  def updateDoc(index: String, config: ElasticSearchConfig, id: String, doc: String):
      ZIO[ElasticSearchEnv, Throwable, HttpResponse] = {
    ZIO.accessM(_.get.updateDoc(index, config, id, doc))
  }

  def main(args: Array[String]): Unit = {
    val runtime = Runtime.default

    implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](Behaviors.empty, "HttpClientModuleTest")
    implicit val ec: ExecutionContextExecutor = system.executionContext

    val liveHttpClient: Layer[Throwable, HttpClient] = AkkaHttpClient.createLive()
    val httpClientEnv: Layer[Throwable, HttpClientEnv] = liveHttpClient ++ AkkaHttpClient.liveDependency

    val liveElasticSearch: Layer[Throwable, ElasticSearch] = ElasticNativeRestApi.createLive(httpClientEnv)
    val elasticSearch: Layer[Throwable, ElasticSearchEnv] = liveElasticSearch ++ httpClientEnv

    def indexExists(index: String): ZIO[ElasticSearchEnv, Throwable, HttpResponse] = {
      for {
        res <- ElasticSearch.indexExists(index, ElasticSearchConfig("elastic.local", 9200))
      } yield res
    }
    val a = runtime.unsafeRun(indexExists("service").provideCustomLayer(elasticSearch))

//    def deleteIndex(index: String): ZIO[ElasticSearchEnv, Throwable, HttpResponse] = {
//      for {
//        res <- ElasticSearch.deleteIndex(index, ElasticSearchConfig("elastic.local", 9200))
//      } yield res
//    }
//    val a = runtime.unsafeRun(deleteIndex("alona2").provideCustomLayer(elasticSearch))


    println(a)
  }

}
