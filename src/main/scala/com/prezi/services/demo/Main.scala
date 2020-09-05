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
import com.prezi.services.demo.core.context.AkkaContext._
import com.prezi.services.demo.core.Interop._
import com.prezi.services.demo.core.Interop
import com.prezi.services.demo.core.context.AkkaContext
import com.prezi.services.demo.dependencies.catsDep.CatsDep
import com.prezi.services.demo.dependencies.futureDep.FutureDep
import com.prezi.services.demo.dependencies.pureDep.PureDep
import com.prezi.services.demo.dependencies.zioDep.ZioDep
import com.prezi.services.demo.model.Answer
import zio._
import zio.console.Console
import zio.interop.catz._
import zio.console
import zio.random.Random
import zio.system.System

import scala.concurrent.duration._
import scala.util.Try

object Main extends CatsApp {
  private val terminateDeadline: FiniteDuration = 10.seconds

  // Final application environment
  type FinalEnvironment = ZEnv with ServiceSpecificOptions with AkkaContext with PureDep with CatsDep with ZioDep with FutureDep

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

  def >>+[RIn0, E0, ROut0 <: Has[_], E1 >: E0, ROut1 <: Has[_]](layer0: ZLayer[RIn0, E0, ROut0],
                                                                layer1: ZLayer[ROut0, E1, ROut1])
                                                               (implicit tagged: Tagged[ROut0]): ZLayer[RIn0, E1, ROut1 with ROut0] =
    layer0 >>> (layer1 ++ ZLayer.requires[ROut0])

  def stageFinal[A](options: ZLayer[System, Throwable, ServiceSpecificOptions])(f: ZIO[FinalEnvironment, Throwable, A]): ZIO[ZEnv, Throwable, A] = {
    val layer0 = ZEnv.live ++ PureDep.live ++ options
    val layer1 = CatsDep.live ++ ZioDep.live ++ AkkaContext.Default.live
    val layer2 = FutureDep.live

    val finalLayer = >>+(>>+(layer0, layer1), layer2)

    f.provideCustomLayer(finalLayer)
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
      override val zioDep: ZioDep.Service = env.get[ZioDep.Service]
      override val futureDep: FutureDep.Service = env.get[FutureDep.Service]
      override val catsDep: CatsDep.Service = env.get[CatsDep.Service]
      override val actor: ActorRef[TestActor.Message] = testActor
      override val actorSystem: typed.ActorSystem[_] = env.get[AkkaContext.Service].actorSystem
      override val random: Random.Service = env.get[Random.Service]
    }
  }

  private def startHttpApi(api: Api): ZIO[FinalEnvironment, Nothing, ZManaged[Console, Throwable, ServerBinding]] = {
    classicActorSystem.flatMap { implicit system =>
      options.map { opt =>
        ZManaged.make[Console, Console, Throwable, ServerBinding] {
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
