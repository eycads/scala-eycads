package com.eycads.alona.sdk.common

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.ShardingEnvelope
import com.eycads.sdk.protocols.ActorCommand

object Types {

  type TypeId = Int
  type ActorRefC[C] = ActorRef[ShardingEnvelope[ActorCommand[C]]]

}
