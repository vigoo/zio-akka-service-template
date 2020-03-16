package com.prezi.services.demo.core

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import cats.instances.string._
import com.prezi.services.demo.config.ServiceOptions._
import com.prezi.services.demo.config.ServiceSpecificOptions
import zio.console.Console
import zio.interop.catz.console
import zio.{Has, ZIO, ZLayer, ZManaged}

import scala.concurrent.ExecutionContext

package object context {

  type AkkaContext = Has[AkkaContext.Service]

  object AkkaContext {

    trait Service {
      val actorSystem: ActorSystem[_]
    }

    object Default {
      private val create: ZIO[ServiceSpecificOptions, Nothing, AkkaContext.Service] =
        for {
          opts <- options
        } yield new Service {
          override val actorSystem: ActorSystem[_] = akka.actor.ActorSystem("service", opts.config).toTyped
        }

      private def terminate(context: AkkaContext.Service): ZIO[Console, Nothing, Any] = {
        console.putStrLn("Terminating actor system").flatMap { _ =>
          ZIO
            .fromFuture { implicit ec =>
              context.actorSystem.toClassic.terminate()
            }
            .unit
            .catchAll(logFatalError)
        }
      }

      private def logFatalError(reason: Throwable): ZIO[Console, Nothing, Unit] =
        console.putStrLn(s"Fatal init/shutdown error: ${reason.getMessage}") // TODO: use logging system instead

      // val managed = ZManaged.make[Console with ServiceSpecificOptions, Console with ServiceSpecificOptions, Throwable, AkkaContext](create)(terminate)
      val live: ZLayer[Console with ServiceSpecificOptions, Throwable, AkkaContext] =
        ZLayer.fromManaged(
          ZManaged.make[ServiceSpecificOptions, ServiceSpecificOptions with Console, Throwable, AkkaContext.Service](create)(terminate(_))
        )

      val any: ZLayer[AkkaContext, Nothing, AkkaContext] = ZLayer.requires[AkkaContext]
    }

    // Helper functions to access contextual values from the environment

    def actorSystem: ZIO[AkkaContext, Nothing, ActorSystem[_]] =
      ZIO.environment[AkkaContext].map(_.get.actorSystem)

    def classicActorSystem: ZIO[AkkaContext, Nothing, akka.actor.ActorSystem] =
      actorSystem.map(_.toClassic)

    def actorExecutionContext: ZIO[AkkaContext, Nothing, ExecutionContext] =
      actorSystem.map(_.executionContext)
  }

}