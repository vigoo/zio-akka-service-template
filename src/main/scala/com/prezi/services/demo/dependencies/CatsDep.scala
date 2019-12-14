package com.prezi.services.demo.dependencies

import cats.effect.IO
import com.prezi.services.demo.model.Answer
import zio.ZIO
import zio.macros.annotation.accessible

// An example dependency with a cats-effect IO interface

trait CatsDep {
  val catsDep: CatsDep.Service[Any]
}

object CatsDep {
  trait Service[R] {
    def provideAnswer(input: Int): IO[Answer]
  }

  class Live(pureDep: PureDep.Service[Any]) extends CatsDep {
    override val catsDep: Service[Any] =  new Service[Any] {
      override def provideAnswer(input: Int): IO[Answer] =
        IO.delay(pureDep.toAnswer(input))
    }
  }

  object Live {
    val create: ZIO[PureDep, Nothing, CatsDep] =
      ZIO.environment[PureDep].map(env => new Live(env.pureDep))
  }
}