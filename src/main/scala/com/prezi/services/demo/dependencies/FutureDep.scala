package com.prezi.services.demo.dependencies

import com.prezi.services.demo.core.context.AkkaContext
import com.prezi.services.demo.dependencies.pureDep.PureDep
import com.prezi.services.demo.model.Answer
import zio.{Has, ZLayer}

import scala.concurrent.{ExecutionContext, Future}

// An example dependency with a Future interface

package object futureDep {

  type FutureDep = Has[FutureDep.Service]

  object FutureDep {

    trait Service {
      def provideAnswer(input: Int): Future[Answer]
    }

    class Live(pureDep: PureDep.Service)(implicit executionContext: ExecutionContext) extends Service {
      override def provideAnswer(input: Int): Future[Answer] = {
        Future(pureDep.toAnswer(input))
      }
    }

    val live: ZLayer[PureDep with AkkaContext, Nothing, FutureDep] = {
      ZLayer.fromServiceM { pureDep =>
        for {
          ec <- AkkaContext.actorExecutionContext
        } yield new Live(pureDep)(ec)
      }
    }
    val any: ZLayer[FutureDep, Nothing, FutureDep] = ZLayer.requires[FutureDep]
  }
}
