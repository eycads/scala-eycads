package com.eycads.alona.sdk.zio.module

import com.datastax.oss.driver.api.core.cql.ResultSet
import zio.{Has, ZIO}

object Cassandra {

  type Cassandra = Has[Cassandra.Service]

  trait Service {
    def executeQueryWithResult(query: String): ZIO[Cassandra, Throwable, ResultSet]
    def executeQuery(query: String): ZIO[Cassandra, Throwable, String]
  }

  def executeQuery(query: String): ZIO[Cassandra, Throwable, String] = {
    ZIO.accessM(_.get.executeQuery(query))
  }

  def executeQueryWithResult(script: String): ZIO[Cassandra, Throwable, ResultSet] = {
    ZIO.accessM(_.get.executeQueryWithResult(script))
  }

}
