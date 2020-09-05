//package com.prezi.services.demo.dependencies
//
//import cats.effect.IO
//import com.prezi.services.demo.model.Answer
//import com.prezi.services.demo.{CatsSupport, OptionsSupport, TestContextSupport, ZioSupport}
//import org.specs2.mutable.SpecificationWithJUnit
//import zio.ZIO
//
//class CatsDepSpecs
//  extends SpecificationWithJUnit
//    with ZioSupport
//    with CatsSupport
//    with OptionsSupport
//    with TestContextSupport
//    with DepSpecsHelper {
//
//  "CatsDep" should {
//    "provide the expected answer" in {
//      run {
//        withDep { dep =>
//          for {
//            answer <- dep.provideAnswer(100)
//          } yield answer must beEqualTo(Answer("100"))
//        }
//      }
//    }
//  }
//
//  private def withDep[T](f: CatsDep.Service[Any] => IO[T]): ZIO[BaseEnvironment, Throwable, T] =
//    withPureDepBasedDep[T, CatsDep, CatsDep.Service[Any]](
//      CatsDep.Live.create,
//      _.catsDep
//    )(defaultOptions)(dep => runIO(f(dep)))
//}
