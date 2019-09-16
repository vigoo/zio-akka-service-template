package com.prezi.services.demo.actors

import com.prezi.services.demo.config.ServiceSpecificOptions
import com.prezi.services.demo.core.Context.actorSystem
import com.prezi.services.demo.core.{AkkaContext, Interop}
import com.prezi.services.demo.core.Interop._
import com.prezi.services.demo.dependencies.{DepSpecsHelper, PureDep, ZioDep}
import com.prezi.services.demo.model.Answer
import com.prezi.services.demo.{OptionsSupport, TestContextSupport, ZioSupport}
import org.specs2.mutable.SpecificationWithJUnit
import zio.delegate._
import zio.ZIO

import scala.concurrent.duration._
import scala.util.Try

class TestActorSpecs
  extends SpecificationWithJUnit
    with ZioSupport
    with OptionsSupport
    with TestContextSupport
    with DepSpecsHelper {

  type TestEnv = BaseEnvironment with PureDep with ZioDep with AkkaContext

  "TestActor" should {
    "work as expected" in {
      run {
        withDep { _ =>
          for {
            interop <- createInteop[TestEnv]
            system <- actorSystem
            actor <- system.spawn(TestActor.create()(interop), "test-actor")
            testAnswer <- actor.ask[TestEnv, Try[Answer]](TestActor.Question(100, _), 1.second)
          } yield testAnswer should beASuccessfulTry(beEqualTo(Answer("100")))
        }
      }
    }
  }

  private def withDep[T](f: ZioDep.Service => ZIO[TestEnv, Throwable, T]): ZIO[BaseEnvironment, Throwable, T] =
    withPureDepBasedDep[T, ZioDep, ZioDep.Service](
      ZioDep.withZioDep[BaseEnvironment with ServiceSpecificOptions with AkkaContext with PureDep],
      _.zioDep
    )(defaultOptions)(f)
}
