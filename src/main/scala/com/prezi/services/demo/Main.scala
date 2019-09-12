package com.prezi.services.demo

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorSystem, typed}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.{ActorMaterializer, Materializer}
import cats.instances.string._
import com.prezi.services.demo.actors.TestActor
import com.prezi.services.demo.api.Api
import com.prezi.services.demo.core.Context._
import com.prezi.services.demo.core.Interop._
import com.prezi.services.demo.core.{AkkaContext, Context, Interop}
import com.prezi.services.demo.dependencies.{CatsDep, FutureDep, PureDep, ZioDep}
import com.prezi.services.demo.model.Answer
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console
import zio.console.Console
import zio.delegate._
import zio.interop.catz._
import zio.random.Random
import zio.system.System

import scala.concurrent.duration._
import scala.util.Try

object Main extends CatsApp {
  private val terminateDeadline: FiniteDuration = 10.seconds

  // Environment types

  // Environment stages to build up the final environment from the initial one
  type EnvStage1 = Environment with AkkaContext with PureDep

  private def toStage1(stage0: Environment, actorSystem: typed.ActorSystem[_], materializer: Materializer): EnvStage1 =
    PureDep.withPureDep[Environment with AkkaContext](
      withAkkaContext[Environment](
        stage0, actorSystem, materializer))

  // Final application environment
  type FinalEnvironment = EnvStage1 with CatsDep with ZioDep with FutureDep

  private def toFinal(stage1: EnvStage1): FinalEnvironment =
    CatsDep.withCatsDep[EnvStage1 with FutureDep with ZioDep](
      ZioDep.withZioDep[EnvStage1 with FutureDep](
        FutureDep.withFutureDep[EnvStage1](
          stage1)))

  // Main entry point
  override def run(args: List[String]): ZIO[Main.Environment, Nothing, Int] = {
    for {
      _ <- console.putStrLn("Starting up...")
      _ <- stage1 {
        stageFinal {
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

    managedContext.use { ctx =>
      f.provideSomeM[Environment, Throwable] {
        for {
          env <- ZIO.environment[Environment]
        } yield toStage1(env, ctx.actorSystem, ctx.materializer)
      }
    }
  }

  // ... intermediate stages if needed

  private def stageFinal[A](f: ZIO[FinalEnvironment, Throwable, A]): ZIO[EnvStage1, Throwable, A] =
    f.provideSomeM[EnvStage1, Throwable] {
      for {
        env <- ZIO.environment[EnvStage1]
      } yield toFinal(env)
    }

  private def createInterop(): ZIO[FinalEnvironment, Nothing, Interop[FinalEnvironment]] =
    ZIO.environment[FinalEnvironment].map { env =>
      Interop.create(Runtime(env, runtime.Platform))
    }

  private def createHttpApi(interopImpl: Interop[FinalEnvironment],
                            testActor: ActorRef[TestActor.Message]): ZIO[FinalEnvironment, Nothing, Api] = {
    for {
      env <- ZIO.environment
    } yield new Api {
      override implicit val interop: Interop[FinalEnvironment] = interopImpl
      override val zioDep: ZioDep.Service = env.zioDep
      override val futureDep: FutureDep.Service = env.futureDep
      override val catsDep: CatsDep.Service = env.catsDep
      override val actor: ActorRef[TestActor.Message] = testActor
      override val actorSystem: typed.ActorSystem[_] = env.actorSystem
      override implicit val materializer: Materializer = env.materializer
      override val random: Random.Service[Any] = env.random
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

  private def createActor(implicit interop: Interop[Main.FinalEnvironment]): ZIO[FinalEnvironment, Throwable, ActorRef[TestActor.Message]] =
    for {
      system <- actorSystem
      actor <- system.spawn(TestActor.create(), "test-actor")
    } yield actor

  private def logFatalError(reason: Throwable): ZIO[Console, Nothing, Unit] =
    console.putStrLn(s"Fatal init/shutdown error: ${reason.getMessage}") // TODO: use logging system instead
}
