package com.eycads.alona.sdk.zio.impl.http

import akka.actor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse, StatusCodes}
import com.eycads.alona.sdk.zio.module.HttpClient
import com.eycads.alona.sdk.zio.module.HttpClient.{HttpClient, HttpClientDependency, HttpClientEnv}
import zio.{Has, Layer, Schedule, Task, ZIO, ZLayer}

import scala.concurrent.ExecutionContextExecutor

case class AkkaHttpClient() extends HttpClient.Service {

  override def executeRequest(httpRequest: HttpRequest): ZIO[HttpClientEnv, Throwable, HttpResponse] = {
    ZIO.accessM { dependencies =>
      implicit val system: actor.ActorSystem = dependencies.get[HttpClientDependency].actorSystem.toClassic
      implicit val ec: ExecutionContextExecutor = system.dispatcher
      for {
        res <- Task.fromFuture(_ =>
          Http().singleRequest(httpRequest))
            .flatMap {
              case r if r.status == StatusCodes.OK => Task.succeed(r)
              case _ => Task.fail(throw new Exception(s"Request failing, system: ${system.name}, thread: ${Thread.currentThread().getName}"))
            }
            .catchAll(e =>
              Task.succeed(
                HttpResponse(entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"Too many tries, $e \n"))
              )
            )
      } yield res
    }

  }

  override def executeRequestAny(httpRequest: HttpRequest): ZIO[HttpClientEnv, Throwable, HttpResponse] = {
    ZIO.accessM { dependencies =>
      implicit val system: actor.ActorSystem = dependencies.get[HttpClientDependency].actorSystem.toClassic
      implicit val ec: ExecutionContextExecutor = system.dispatcher
      for {
        res <- Task.fromFuture(_ =>
          Http().singleRequest(httpRequest))
            .flatMap(r => Task.succeed(r))
            .catchAll(e =>
              Task.succeed(
                HttpResponse(entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"Too many tries, $e \n"))
              )
            )
        _ = res.discardEntityBytes()
      } yield res
    }
  }
}

object AkkaHttpClient {

  val live: Layer[Throwable, Has[HttpClient.Service]] =
    ZLayer.succeed[HttpClient.Service](AkkaHttpClient())

  def liveDependency(implicit actorSystem: ActorSystem[Nothing]): Layer[Throwable, Has[HttpClientDependency]] =
    ZLayer.succeed(HttpClientDependency(actorSystem))

  def createLive()(implicit actorSystem: ActorSystem[Nothing]): Layer[Throwable, HttpClient] ={
    liveDependency >>> live
  }

}

