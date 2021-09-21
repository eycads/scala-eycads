package com.eycads.alona.account.api.routes

import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.util.Timeout
import com.eycads.alona.account.actors.UserEntity
import com.eycads.alona.account.api.program.UserApiProgram
import com.google.`type`.date.Date
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import com.eycads.protocols.sdk.Gender
import com.eycads.protocols.sdk.relation.ParentRelationOverride
import com.eycads.protocols.user.commands.{CreateUserCompact, CreateUserCompact2}
import com.eycads.alona.sdk.actor.common.{UniqueConstraintActor, UniqueConstraints}
import com.eycads.alona.sdk.enums.HashConfig
import com.eycads.sdk.protocols.{ActorCommand, UniqueConstraintCommand}
import com.eycads.alona.sdk.server.EycadsRoutes
import com.eycads.alona.sdk.zio.Environment.{ApiEnv, EventSourcedActorEnv}
import com.eycads.alona.sdk.zio.impl.actor.AkkaActor
import com.eycads.alona.sdk.zio.impl.hash.PBKDF2Hash
import com.eycads.alona.sdk.zio.impl.generator.ScalaGenerator
import com.eycads.alona.sdk.zio.impl.logger.ScalaLogger
import com.eycads.alona.sdk.zio.types.ErrorTypes.ZioError
import io.circe.Decoder
import io.circe.generic.semiauto
import zio.{Layer, Runtime, ZIO}

import scala.concurrent.duration.DurationInt

class UserApiRoutes(context: ActorContext[Nothing]) extends EycadsRoutes with LazyLogging {

  implicit val system: ActorSystem[Nothing] = context.system
  implicit val timeout: Timeout = 30.seconds
  implicit val scheduler: Scheduler = system.scheduler
  val cs: ClusterSharding = ClusterSharding(system )
  val runtime = Runtime.default

  val liveApi = AkkaActor.createLive(context) ++
    PBKDF2Hash.createLive(HashConfig.PBKDF2v1) ++
    ScalaGenerator.createLive ++
    ScalaLogger.createLive(logger)
  implicit val live: Layer[Any, EventSourcedActorEnv] =
    AkkaActor.createLive(context) ++ ScalaLogger.createLive(logger)

  val userActorRef = UserEntity.init(system, cs)
  val uniqueUserNameActorRef: ActorRef[ShardingEnvelope[ActorCommand[UniqueConstraintCommand]]] = {
    UniqueConstraintActor.init(system, cs, UniqueConstraints.USER_NAME)
  }

  override def routes: Route = {
    import io.circe.generic.extras.auto._
    import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
    import io.circe.generic.semiauto._

//    implicit val fooDecoder: Decoder[CreateUserCompact2] = deriveDecoder[CreateUserCompact2]

    path("user") {
      post {
        entity(as[String]) {
          req => {
            println("req: " + req)
            val program = UserApiProgram.createUserCompact(userActorRef, uniqueUserNameActorRef)
            val res = runtime.unsafeRun(program.provideCustomLayer(liveApi))
            complete("res")
          }
        }
      }
    }

  }

}
