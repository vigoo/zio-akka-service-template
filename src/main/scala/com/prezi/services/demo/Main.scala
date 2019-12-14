package com.prezi.services.demo

import akka.actor.typed
import akka.actor.typed.ActorRef
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import cats.instances.string._
import com.prezi.services.demo.actors.TestActor
import com.prezi.services.demo.api.Api
import com.prezi.services.demo.config.ServiceOptions.options
import com.prezi.services.demo.config.{ServiceOptions, ServiceSpecificOptions}
import com.prezi.services.demo.core.AkkaContext._
import com.prezi.services.demo.core.Interop._
import com.prezi.services.demo.core.{AkkaContext, Interop}
import com.prezi.services.demo.dependencies.{CatsDep, FutureDep, PureDep, ZioDep}
import com.prezi.services.demo.model.Answer
import zio._
import zio.console.Console
import zio.interop.catz._
import zio.console
import zio.macros.delegate._
import zio.random.Random
import zio.system.System

import scala.concurrent.duration._
import scala.util.Try

object Main extends CatsApp {
  private val terminateDeadline: FiniteDuration = 10.seconds

  // Final application environment
  type FinalEnvironment = ZEnv with ServiceSpecificOptions with AkkaContext with PureDep with CatsDep with ZioDep with FutureDep

  private def enrichWithServiceSpecificOptions(options: ServiceOptions): EnrichWith[ServiceSpecificOptions] = enrichWith[ServiceSpecificOptions](ServiceSpecificOptions.Static(options))
  private val enrichWithAkkaContext = enrichWithManaged[AkkaContext](AkkaContext.Default.managed)
  private val enrichWithPureDep = enrichWith[PureDep](PureDep.Live)
  private val enrichWithCatsDep = enrichWithM[CatsDep](CatsDep.Live.create)
  private val enrichWithZioDep = enrichWithM[ZioDep](ZioDep.Live.create)
  private val enrichWithFutureDep = enrichWithM[FutureDep](FutureDep.Live.create)

  // Main entry point
  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    for {
      _ <- console.putStrLn("Starting up...")
      _ <- stageFinal(ServiceOptions.environmentDependentOptions) {
          for {
            interop <- createInterop()
            actor <- createActor(interop)

            // Demonstrating ask-ing an actor from ZIO
            testAnswer <- actor.ask[FinalEnvironment, Try[Answer]](TestActor.Question(100, _), 1.second)
            _ <- console.putStrLn(s"Actor answered with: $testAnswer")

            // Launching the akka-http server
            api <- createHttpApi(interop, actor)
            httpServer <- startHttpApi(api)
            _ <- httpServer.useForever
          } yield ()
        }.catchAll(logFatalError)
    } yield 0
  }

  def stageFinal[A](getOptions: ZIO[System, Throwable, ServiceOptions])(f: ZIO[FinalEnvironment, Throwable, A]): ZIO[ZEnv, Throwable, A] = {
    for {
      options <- getOptions
      finalEnv = ZIO.succeed(new DefaultRuntime {}.environment) @@
        enrichWithServiceSpecificOptions(options) @@
        enrichWithAkkaContext @@
        enrichWithPureDep @@
        enrichWithCatsDep @@
        enrichWithZioDep @@
        enrichWithFutureDep
      result <- f.provideSomeManaged(finalEnv)
    } yield result
  }

  private def createInterop(): ZIO[FinalEnvironment, Nothing, Interop[FinalEnvironment]] =
    ZIO.environment[FinalEnvironment].map { env =>
      Interop.create(Runtime(env, runtime.platform))
    }

  def createHttpApi(interopImpl: Interop[FinalEnvironment],
                    testActor: ActorRef[TestActor.Message]): ZIO[FinalEnvironment, Nothing, Api] = {
    for {
      env <- ZIO.environment
    } yield new Api {
      override implicit val interop: Interop[FinalEnvironment] = interopImpl
      override val zioDep: ZioDep.Service[Any] = env.zioDep
      override val futureDep: FutureDep.Service[Any] = env.futureDep
      override val catsDep: CatsDep.Service[Any] = env.catsDep
      override val actor: ActorRef[TestActor.Message] = testActor
      override val actorSystem: typed.ActorSystem[_] = env.actorSystem
      override val random: Random.Service[Any] = env.random
    }
  }

  private def startHttpApi(api: Api): ZIO[FinalEnvironment, Nothing, ZManaged[Console, Throwable, ServerBinding]] = {
    classicActorSystem.flatMap { implicit system =>
        options.map { opt =>
          ZManaged.make[Console, Throwable, ServerBinding] {
            console.putStrLn("Starting HTTP server").flatMap { _ =>
              ZIO.fromFuture { implicit ec =>
                Http().bindAndHandle(api.route, "0.0.0.0", port = opt.port)
              }
            }
          }(terminateHttpServer)
        }
    }
  }

  private def terminateHttpServer(binding: ServerBinding): ZIO[Console, Nothing, Unit] = {
    console.putStrLn("Terminating http server").flatMap { _ =>
      ZIO
        .fromFuture { implicit ec =>
          binding.terminate(hardDeadline = terminateDeadline)
        }
        .unit
        .catchAll(logFatalError)
    }
  }

  private def createActor(implicit interop: Interop[Main.FinalEnvironment]): ZIO[FinalEnvironment, Throwable, ActorRef[TestActor.Message]] =
    for {
      system <- actorSystem
      actor <- system.spawn(TestActor.create(), "test-actor")
    } yield actor

  private def logFatalError(reason: Throwable): ZIO[Console, Nothing, Unit] =
    console.putStrLn(s"Fatal init/shutdown error: ${reason.getMessage}") // TODO: use logging system instead
}
