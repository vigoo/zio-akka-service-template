package com.prezi.services.demo.dependencies

import com.prezi.services.demo.dependencies.pureDep.PureDep
import com.prezi.services.demo.dependencies.zioDep.ZioDep
import com.prezi.services.demo.model.Answer
import com.prezi.services.demo.serviceconfig
import zio.ZIO
import zio.test._
import zio.test.Assertion._

object ZioDepSpecs extends DefaultRunnableSpec {
  override def spec =
    suite("ZioDep")(
      testM("provide the expected answer") {
        for {
          answer <- ZIO.accessM[ZioDep](_.get.provideAnswer(100))
        } yield assert(answer)(equalTo(Answer("100")))
      }
    ).provideCustomLayer(serviceconfig.test.mapError(TestFailure.fail) >+> PureDep.live >>> ZioDep.live)
}

