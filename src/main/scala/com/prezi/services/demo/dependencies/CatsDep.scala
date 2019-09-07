package com.prezi.services.demo.dependencies

import cats.effect.IO
import com.prezi.services.demo.Main
import com.prezi.services.demo.model.Answer
import zio.ZIO

trait CatsDep {
  val catsDep: CatsDep.Service
}

object CatsDep {
  trait Service {
    def provideAnswer(input: Int): IO[Answer]
  }

  def create(): ZIO[Main.EnvStage1, Nothing, Service] = {
    for {
      pureDep <- ZIO.environment[Main.EnvStage1].map(_.pureDep)
    } yield new Service {
      override def provideAnswer(input: Int): IO[Answer] =
        IO.delay(pureDep.toAnswer(input))
    }
  }
}