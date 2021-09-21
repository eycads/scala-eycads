package com.eycads.alona.sdk.zio.impl.cassandra

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.ResultSet
import com.datastax.oss.driver.internal.core.cql.{MultiPageResultSet, SinglePageResultSet}
import com.eycads.alona.sdk.zio.module.Cassandra
import com.eycads.alona.sdk.zio.module.Cassandra.{Cassandra, Service}
import zio.{Has, Layer, Task, ZIO, ZLayer}

object DatastaxCassandra {

  def live(cqlSession: CqlSession): Layer[Throwable, Has[Cassandra.Service]] = {
    ZLayer.succeed[Cassandra.Service](DatastaxCassandra(cqlSession))
  }

  def createLive(cqlSession: CqlSession): Layer[Throwable, Cassandra] =
    live(cqlSession)

}

case class DatastaxCassandra(cqlSession: CqlSession) extends Service {

  override def executeQueryWithResult(query: String): ZIO[Cassandra, Throwable, ResultSet] = {
    for {
      res <- Task(cqlSession.execute(query))
    } yield res
  }

  override def executeQuery(query: String): ZIO[Cassandra, Throwable, String] = {
    for {
      res <- Task(cqlSession.execute(query))
      message <- Task("Success")
    } yield message
  }

}
