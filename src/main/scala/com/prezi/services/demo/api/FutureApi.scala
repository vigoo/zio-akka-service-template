package com.prezi.services.demo.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.prezi.services.demo.dependencies.futureDep.FutureDep
import de.heikoseeberger.akkahttpcirce.BaseCirceSupport

import scala.util.{Failure, Success}

/**
 * Example of evaluating a Future value to create a response
 */
trait FutureApi {
  this: ErrorResponses with BaseCirceSupport =>

  // dependencies
  val futureDep: FutureDep.Service

  val futureRoute: Route =
    path("future") {
      get {
        parameter("input".as[Int]) { input =>
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
