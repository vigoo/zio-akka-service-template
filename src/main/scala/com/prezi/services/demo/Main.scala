package com.prezi.services.demo

import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorSystem, typed}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.{ActorMaterializer, Materializer}
import cats.instances.string._
import com.prezi.services.demo.api.Api
import com.prezi.services.demo.core.Context._
import com.prezi.services.demo.core.{AkkaContext, Interop}
import com.prezi.services.demo.dependencies.{CatsDep, FutureDep, PureDep, ZioDep}
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console
import zio.console.Console
import zio.interop.catz._
import zio.random.Random
import zio.system.System

import scala.concurrent.duration._

object Main extends CatsApp {
  private val terminateDeadline: FiniteDuration = 10.seconds

  // Environment types

  // Environment stages to build up the final environment from the initial one
  type EnvStage1 = Environment with AkkaContext with PureDep

  private def toStage1(stage0: Environment, akkaContext: AkkaContext, pureDepImpl: PureDep.Service): EnvStage1 =
    new Clock with Console with System with Random with Blocking with PureDep with AkkaContext {
      override val actorSystem: typed.ActorSystem[_] = akkaContext.actorSystem
      override val materializer: Materializer = akkaContext.materializer
      override val pureDep: PureDep.Service = pureDepImpl
      override val blocking: Blocking.Service[Any] = stage0.blocking
      override val random: Random.Service[Any] = stage0.random
      override val console: Console.Service[Any] = stage0.console
      override val clock: Clock.Service[Any] = stage0.clock
      override val system: System.Service[Any] = stage0.system
    }

  // Final application environment
  type FinalEnvironment = EnvStage1 with CatsDep with ZioDep with FutureDep

  private def toFinal(stage1: EnvStage1,
                      catsDepImpl: CatsDep.Service,
                      zioDepImpl: ZioDep.Service,
                      futureDepImpl: FutureDep.Service): FinalEnvironment =
    new Clock with Console with System with Random with Blocking with PureDep with AkkaContext with CatsDep with ZioDep with FutureDep {
      override val catsDep: CatsDep.Service = catsDepImpl
      override val zioDep: ZioDep.Service = zioDepImpl
      override val futureDep: FutureDep.Service = futureDepImpl
      override val pureDep: PureDep.Service = stage1.pureDep
      override val blocking: Blocking.Service[Any] = stage1.blocking
      override val random: Random.Service[Any] = stage1.random
      override val console: Console.Service[Any] = stage1.console
      override val clock: Clock.Service[Any] = stage1.clock
      override val system: System.Service[Any] = stage1.system
      override val actorSystem: typed.ActorSystem[_] = stage1.actorSystem
      override val materializer: Materializer = stage1.materializer
    }

  // Main entry point
  override def run(args: List[String]): ZIO[Main.Environment, Nothing, Int] = {
    for {
      _ <- console.putStrLn("Starting up...")
      _ <- stage1 {
        stageFinal {
          for {
            interop <- createInterop()
            api <- createHttpApi(interop)
            httpServer <- startHttpApi(api)
            _ <- httpServer.useForever
          } yield ()
        }
      }.catchAll(logFatalError)
    } yield 0
  }

  private def stage1[A](f: ZIO[EnvStage1, Throwable, A]): ZIO[Environment, Throwable, A] = {
    val managedContext = ZManaged.make[Console, Throwable, AkkaContext] {
      for {
        sys <- ZIO(ActorSystem("demo-service"))
        mat <- ZIO(ActorMaterializer()(sys))
      } yield new AkkaContext {
        override val actorSystem: typed.ActorSystem[_] = sys.toTyped
        override val materializer: Materializer = mat
      }
    }(terminateActorSystem)
    val pureDep = PureDep.Default

    managedContext.use { ctx =>
      f.provideSomeM[Environment, Throwable] {
        for {
          env <- ZIO.environment[Environment]
        } yield toStage1(env, ctx, pureDep)
      }
    }
  }

  // ... intermediate stages if needed

  private def stageFinal[A](f: ZIO[FinalEnvironment, Throwable, A]): ZIO[EnvStage1, Throwable, A] = {
    f.provideSomeM[EnvStage1, Throwable] {
      for {
        catsDep <- CatsDep.create()
        zioDep <- ZioDep.create()
        futureDep <- FutureDep.create()
        finalEnv <- ZIO.environment[EnvStage1].map { env =>
          toFinal(env, catsDep, zioDep, futureDep)
        }
      } yield finalEnv
    }
  }

  private def createInterop(): ZIO[FinalEnvironment, Nothing, Interop[FinalEnvironment]] =
    ZIO.environment[FinalEnvironment].map { env =>
      Interop.create(Runtime(env, runtime.Platform))
    }

  private def createHttpApi(interopImpl: Interop[FinalEnvironment]): ZIO[FinalEnvironment, Nothing, Api] = {
    for {
      env <- ZIO.environment
    } yield new Api {
      override val interop: Interop[FinalEnvironment] = interopImpl
      override val zioDep: ZioDep.Service = env.zioDep
      override val futureDep: FutureDep.Service = env.futureDep
      override val catsDep: CatsDep.Service = env.catsDep
    }
  }

  private def startHttpApi(api: Api): ZIO[FinalEnvironment, Nothing, ZManaged[Console, Throwable, ServerBinding]] = {
    untypedActorSystem.flatMap { implicit system =>
      materializer.map { implicit mat =>
        ZManaged.make[Console, Throwable, ServerBinding] {
          console.putStrLn("Starting HTTP server").flatMap { _ =>
            ZIO.fromFuture { implicit ec =>
              Http().bindAndHandle(api.route, "0.0.0.0", port = 8080)
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

  private def terminateActorSystem(akkaContext: AkkaContext): ZIO[Console, Nothing, Unit] = {
    console.putStrLn("Terminating actor system").flatMap { _ =>
      ZIO
        .fromFuture { implicit ec =>
          akkaContext.actorSystem.toUntyped.terminate()
        }
        .unit
        .catchAll(logFatalError)
    }
  }

  private def logFatalError(reason: Throwable): ZIO[Console, Nothing, Unit] =
    console.putStrLn(s"Fatal init/shutdown error: ${reason.getMessage}") // TODO: use logging system instead
}
