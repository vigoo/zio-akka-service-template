package com.prezi.services.demo.dependencies

import com.prezi.services.demo.model.Answer

trait PureDep {
  val pureDep: PureDep.Service
}

object PureDep {

  trait Service {
    def toAnswer(input: Int): Answer
  }

  object Default extends PureDep.Service {
    override def toAnswer(input: Int): Answer = Answer(input.toString)
  }

}