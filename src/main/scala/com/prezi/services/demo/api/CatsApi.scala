package com.prezi.services.demo.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.prezi.services.demo.Main
import com.prezi.services.demo.core.Interop
import com.prezi.services.demo.dependencies.CatsDep
import de.heikoseeberger.akkahttpcirce.BaseCirceSupport

import scala.util.{Failure, Success}

trait CatsApi {
  this: ErrorResponses with BaseCirceSupport =>

  implicit val interop: Interop[Main.FinalEnvironment]
  val catsDep: CatsDep.Service

  val catsRoute: Route =
    path("cats") {
      get {
        parameter('input.as[Int]) { input =>
          val ioAnswer = catsDep.provideAnswer(input)
          val futureAnswer = interop.ioToFuture(ioAnswer)

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
