package com.prezi.services.demo.dependencies

import com.prezi.services.demo.core.context.AkkaContext
import com.prezi.services.demo.dependencies.futureDep.FutureDep
import com.prezi.services.demo.dependencies.pureDep.PureDep
import com.prezi.services.demo.model.Answer
import com.prezi.services.demo.{TestLogging, serviceconfig}
import zio.console.Console
import zio.{ZIO, ZLayer}
import zio.test._
import zio.test.Assertion._

object FutureDepSpecs extends DefaultRunnableSpec with TestLogging {
  private val testServiceOptions = serviceconfig.test
  private val testAkkaContext = (logging ++ testServiceOptions) >+> AkkaContext.Default.live
  private val testEnv = testAkkaContext >+> PureDep.live >+> FutureDep.live

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
