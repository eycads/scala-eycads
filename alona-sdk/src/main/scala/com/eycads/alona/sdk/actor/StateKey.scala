package com.eycads.alona.sdk.actor

import com.eycads.sdk.protocols.WithStateData

trait StateKey[+T] {
  val key: String
  val defaultInstance: T

  def getWithStateDataInstance[SD]: Option[WithStateData[SD]] = {
    defaultInstance match {
      case state: WithStateData[SD] => Some(state)
      case _ => None
    }
  }
}

object StateKey {
  def apply[A](name: A): StateKey[A] = new StateKey[A] {
    override val key: String = name.getClass.getSimpleName
    override val defaultInstance: A = name
  }
//  def apply[A](implicit S: StateKey[A]): StateKey[A] = S

  def instance[A](keyInit: String): StateKey[A] = new StateKey[A] {
    override val key: String = keyInit
    override val defaultInstance: A = ???
  }

  def getKey[S, R]: PartialFunction[(StateKey[S], R), (String, R)] = {
    case (k, v) => k.key -> v
  }
}
