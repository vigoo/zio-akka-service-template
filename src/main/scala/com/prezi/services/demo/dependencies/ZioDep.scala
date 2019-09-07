package com.prezi.services.demo.dependencies

import com.prezi.services.demo.Main
import com.prezi.services.demo.model.Answer
import zio.ZIO

trait ZioDep {
  val zioDep: ZioDep.Service
}

object ZioDep {

  trait Service {
    def provideAnswer(input: Int): ZIO[PureDep, Throwable, Answer]
  }

  def create(): ZIO[Main.EnvStage1, Throwable, Service] = {
    ZIO.effectTotal {
      new Service {
        override def provideAnswer(input: Int): ZIO[PureDep, Throwable, Answer] =
          ZIO.environment[PureDep].flatMap { dep =>
            ZIO.effect(dep.pureDep.toAnswer(input))
          }
      }
    }
  }
}