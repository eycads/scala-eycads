package com.eycads.alona.account

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorRefResolver, ActorSystem, Behavior, Scheduler}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.ShardCoordinator
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardingEnvelope}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.pattern.StatusReply
import akka.util.Timeout
import com.eycads.alona.account.actors.{AccountEntity, UserEntity}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import com.eycads.alona.account.actors.UserEntity
import com.eycads.alona.account.api.program.UserApiProgram.{createUserCompact, logger}
import com.eycads.protocols.account.commands.{CreateAccount, GetAccountDetails, UpdateAccount}
import com.eycads.protocols.account.events.{AccountCreated, AccountDetailsRetrieved}
import com.eycads.protocols.sdk.ShardingStrategy
import com.eycads.protocols.sdk.relation.commands.GetRelationData
import com.eycads.protocols.sdk.relation.events.RelationDataRetrieved
import com.eycads.protocols.sdk.uniqueConstraint.commands.AddUniqueConstraint
import com.eycads.alona.sdk.actor.interface.{EntityProxy, EntityRole}
import com.eycads.alona.sdk.actor.common.{UniqueConstraintActor, UniqueConstraints}
import com.eycads.alona.sdk.enums.HashConfig
import com.eycads.sdk.protocols.{AccountCommand, AccountEvent, ActorCommand, ActorMeta, UniqueConstraintCommand, UniqueConstraintEvent}
import com.eycads.alona.sdk.utils.UniqueConstraintUtils
import com.eycads.alona.sdk.zio.Environment.EventSourcedActorEnv
import com.eycads.alona.sdk.zio.impl.actor.AkkaActor
import com.eycads.alona.sdk.zio.impl.hash.PBKDF2Hash
import com.eycads.alona.sdk.zio.impl.generator.ScalaGenerator
import com.eycads.alona.sdk.zio.impl.logger.ScalaLogger
import com.eycads.alona.sdk.zio.module.Actor.Actor
import com.eycads.alona.sdk.zio.module.Logger
import com.eycads.alona.sdk.zio.module.Logger.Logger
import com.eycads.alona.sdk.zio.types.ErrorTypes
import com.eycads.alona.sdk.zio.types.ErrorTypes.ZioError
import zio.{Layer, Runtime, ZIO}

