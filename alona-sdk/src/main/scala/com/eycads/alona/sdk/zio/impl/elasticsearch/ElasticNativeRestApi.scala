package com.eycads.alona.sdk.zio.impl.elasticsearch

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCode}
import com.eycads.alona.sdk.zio.module.{ElasticSearch, HttpClient}
import com.eycads.alona.sdk.zio.module.ElasticSearch.{ElasticSearch, ElasticSearchConfig, ElasticSearchEnv}
import com.eycads.alona.sdk.zio.module.HttpClient.HttpClientEnv
import zio.{Has, Layer, Task, ZIO, ZLayer}

case class ElasticNativeRestApi() extends ElasticSearch.Service {

  override def indexExists(index: String, config: ElasticSearchConfig):
      ZIO[ElasticSearchEnv, Throwable, HttpResponse] = {
    val request = HttpRequest(
      method = HttpMethods.HEAD,
      uri = s"http://${config.host}:${config.port}/$index"
    )

    for {
      httpResponse <- HttpClient.executeRequestAny(request)
    } yield httpResponse
  }

  override def createIndex(index: String, config: ElasticSearchConfig, body: String):
      ZIO[ElasticSearchEnv, Throwable, HttpResponse] = {
    val request = HttpRequest(
      method = HttpMethods.PUT,
      uri = s"http://${config.host}:${config.port}/$index",
      entity = HttpEntity(ContentTypes.`application/json`, body)
    )

    for {
      httpResponse <- HttpClient.executeRequestAny(request)
    } yield httpResponse
  }

  override def deleteIndex(index: String, config: ElasticSearchConfig):
      ZIO[ElasticSearchEnv, Throwable, HttpResponse] = {
    val request = HttpRequest(
      method = HttpMethods.DELETE,
      uri = s"http://${config.host}:${config.port}/$index",
    )

    for {
      httpResponse <- HttpClient.executeRequestAny(request)
    } yield httpResponse
  }

  override def createDoc(index: String, config: ElasticSearchConfig, id: String, doc: String):
      ZIO[ElasticSearchEnv, Throwable, HttpResponse] = {
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = s"http://${config.host}:${config.port}/$index/_create/$id",
      entity = HttpEntity(ContentTypes.`application/json`, doc)
    )

    for {
      _ <- Task(println(request))
      httpResponse <- HttpClient.executeRequestAny(request)
    } yield httpResponse
  }

  override def updateDoc(index: String, config: ElasticSearchConfig, id: String, doc: String):
      ZIO[ElasticSearchEnv, Throwable, HttpResponse] = {
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = s"http://${config.host}:${config.port}/$index/_update/$id",
      entity = HttpEntity(ContentTypes.`application/json`, doc)
    )

    for {
      _ <- Task(println(request))
      httpResponse <- HttpClient.executeRequestAny(request)
    } yield httpResponse
  }
}

object ElasticNativeRestApi {

  val live: Layer[Throwable, Has[ElasticSearch.Service]] =
    ZLayer.succeed[ElasticSearch.Service](ElasticNativeRestApi())

  def createLive(httpClientLayer: Layer[Throwable, HttpClientEnv]): Layer[Throwable, ElasticSearch] = {
    httpClientLayer >>> live
  }

}
