package com.prezi.services.demo.api

import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.testkit.{RouteTest, TestFrameworkInterface}
import izumi.reflect.Tag
import zio.test.environment.TestEnvironment
import zio.test.{RunnableSpec, Spec, TestFailure, TestSuccess, ZSpec}
import zio.{Has, ZIO, ZLayer, ZManaged}

trait ServiceSpecs[RIn >: TestEnvironment, SR <: Has[_], Api]
  extends RunnableSpec[TestEnvironment, Any] {

  import ServiceSpecs._

  def createApi: ZIO[TestEnvironment with SR, TestFailure[Throwable], Api]
  def routeTests: Api => RouteTests
  def testEnv: ZLayer[RIn, TestFailure[Throwable], SR]
  implicit def implicits: ServiceSpecsImplicits[RIn, SR]

  trait RouteTests extends RouteTest with TestFrameworkInterface {
    override def failTest(msg: String): Nothing =
      throw AkkaHttpTestFailure(msg)

    override def testExceptionHandler: ExceptionHandler = ExceptionHandler {
      case e: AkkaHttpTestFailure => throw e
    }

    def spec: Vector[ZSpec[TestEnvironment with SR, Throwable]]
  }

  override val spec =
    Spec.suite[TestEnvironment with SR, TestFailure[Throwable], TestSuccess](
      "ZIO API",
      ZManaged.fromEffect {
        for {
          api <- createApi
          tests = routeTests(api)
        } yield tests.spec
      },
      None).provideCustomLayer(testEnv)
}

object ServiceSpecs {
  case class AkkaHttpTestFailure(message: String) extends RuntimeException

  case class ServiceSpecsImplicits[RIn, SR](
    tag: Tag[SR],
    ev: TestEnvironment with SR <:< RIn with SR
  )

  implicit def implicitTag[RIn, SR](implicit i: ServiceSpecsImplicits[RIn, SR]): Tag[SR] = i.tag
  implicit def implicitEv[RIn, SR](implicit i: ServiceSpecsImplicits[RIn, SR]): TestEnvironment with SR <:< RIn with SR = i.ev

  def implicits[RIn, SR](implicit tag: Tag[SR], ev: TestEnvironment with SR <:< RIn with SR): ServiceSpecsImplicits[RIn, SR] =
    ServiceSpecsImplicits[RIn, SR](tag, ev)
}
