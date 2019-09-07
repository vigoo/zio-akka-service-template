package com.prezi.services.demo.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.prezi.services.demo.dependencies.FutureDep
import de.heikoseeberger.akkahttpcirce.BaseCirceSupport

import scala.util.{Failure, Success}

trait FutureApi {
  this: ErrorResponses with BaseCirceSupport =>

  val futureDep: FutureDep.Service

  val futureRoute: Route =
    path("future") {
      get {
        parameter('input.as[Int]) { input =>
          val futureAnswer = futureDep.provideAnswer(input)
          onComplete(futureAnswer) {
            case Failure(reason) =>
              respondWithError(reason)
            case Success(answer) =>
              complete(answer)
          }
        }
      }
    }
}
