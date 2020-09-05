package com.prezi.services.demo.dependencies

import cats.effect.IO
import com.prezi.services.demo.dependencies.pureDep.PureDep
import com.prezi.services.demo.model.Answer
import zio.{Has, ZLayer}

// An example dependency with a cats-effect IO interface

package object catsDep {

  type CatsDep = Has[CatsDep.Service]

  object CatsDep {

    trait Service {
      def provideAnswer(input: Int): IO[Answer]
    }

    class Live(pureDep: PureDep.Service) extends Service {
      override def provideAnswer(input: Int): IO[Answer] =
        IO.delay(pureDep.toAnswer(input))
    }

    val live: ZLayer[PureDep, Nothing, CatsDep] = ZLayer.fromService(pureDep => new Live(pureDep))
    val any: ZLayer[CatsDep, Nothing, CatsDep] = ZLayer.requires[CatsDep]
  }
}