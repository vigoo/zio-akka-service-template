package com.prezi.services.demo.api

import akka.actor.Scheduler
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import com.prezi.services.demo.actors.TestActor
import com.prezi.services.demo.model.Answer
import de.heikoseeberger.akkahttpcirce.BaseCirceSupport

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait ActorApi {
  this: ErrorResponses with BaseCirceSupport =>

  val actor: ActorRef[TestActor.Message]
  val actorSystem: ActorSystem[_]

  val actorRoute: Route =
    path("actor") {
      get {
        parameter('input.as[Int]) { input =>
          implicit val timeout: Timeout = 1.second
          implicit val scheduler: Scheduler = actorSystem.scheduler

          val futureAnswer: Future[Try[Answer]] = actor ? (TestActor.Question(input, _))
          onComplete(futureAnswer) {
            case Failure(reason) =>
              respondWithError(reason)
            case Success(Failure(reason)) =>
              respondWithError(reason)
            case Success(Success(answer)) =>
              complete(answer)
          }
        }
      }
    }
}
