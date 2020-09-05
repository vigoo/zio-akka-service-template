package com.prezi.services.demo.api

import akka.http.scaladsl.model.StatusCodes
import com.prezi.services.demo.Main.ServiceLayers
import com.prezi.services.demo.actors.TestActor
import com.prezi.services.demo.core.Interop
import com.prezi.services.demo.core.Interop._
import com.prezi.services.demo.core.context.AkkaContext.actorSystem
import com.prezi.services.demo.model.Answer
import com.prezi.services.demo.{Main, TestLogging, serviceconfig}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import zio._
import zio.console.Console
import zio.test.Assertion._
import zio.test._

object ZioApiSpecs extends DefaultRunnableSpec with ServiceSpecs[Console, Main.ServiceLayers, Api] with TestLogging {
  override def implicits = ServiceSpecs.implicits

  override def createApi =
    (for {
      runtime <- ZIO.runtime[Main.FinalEnvironment]
      interop = Interop.create(runtime)
      system <- actorSystem
      actor <- system.spawn(TestActor.create()(interop), "test-actor")
      api <- Main.createHttpApi(interop, actor)
    } yield api).mapError(TestFailure.die)

  override def routeTests = (api: Api) => new RouteTests with FailFastCirceSupport {
    val spec: Vector[ZSpec[Main.FinalEnvironment, Throwable]] = Vector(
      testM("return the correct answer") {
        ZIO.effect {
          Get("/zio?input=111") ~> api.route ~> check {
            assert(status)(equalTo(StatusCodes.OK)) &&
              assert(responseAs[Answer])(equalTo(Answer("111")))
          }
        }
      }
    )
  }

  override def testEnv: ZLayer[Console, TestFailure[Throwable], ServiceLayers] =
    (Console.any ++ logging) >>> Main.liveServiceEnvironment(serviceconfig.test).mapError(TestFailure.fail)
}
