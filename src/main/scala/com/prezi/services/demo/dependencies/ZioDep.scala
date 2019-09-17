package com.prezi.services.demo.dependencies

import com.prezi.services.demo.Main
import com.prezi.services.demo.model.Answer
import zio.ZIO
import zio.delegate._

// An example dependency with a ZIO interface

trait ZioDep {
  val zioDep: ZioDep.Service
}

object ZioDep {

  trait Service {
    def provideAnswer(input: Int): ZIO[PureDep, Throwable, Answer]
  }

  trait Live extends ZioDep {
    this: PureDep =>

    override val zioDep: Service = new Service {
      override def provideAnswer(input: Int): ZIO[PureDep, Throwable, Answer] =
        ZIO.effect(pureDep.toAnswer(input))
    }
  }

  def withZioDep[A <: PureDep](a: A)(implicit ev: A Mix ZioDep): A with ZioDep = {
    class Instance(@delegate underlying: PureDep) extends Live
    ev.mix(a, new Instance(a))
  }
}