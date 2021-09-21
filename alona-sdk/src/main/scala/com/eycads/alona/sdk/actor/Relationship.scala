package com.eycads.alona.sdk.actor

trait Relationship
trait CreationTimeRelationship

trait ManyToOne[M, O] extends CreationTimeRelationship {
  val entityName: Int
  val foreignEntityName: Int
}
trait OneToMany[O, M] extends CreationTimeRelationship

object ManyToOne {
  def apply[M, O](): ManyToOne[M, O] = new ManyToOne[M, O] {
    val entityName = 1
    val foreignEntityName = 2
  }
}

case class CreationTimeSettings(
  parentId: Int,
  ownerId: Int,
  relationship: CreationTimeRelationship
)


