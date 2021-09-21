package com.eycads.alona.sdk.zio.module

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.HttpMessage.DiscardedEntity
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse}
import com.eycads.alona.sdk.zio.impl.http.AkkaHttpClient
import zio.{Has, Layer, Runtime, ZIO}

import scala.concurrent.ExecutionContextExecutor

object HttpClient {

  type HttpClient = Has[HttpClient.Service]

  case class HttpClientDependency(
      actorSystem: ActorSystem[Nothing]
  )

  type HttpClientEnv = HttpClient with Has[HttpClientDependency]

  trait Service {
    def executeRequest(httpRequest: HttpRequest): ZIO[HttpClientEnv, Throwable, HttpResponse]
    def executeRequestAny(httpRequest: HttpRequest): ZIO[HttpClientEnv, Throwable, HttpResponse]
  }

  def executeRequest(httpRequest: HttpRequest): ZIO[HttpClientEnv, Throwable, HttpResponse] = {
    ZIO.accessM(_.get.executeRequest(httpRequest))
  }

  def executeRequestAny(httpRequest: HttpRequest): ZIO[HttpClientEnv, Throwable, HttpResponse] = {
    ZIO.accessM(_.get.executeRequestAny(httpRequest))
  }

  def main(args: Array[String]): Unit = {
    val myRuntime = Runtime.default

    implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](Behaviors.empty, "HttpClientModuleTest")
    implicit val ec: ExecutionContextExecutor = system.executionContext

    val liveHttpClient: Layer[Throwable, HttpClient] = AkkaHttpClient.createLive()

    val serverEnv: Layer[Throwable, HttpClientEnv] = liveHttpClient ++ AkkaHttpClient.liveDependency

    def execute(httpRequest: HttpRequest): ZIO[HttpClientEnv, Throwable, HttpResponse] = {
      for {
        res <- HttpClient.executeRequest(httpRequest)
      } yield res
    }

    val a: HttpResponse = myRuntime.unsafeRun(execute(HttpRequest(
      HttpMethods.HEAD,
      "http://localhost:9200/service"
    )).provideCustomLayer(serverEnv))
    println(a)

    val discarded: DiscardedEntity = a.discardEntityBytes()
    discarded.future.onComplete { done => println("Entity discarded completely!") }

  }

}

