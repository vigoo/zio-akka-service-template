package com.prezi.services.demo.dependencies

import com.prezi.services.demo.model.Answer

// An example dependency with a pure interface, also used to show an example of one dependency depending on another
// (as CatsDep, FutureDep and ZioDep are all depending on this one)

trait PureDep {
  val pureDep: PureDep.Service
}

object PureDep {

  trait Service {
    def toAnswer(input: Int): Answer
  }

  trait Live extends PureDep {
    override val pureDep: Service= new Service{
      override def toAnswer(input: Int): Answer = Answer(input.toString)
    }
  }

  object Live extends Live
}