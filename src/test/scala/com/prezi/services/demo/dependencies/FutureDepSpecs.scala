package com.prezi.services.demo.dependencies

import com.prezi.services.demo.config.{ServiceOptions, ServiceSpecificOptions}
import com.prezi.services.demo.core.context.AkkaContext
import com.prezi.services.demo.dependencies.futureDep.FutureDep
import com.prezi.services.demo.dependencies.pureDep.PureDep
import com.prezi.services.demo.model.Answer
import zio.console.Console
import zio.{ZIO, ZLayer}
import zio.test._
import zio.test.Assertion._

object FutureDepSpecs extends DefaultRunnableSpec {
  private val testServiceOptions = ServiceOptions.defaultTestOptions
  private val testAkkaContext = (ZLayer.requires[Console] ++ testServiceOptions) >>> AkkaContext.Default.live
  private val testEnv = (PureDep.live ++ testAkkaContext) >>> FutureDep.live

  override def spec =
    suite("FutureDep")(
      testM("provide the expected answer") {
        for {
          futureAnswer <- ZIO.access[FutureDep](_.get.provideAnswer(100))
          answer <- ZIO.fromFuture(_ => futureAnswer)
        } yield assert(answer)(equalTo(Answer("100")))
      }
    ).provideCustomLayer(
      testEnv.mapError(TestFailure.fail)
    )
}
