package com.prezi.services.demo.dependencies

import com.prezi.services.demo.dependencies.pureDep.PureDep
import com.prezi.services.demo.model.Answer
import zio.{Has, ZIO, ZLayer}

// An example dependency with a ZIO interface

package object zioDep {

  type ZioDep = Has[ZioDep.Service]

  object ZioDep {

    trait Service {
      def provideAnswer(input: Int): ZIO[Any, Throwable, Answer]
    }

    class Live(pureDep: PureDep.Service) extends Service {
      override def provideAnswer(input: Int): ZIO[Any, Throwable, Answer] =
        ZIO.effect(pureDep.toAnswer(input))
    }

    val live: ZLayer[PureDep, Nothing, ZioDep] = ZLayer.fromService(pureDep => new Live(pureDep))
    val any: ZLayer[ZioDep, Nothing, ZioDep] = ZLayer.requires[ZioDep]
  }
}
