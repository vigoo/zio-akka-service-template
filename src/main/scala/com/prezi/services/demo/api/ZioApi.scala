package com.prezi.services.demo.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.prezi.services.demo.Main
import com.prezi.services.demo.core.Interop
import com.prezi.services.demo.dependencies.ZioDep
import de.heikoseeberger.akkahttpcirce.BaseCirceSupport

import scala.util.{Failure, Success}

trait ZioApi {
  this: ErrorResponses with BaseCirceSupport =>

  val interop: Interop[Main.FinalEnvironment]
  val zioDep: ZioDep.Service

  val zioRoute: Route =
    path("zio") {
      get {
        parameter('input.as[Int]) { input =>
          val zioAnswer = zioDep.provideAnswer(input)
          val futureAnswer = interop.zioToFuture(zioAnswer)

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
