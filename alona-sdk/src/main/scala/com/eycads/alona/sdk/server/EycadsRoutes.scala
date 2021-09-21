package com.eycads.alona.sdk.server

import akka.http.scaladsl.server.{Directives, Route}

trait EycadsRoutes extends Directives {

  def routes: Route

}