import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object AccountServerApp extends LazyLogging {
  val conf: Config = ConfigFactory.load("application")

  def main(args: Array[String]): Unit = {
    ActorSystem[Nothing](apiBehavior, "server-system", conf)
  }

  def apiBehavior: Behavior[Nothing] = {
    Behaviors.setup[Nothing] { context =>
      val runtime = Runtime.default
      val system: ActorSystem[Nothing] = context.system
      implicit val scheduler: Scheduler = system.scheduler
      implicit val timeout: Timeout = 1.minute
      implicit val ec: ExecutionContextExecutor = system.executionContext
      val liveApi = AkkaActor.createLive(context) ++
        PBKDF2Hash.createLive(HashConfig.PBKDF2v1) ++
        ScalaGenerator.createLive ++
        ScalaLogger.createLive(logger)
      implicit val live: Layer[Any, EventSourcedActorEnv] =
        AkkaActor.createLive(context) ++ ScalaLogger.createLive(logger)
      val cs: ClusterSharding = ClusterSharding(system)
      val userActorRef = UserEntity.init(system, cs)
      val uniqueUserNameActorRef: ActorRef[ShardingEnvelope[ActorCommand[UniqueConstraintCommand]]] = {
        UniqueConstraintActor.init(system, cs, UniqueConstraints.USER_NAME)
      }
      val program = createUserCompact(userActorRef, uniqueUserNameActorRef)(timeout, system)
        .foldM(
          err => Logger.error("fail"),
          success => ZIO.succeed(success)
        )
      val wew = runtime.unsafeRun(program.provideCustomLayer(liveApi))
      println("wew: " + wew)
      Behaviors.empty
    }
  }

  def serverBehavior: Behavior[Nothing] = {
    Behaviors.setup[Nothing] { context =>
      val system: ActorSystem[Nothing] = context.system
      implicit val scheduler: Scheduler = system.scheduler
//      implicit val timeout: Timeout = 30.seconds
      implicit val timeout: Timeout = 1.minute
      implicit val ec: ExecutionContextExecutor = system.executionContext
      val settings = ClusterShardingSettings(system).withRole("AccountRole")
//      val cs2: ShardCoordinator.ShardAllocationStrategy = ClusterSharding(system).defaultShardAllocationStrategy(settings)
      implicit val live: Layer[Any, EventSourcedActorEnv] =
        AkkaActor.createLive(context) ++ ScalaLogger.createLive(logger)
      val cs: ClusterSharding = ClusterSharding(system)
      val accountActorRef: ActorRef[ShardingEnvelope[ActorCommand[AccountCommand]]] = AccountEntity.init(system, cs)
      val shardId = "" + java.util.UUID.randomUUID
      val shardIdCreateSkip = "" + java.util.UUID.randomUUID

      val uniqueAccountNameActorRef: ActorRef[ShardingEnvelope[ActorCommand[UniqueConstraintCommand]]] =
        UniqueConstraintActor.init(
          system,
          cs,
          UniqueConstraints.ACCOUNT_NAME
        )
      val uniqueId = "" + java.util.UUID.randomUUID

      val addCons = uniqueAccountNameActorRef.askWithStatus[UniqueConstraintEvent](ref => {
        UniqueConstraintUtils.createAddUniqueConstraint(
          "Alona",
          uniqueId,
          ref,
          UniqueConstraints.ACCOUNT_NAME,
          isStatusReply = true
        )(system)
      })
      addCons.map(x => println("addCons: " + addCons))
      val removeCons = uniqueAccountNameActorRef.askWithStatus[UniqueConstraintEvent](ref => {
        UniqueConstraintUtils.createRemoveUniqueConstraint(
          "Alona2",
          uniqueId,
          ref,
          UniqueConstraints.ACCOUNT_NAME,
          isStatusReply = true
        )(system)
      })
      removeCons.onComplete {
        case Success(value) => println("removeCons: " + value)
        case Failure(ex) => println("removeCons error: " + ex)
      }

//      val createSkip = accountActorRef.ask[AccountCreated](ref =>{
//        val actRef = ActorRefResolver(system).toSerializationFormat(ref)
//        ShardingEnvelope(shardIdCreateSkip, ActorCommand.create("accountId",
//          CreateAccount(
//            shardIdCreateSkip,
//            1296,
//            Some(actRef),
//            ownerId = "alona-acc",
//            skipParentConfirmation = true
//          )
//        ))
//      })
//      createSkip.map(x => println("createSkip: " + createSkip))
//      val get1 = accountActorRef.ask[AccountDetailsRetrieved](ref => {
//        val actRef = ActorRefResolver(system).toSerializationFormat(ref)
//        ShardingEnvelope(shardIdCreateSkip, ActorCommand.create("accountId", GetAccountDetails(Some(actRef))))
//      })
//      get1.map(x => println("createSkipGet: " + get1))
//      val getRelationSkip = accountActorRef.ask[RelationDataRetrieved](ref => {
//        val actRef = ActorRefResolver(system).toSerializationFormat(ref)
//        ShardingEnvelope(shardIdCreateSkip, ActorCommand.create("accountId", GetRelationData(replyTo = Some(actRef))))
//      })
//      getRelationSkip.map(x => println("createSkipGetRelation: " + getRelationSkip))
//      Thread.sleep(5000)
//      println("xxxxxxxxxxxxxxxxxxxxxxxxx")
//      println("")
//
//      val create1 = accountActorRef.ask[AccountCreated](ref =>{
//        val actRef = ActorRefResolver(system).toSerializationFormat(ref)
//        ShardingEnvelope(shardId, ActorCommand.create("accountId",
//          CreateAccount(
//            shardId,
//            1296,
//            Some(actRef),
//            ownerId = shardIdCreateSkip
//          ))
//        )
//      })
//      create1.map(x => println("create1: " + create1))
//      Thread.sleep(5000)
//      val get2 = accountActorRef.ask[AccountEvent](ref => {
//        val actRef = ActorRefResolver(system).toSerializationFormat(ref)
//        ShardingEnvelope(shardId, ActorCommand.create("accountId", GetAccountDetails(Some(actRef))))
//      })
//      get2.map(x => println("get2: " + get2))
//      val getRelation = accountActorRef.ask[RelationDataRetrieved](ref => {
//        val actRef = ActorRefResolver(system).toSerializationFormat(ref)
//        ShardingEnvelope(shardId, ActorCommand.create("accountId", GetRelationData(replyTo = Some(actRef))))
//      })
//      getRelation.map(x => println("getRelation: " + getRelation))
//      val getRelationSkip2 = accountActorRef.ask[RelationDataRetrieved](ref => {
//        val actRef = ActorRefResolver(system).toSerializationFormat(ref)
//        ShardingEnvelope(shardIdCreateSkip, ActorCommand.create("accountId", GetRelationData(replyTo = Some(actRef))))
//      })
//      getRelationSkip2.map(x => println("getRelationSkip2: " + getRelationSkip2))


//      accountActorRef ! ShardingEnvelope(shardId, ActorCommand.create("accountId", SendCreationRequest(shardId, 1111)))
//      val get3 = accountActorRef.ask[AccountEvent](ref => {
//        val actRef = ActorRefResolver(system).toSerializationFormat(ref)
//        ShardingEnvelope(shardId, GetAccountDetails(Some(actRef)))
//      })
//      get3.map(x => println("get3: " + get3))
      Behaviors.empty
    }
  }

}
