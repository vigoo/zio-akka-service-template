package com.prezi.services.demo.actors

import com.prezi.services.demo.core.Interop
import com.prezi.services.demo.core.Interop._
import com.prezi.services.demo.core.context.AkkaContext
import com.prezi.services.demo.core.context.AkkaContext.actorSystem
import com.prezi.services.demo.dependencies.pureDep.PureDep
import com.prezi.services.demo.dependencies.zioDep.ZioDep
import com.prezi.services.demo.model.Answer
import com.prezi.services.demo.{TestLogging, serviceconfig}
import zio.logging.Logging
import zio.test.Assertion._
import zio.test._
import zio.{ZIO, ZLayer}

import scala.concurrent.duration._
import scala.util.Try

object TestActorSpecs extends DefaultRunnableSpec with TestLogging {

  type TestEnv = ZioDep with PureDep with AkkaContext

  private val testServiceOptions = serviceconfig.test
  private val testAkkaContext = (logging ++ testServiceOptions) >>> AkkaContext.Default.live
  private val testEnv = (PureDep.live ++ testAkkaContext) >+> ZioDep.live

  override val spec =
    suite("TestActor")(
      testM("work as expected") {
        ZIO.runtime[TestEnv].flatMap { runtime =>
          implicit val interop = Interop.create(runtime)
          for {
            system <- actorSystem
            actor <- system.spawn(TestActor.create(), "test-actor")
            testAnswer <- actor.ask[TestEnv, Try[Answer]](TestActor.Question(100, _), 1.second)
          } yield assert(testAnswer)(isSuccess(equalTo(Answer("100"))))
        }
      }
    ).provideCustomLayerShared(testEnv.mapError(TestFailure.fail))
}
