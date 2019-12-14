package com.prezi.services.demo.api

import akka.http.scaladsl.testkit.Specs2RouteTest
import com.prezi.services.demo.Main.FinalEnvironment
import com.prezi.services.demo.actors.TestActor
import com.prezi.services.demo.core.AkkaContext.actorSystem
import com.prezi.services.demo.core.Interop._
import com.prezi.services.demo.{Main, OptionsSupport, TestContextSupport, ZioSupport}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import org.specs2.mutable.SpecificationWithJUnit
import zio.ZIO

abstract class ServiceSpecs
  extends SpecificationWithJUnit
    with Specs2RouteTest
    with ZioSupport
    with OptionsSupport
    with TestContextSupport
    with FailFastCirceSupport {

  def withApi[T](f: Api => ZIO[FinalEnvironment, Throwable, T]): ZIO[BaseEnvironment, Throwable, T] = {
    Main.stageFinal(defaultOptions) {
      for {
        interop <- createInteop[Main.FinalEnvironment]
        system <- actorSystem
        actor <- system.spawn(TestActor.create()(interop), "test-actor")
        api <- Main.createHttpApi(interop, actor)
        result <- f(api)
      } yield result
    }
  }
}
