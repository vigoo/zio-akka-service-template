package com.prezi.services.demo.dependencies

import com.prezi.services.demo.core.AkkaContext
import com.prezi.services.demo.model.Answer
import zio.ZIO
import zio.macros.annotation.accessible

import scala.concurrent.{ExecutionContext, Future}

// An example dependency with a Future interface

trait FutureDep {
  val futureDep: FutureDep.Service
}

object FutureDep {
  trait Service {
    def provideAnswer(input: Int): Future[Answer]
  }

  class Live(pureDep: PureDep.Service)(implicit executionContext: ExecutionContext) extends FutureDep {

    override val futureDep: Service = new Service {
      override def provideAnswer(input: Int): Future[Answer] = {
        Future(pureDep.toAnswer(input))
      }
    }
  }

  object Live {
    val create: ZIO[PureDep with AkkaContext, Nothing, FutureDep] =
      for {
        pureDep <- ZIO.environment[PureDep].map(_.pureDep)
        ec <- AkkaContext.actorExecutionContext
      } yield new Live(pureDep)(ec)
  }
}