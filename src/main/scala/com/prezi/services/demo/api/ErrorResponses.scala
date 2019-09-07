package com.prezi.services.demo.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

trait ErrorResponses {
  def respondWithError(reason: Throwable): Route =
    complete(StatusCodes.InternalServerError, reason.getMessage)
}
