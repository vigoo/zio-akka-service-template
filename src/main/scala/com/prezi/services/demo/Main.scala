package com.prezi.services.demo

import akka.actor.typed
import akka.actor.typed.ActorRef
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import com.prezi.services.demo.actors.TestActor
import com.prezi.services.demo.api.Api
import com.prezi.services.demo.serviceconfig
import com.prezi.services.demo.core.Interop
import com.prezi.services.demo.core.Interop._
import com.prezi.services.demo.core.context.AkkaContext
import com.prezi.services.demo.core.context.AkkaContext._
import com.prezi.services.demo.dependencies.catsDep.CatsDep
import com.prezi.services.demo.dependencies.futureDep.FutureDep
import com.prezi.services.demo.dependencies.pureDep.PureDep
import com.prezi.services.demo.dependencies.zioDep.ZioDep
import com.prezi.services.demo.model.Answer
import com.prezi.services.demo.serviceconfig.{Configuration, TypesafeZConfig, serviceConfig}
import zio._
import zio.config.{ZConfig, config}
import zio.console.Console
import zio.random.Random
import zio.system.System

import scala.concurrent.duration._
import scala.util.Try

object Main extends App {
  private val terminateDeadline: FiniteDuration = 10.seconds

  type ServiceLayers = TypesafeZConfig[Configuration] with AkkaContext with PureDep with CatsDep with ZioDep with FutureDep
  type FinalEnvironment = ZEnv with ServiceLayers

  def liveServiceEnvironment[RIn <: Has[_]](options: ZLayer[RIn, Throwable, TypesafeZConfig[Configuration]]): ZLayer[RIn with Console, Throwable, ServiceLayers] = {
    (options ++ PureDep.live ++ Console.any) >+>
      AkkaContext.Default.live >+>
      (FutureDep.live ++ CatsDep.live ++ ZioDep.live)
  }

  // Main entry point
  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    for {
      _ <- console.putStrLn("Starting up...")
      _ <- inServiceEnvironment(serviceconfig.live) {
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
    } yield ExitCode.success
  }

  def inServiceEnvironment[A](options: ZLayer[System, Throwable, TypesafeZConfig[Configuration]])(f: ZIO[FinalEnvironment, Throwable, A]): ZIO[ZEnv, Throwable, A] = {
    f.provideCustomLayer(liveServiceEnvironment(options))
  }

  private def createInterop(): ZIO[FinalEnvironment, Nothing, Interop[FinalEnvironment]] =
    ZIO.runtime.map(Interop.create)

  def createHttpApi(interopImpl: Interop[FinalEnvironment],
                    testActor: ActorRef[TestActor.Message]): ZIO[FinalEnvironment, Nothing, Api] = {
    for {
      env <- ZIO.environment
    } yield new Api {
      override implicit val interop: Interop[FinalEnvironment] = interopImpl
      override val zioDep: ZioDep.Service = env.get[ZioDep.Service]
      override val futureDep: FutureDep.Service = env.get[FutureDep.Service]
      override val catsDep: CatsDep.Service = env.get[CatsDep.Service]
      override val actor: ActorRef[TestActor.Message] = testActor
      override val actorSystem: typed.ActorSystem[_] = env.get[AkkaContext.Service].actorSystem
      override val random: Random.Service = env.get[Random.Service]
    }
  }

  private def startHttpApi(api: Api): ZIO[FinalEnvironment, Nothing, ZManaged[Console, Throwable, ServerBinding]] = {
    for {
      system <- classicActorSystem
      opt <- config[Configuration]
      create = {
        implicit val sys = system
        for {
          _ <- console.putStrLn("Starting HTTP server")
          result <- ZIO.fromFuture { implicit ec =>
            Http()
              .newServerAt("0.0.0.0", port = opt.http.port)
              .bindFlow(api.route)
          }
        } yield result
      }
    } yield ZManaged.make(create)(terminateHttpServer)
  }

  private def terminateHttpServer(binding: ServerBinding): ZIO[Console, Nothing, Unit] =
    for {
      _ <- console.putStrLn("Terminating http server")
      _ <-
        ZIO.fromFuture { implicit ec =>
          binding.terminate(hardDeadline = terminateDeadline)
        }.unit.catchAll(logFatalError)
    } yield ()

  private def createActor(implicit interop: Interop[Main.FinalEnvironment]): ZIO[FinalEnvironment, Throwable, ActorRef[TestActor.Message]] =
    for {
      system <- actorSystem
      actor <- system.spawn(TestActor.create(), "test-actor")
    } yield actor

  private def logFatalError(reason: Throwable): ZIO[Console, Nothing, Unit] =
    console.putStrLn(s"Fatal init/shutdown error: ${reason.getMessage}") // TODO: use logging system instead
}
