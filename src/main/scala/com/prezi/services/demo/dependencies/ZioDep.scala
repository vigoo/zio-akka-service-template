package com.prezi.services.demo.dependencies

import com.prezi.services.demo.model.Answer
import zio.ZIO
import zio.macros.annotation.accessible

// An example dependency with a ZIO interface

@accessible(">")
trait ZioDep {
  val zioDep: ZioDep.Service[Any]
}

object ZioDep {

  trait Service[R] {
    def provideAnswer(input: Int): ZIO[R, Throwable, Answer]
  }

  class Live(pureDep: PureDep.Service[Any]) extends ZioDep {
    override val zioDep: Service[Any] = new Service[Any] {
      override def provideAnswer(input: Int): ZIO[Any, Throwable, Answer] =
        ZIO.effect(pureDep.toAnswer(input))
    }
  }

  object Live {
    val create: ZIO[PureDep, Nothing, ZioDep] =
      ZIO.environment[PureDep].map(env => new Live(env.pureDep))
  }
}