package com.prezi.services.demo.dependencies

import cats.effect.IO
import com.prezi.services.demo.Main
import com.prezi.services.demo.model.Answer
import zio.ZIO
import zio.delegate._

trait CatsDep {
  val catsDep: CatsDep.Service
}

object CatsDep {
  trait Service {
    def provideAnswer(input: Int): IO[Answer]
  }

  trait Live extends CatsDep {
    this: PureDep =>

    override val catsDep: Service =  new Service {
      override def provideAnswer(input: Int): IO[Answer] =
        IO.delay(pureDep.toAnswer(input))
    }
  }

  def withCatsDep[A <: PureDep](a: A)(implicit ev: A Mix CatsDep): A with CatsDep = {
    class Instance(@delegate underlying: PureDep) extends Live
    ev.mix(a, new Instance(a))
  }
}