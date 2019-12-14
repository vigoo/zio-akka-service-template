package com.prezi.services.demo.dependencies

import com.prezi.services.demo.model.Answer
import com.prezi.services.demo.{OptionsSupport, TestContextSupport, ZioSupport}
import org.specs2.mutable.SpecificationWithJUnit
import zio.ZIO

class ZioDepSpecs
  extends SpecificationWithJUnit
    with ZioSupport
    with OptionsSupport
    with TestContextSupport
    with DepSpecsHelper {

  "ZioDep" should {
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

  private def withDep[T](f: ZioDep.Service[Any] => ZIO[BaseEnvironment with PureDep with ZioDep, Throwable, T]): ZIO[BaseEnvironment, Throwable, T] =
    withPureDepBasedDep[T, ZioDep, ZioDep.Service[Any]](
      ZioDep.Live.create,
      _.zioDep
    )(defaultOptions)(f)
}

