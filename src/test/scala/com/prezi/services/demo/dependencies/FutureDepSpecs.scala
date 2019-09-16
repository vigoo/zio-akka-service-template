package com.prezi.services.demo.dependencies

import com.prezi.services.demo.config.ServiceSpecificOptions
import com.prezi.services.demo.core.AkkaContext
import com.prezi.services.demo.model.Answer
import com.prezi.services.demo.{OptionsSupport, TestContextSupport, ZioSupport}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.SpecificationWithJUnit
import zio.delegate._
import zio.ZIO

class FutureDepSpecs(implicit ee: ExecutionEnv)
  extends SpecificationWithJUnit
    with ZioSupport
    with OptionsSupport
    with TestContextSupport
    with DepSpecsHelper {

  "FutureDep" should {
    "provide the expected answer" in {
      run {
        withDep { dep =>
          ZIO {
            dep.provideAnswer(100) must beEqualTo(Answer("100")).await
          }
        }
      }
    }
  }

  private def withDep[T](f: FutureDep.Service => ZIO[BaseEnvironment with FutureDep, Throwable, T]): ZIO[BaseEnvironment, Throwable, T] =
    withPureDepBasedDep[T, FutureDep, FutureDep.Service](
      FutureDep.withFutureDep[BaseEnvironment with ServiceSpecificOptions with AkkaContext with PureDep],
      _.futureDep)(defaultOptions)(f)
}
