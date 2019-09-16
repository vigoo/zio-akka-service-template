package com.prezi.services.demo

import akka.actor.{ActorSystem, typed}
import akka.actor.typed.scaladsl.adapter._
import akka.stream.{ActorMaterializer, Materializer}
import com.prezi.services.demo.config.{ServiceOptions, ServiceSpecificOptions}
import com.prezi.services.demo.core.{AkkaContext, Context}
import zio.console.Console
import zio.delegate._
import zio.{ZIO, ZManaged, console}

trait TestContextSupport {
  this: ZioSupport =>

  private def logFatalError(reason: Throwable): ZIO[Console, Nothing, Unit] =
    console.putStrLn(s"Fatal init/shutdown error: ${reason.getMessage}")

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

  def withContext[T](f: ZIO[BaseEnvironment with ServiceSpecificOptions with AkkaContext, Throwable, T]): ZIO[BaseEnvironment with ServiceSpecificOptions, Throwable, T] = {
    val managedContext: ZManaged[Console with ServiceSpecificOptions, Throwable, AkkaContext] = ZManaged.make[Console with ServiceSpecificOptions, Throwable, AkkaContext] {
      for {
        options <- ServiceOptions.options
        sys <- ZIO(ActorSystem("demo-service-test", options.config))
        mat <- ZIO(ActorMaterializer()(sys))
      } yield new AkkaContext {
        override val actorSystem: typed.ActorSystem[_] = sys.toTyped
        override val materializer: Materializer = mat
      }
    }(terminateActorSystem)
    managedContext.use { ctx =>
      f.provideSomeM {
        for {
          env <- ZIO.environment
        } yield Context.withAkkaContext[BaseEnvironment with ServiceSpecificOptions](env, ctx.actorSystem, ctx.materializer)
      }
    }
  }
}
