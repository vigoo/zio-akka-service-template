package com.prezi.services.demo.dependencies

import com.prezi.services.demo.model.Answer
import zio.delegate._

trait PureDep {
  val pureDep: PureDep.Service
}

object PureDep {

  trait Service {
    def toAnswer(input: Int): Answer
  }

  trait Live extends PureDep {
    override val pureDep: Service = new Service {
      override def toAnswer(input: Int): Answer = Answer(input.toString)
    }
  }

  def withPureDep[A](a: A)(implicit ev: A Mix PureDep): A with PureDep = {
    class Instance(@delegate underlying: Any) extends Live
    ev.mix(a, new Instance(a))
  }
}