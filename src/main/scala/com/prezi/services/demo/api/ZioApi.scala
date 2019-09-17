package com.prezi.services.demo.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.prezi.services.demo.Main
import com.prezi.services.demo.api.directives.ZioDirectives
import com.prezi.services.demo.core.Interop
import com.prezi.services.demo.dependencies.ZioDep
import de.heikoseeberger.akkahttpcirce.BaseCirceSupport

import scala.util.{Failure, Success}

/**
 * Example of evaluating a ZIO effect to create a HTTP response
 */
trait ZioApi extends ZioDirectives[Main.FinalEnvironment] {
  this: ErrorResponses with BaseCirceSupport  =>

  // dependencies
  implicit val interop: Interop[Main.FinalEnvironment]
  val zioDep: ZioDep.Service

  val zioRoute: Route =
    path("zio") {
      get {
        parameter('input.as[Int]) { input =>
          val zioAnswer = zioDep.provideAnswer(input)
          onRIOComplete(zioAnswer) {
            case Failure(reason) =>
              respondWithError(reason)
            case Success(answer) =>
              complete(answer)
          }
        }
      }
    }

}
