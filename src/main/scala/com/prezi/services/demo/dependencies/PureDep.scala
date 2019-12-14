package com.prezi.services.demo.dependencies

import com.prezi.services.demo.model.Answer
import zio.ZIO
import zio.macros.accessible._
import zio.macros.annotation.accessible

// An example dependency with a pure interface, also used to show an example of one dependency depending on another
// (as CatsDep, FutureDep and ZioDep are all depending on this one)

trait PureDep {
  val pureDep: PureDep.Service[Any]
}

object PureDep {

  trait Service[R] {
    def toAnswer(input: Int): Answer
  }

  trait Live extends PureDep {
    override val pureDep: Service[Any] = new Service[Any] {
      override def toAnswer(input: Int): Answer = Answer(input.toString)
    }
  }

  object Live extends Live
}