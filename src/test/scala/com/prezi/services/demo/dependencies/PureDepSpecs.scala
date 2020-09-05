package com.prezi.services.demo.dependencies

import com.prezi.services.demo.dependencies.pureDep.PureDep
import com.prezi.services.demo.model.Answer
import zio.ZIO
import zio.test._
import zio.test.Assertion._

object PureDepSpecs extends DefaultRunnableSpec {

  override def spec =
    suite("PureDep")(
      testM("provide the expected answer") {
        assertM(ZIO.access[PureDep](_.get.toAnswer(100)))(equalTo(Answer("100")))
      }
    ).provideCustomLayer(PureDep.live)
}
