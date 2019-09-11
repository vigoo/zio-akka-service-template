package com.prezi.services.demo.api

import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

trait Api
  extends FutureApi
    with CatsApi
    with ZioApi
    with ActorApi
    with ErrorResponses
    with FailFastCirceSupport {

  val route: Route = futureRoute ~ catsRoute ~ zioRoute ~ actorRoute
}
