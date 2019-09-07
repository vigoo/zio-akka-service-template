package com.prezi.services.demo.dependencies

import com.prezi.services.demo.Main
import com.prezi.services.demo.model.Answer
import zio.ZIO

import scala.concurrent.{ExecutionContext, Future}

trait FutureDep {
  val futureDep: FutureDep.Service
}

object FutureDep {
  trait Service {
    def provideAnswer(input: Int): Future[Answer]
  }

  def create(): ZIO[Main.EnvStage1, Nothing, Service] = {
    for {
      pureDep <- ZIO.environment[Main.EnvStage1].map(_.pureDep)
      executionContext <- ZIO.environment[Main.EnvStage1].map(_.actorSystem.executionContext)
    } yield new Service {
      override def provideAnswer(input: Int): Future[Answer] = {
        implicit val ec: ExecutionContext = executionContext
        Future(pureDep.toAnswer(input))
      }
    }
  }

}