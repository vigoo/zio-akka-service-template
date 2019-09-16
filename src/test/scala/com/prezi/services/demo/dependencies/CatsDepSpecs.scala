package com.prezi.services.demo.dependencies

import cats.effect.IO
import com.prezi.services.demo.config.ServiceSpecificOptions
import com.prezi.services.demo.{CatsSupport, OptionsSupport, TestContextSupport, ZioSupport}
import com.prezi.services.demo.core.AkkaContext
import com.prezi.services.demo.model.Answer
import org.specs2.mutable.SpecificationWithJUnit
import zio.delegate._
import zio.ZIO

class CatsDepSpecs
  extends SpecificationWithJUnit
    with ZioSupport
    with CatsSupport
    with OptionsSupport
    with TestContextSupport
    with DepSpecsHelper {

  "CatsDep" should {
    "provide the expected answer" in {
      run {
        withDep { dep =>
          for {
            answer <- dep.provideAnswer(100)
          } yield answer must beEqualTo(Answer("100"))
        }
      }
    }
  }

  private def withDep[T](f: CatsDep.Service => IO[T]): ZIO[BaseEnvironment, Throwable, T] =
    withPureDepBasedDep[T, CatsDep, CatsDep.Service](
      CatsDep.withCatsDep[BaseEnvironment with ServiceSpecificOptions with AkkaContext with PureDep],
      _.catsDep
    )(defaultOptions)(dep => runIO(f(dep)))
}
