package com.eycads.alona.account.api.program

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorRefResolver, ActorSystem, Behavior, Scheduler}
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.pattern.StatusReply
import akka.util.Timeout
import com.eycads.alona.account.actors.UserEntity
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import com.eycads.alona.account.AccountServerApp.logger
import com.eycads.protocols.sdk.Gender
import com.eycads.protocols.sdk.uniqueConstraint.commands.AddUniqueConstraint
import com.eycads.protocols.user.commands.{CreateUserCompact, UserCreated}
import com.eycads.alona.sdk.common.Types.ActorRefC
import com.eycads.alona.sdk.actor.common.UniqueConstraints
import com.eycads.alona.sdk.enums.HashConfig
import com.eycads.sdk.protocols.{ActorCommand, CommandWithReplyTo, Event, RelationCommand, Response, UniqueConstraintCommand, UniqueConstraintEvent, UserCommand}
import com.eycads.alona.sdk.utils.UniqueConstraintUtils
import com.eycads.alona.sdk.zio.Environment.{ApiEnv, EventSourcedActorEnv}
import com.eycads.alona.sdk.zio.impl.actor.AkkaActor
import com.eycads.alona.sdk.zio.impl.hash.PBKDF2Hash
import com.eycads.alona.sdk.zio.impl.generator.ScalaGenerator
import com.eycads.alona.sdk.zio.impl.logger.ScalaLogger
import com.eycads.alona.sdk.zio.module.{Actor, Generator, Logger}
import com.eycads.alona.sdk.zio.types.ErrorTypes.ZioError
import zio.{Layer, Runtime, ZIO}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

object UserApiProgram extends LazyLogging {

  def createUserCompact[AS](
    userActorRef: ActorRefC[UserCommand],
    uniqueUserNameActorRef: ActorRefC[UniqueConstraintCommand]
  )(implicit timeout: Timeout, system: ActorSystem[AS]): ZIO[ApiEnv, ZioError, Event] = {
    for {
      _ <- Logger.info("Start")
      id <- Generator.generateUUIDString
      cmd = CreateUserCompact(
        id = id,
        userName = "djrumix3",
        firstName = "Rumel",
        lastName = "Docdoc",
        email = Some("rumel.docdoc@eycads.com"),
        number = Some("09498545058"),
        gender = Gender.MALE,
        ownerId = "alona-acc",
        skipParentConfirmation = true
      )
      res <- Actor.askWithStatus[UserCommand, UserCreated](
        userActorRef,
        "accountId",
        id,
        cmd,
      )
      _ <- Logger.info("res: " + res)
//      res2 <- Actor.askWithStatusFunc[UniqueConstraintCommand, UniqueConstraintEvent](
//        uniqueUserNameActorRef,
//        x => {
//          UniqueConstraintUtils.createAddUniqueConstraint(
//            cmd.userName,
//            "alona-acc",
//            x,
//            UniqueConstraints.ACCOUNT_NAME,
//            isStatusReply = true
//          )(system)
//        }
//      )
//      _ <- Logger.info("res2: " + res2)
//      res3 <- Actor.askWithStatus[UniqueConstraintCommand, UniqueConstraintEvent](
//        uniqueUserNameActorRef,
//        "accountId",
//        cmd.userName,
//        AddUniqueConstraint(
//          id = "accountId",
//          ownerId = "alona-acc",
//          code = UniqueConstraints.ACCOUNT_NAME.code,
//          value = cmd.userName,
//        )
//      )
//      _ <- Logger.info("res3: " + res3)
      res4 <- Actor.askWithStatusF[UniqueConstraintCommand, UniqueConstraintEvent](
        uniqueUserNameActorRef,
        UniqueConstraintUtils.createAddUniqueConstraintF(
          cmd.userName,
          "alona-acc",
          UniqueConstraints.ACCOUNT_NAME,
          isStatusReply = true
        )(system)
      )
      _ <- Logger.info("res4: " + res4)
    } yield res
  }

//  def main(args: Array[String]): Unit = {
//    val conf: Config = ConfigFactory.load("application")
//    ActorSystem[Nothing](apiBehavior, "server-system", conf)
//  }
}
