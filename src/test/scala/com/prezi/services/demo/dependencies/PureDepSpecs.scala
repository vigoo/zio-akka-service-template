package com.prezi.services.demo.dependencies

import com.prezi.services.demo.ZioSupport
import com.prezi.services.demo.model.Answer
import org.specs2.mutable.SpecificationWithJUnit
import zio.delegate._

class PureDepSpecs extends SpecificationWithJUnit with ZioSupport {

  "PureDep" should {
    "provide the expected answer" in {
      val dep = create()
      dep.toAnswer(100) must beEqualTo(Answer("100"))
    }
  }

  private def create(): PureDep.Service =
    PureDep.withPureDep(baseEnvironment).pureDep
}
