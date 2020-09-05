package com.prezi.services.demo.dependencies

import com.prezi.services.demo.core.Interop
import com.prezi.services.demo.dependencies.catsDep.CatsDep
import com.prezi.services.demo.dependencies.pureDep.PureDep
import com.prezi.services.demo.model.Answer
import zio.ZIO
import zio.test.Assertion._
import zio.test._

object CatsDepSpecs extends DefaultRunnableSpec {

  override def spec =
    suite("CatsDep")(
      testM("provide the expected answer") {
        for {
          runtime <- ZIO.runtime[Any]
          interop = Interop.create(runtime)
          catsSvc <- ZIO.service[CatsDep.Service]
          answer <- interop.ioToZio(catsSvc.provideAnswer(100))
        } yield assert(answer)(equalTo(Answer("100")))
      }
    ).provideCustomLayer(PureDep.live >>> CatsDep.live)
}
