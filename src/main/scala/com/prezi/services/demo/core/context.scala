package com.prezi.services.demo.core

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import cats.instances.string._
import com.prezi.services.demo.serviceconfig.{TypesafeConfig, typesafeConfig}
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
      private val create: ZIO[TypesafeConfig, Nothing, AkkaContext.Service] =
        for {
          config <- typesafeConfig
        } yield new Service {
          override val actorSystem: ActorSystem[_] = akka.actor.ActorSystem("service", config).toTyped
        }

      private def terminate(context: AkkaContext.Service): ZIO[Console, Nothing, Any] = {
        console.putStrLn("Terminating actor system") *>
          ZIO
            .fromFuture { implicit ec =>
              context.actorSystem.toClassic.terminate()
            }
            .unit
            .catchAll(logFatalError)
      }

      private def logFatalError(reason: Throwable): ZIO[Console, Nothing, Unit] =
        console.putStrLn(s"Fatal init/shutdown error: ${reason.getMessage}") // TODO: use logging system instead

      val live: ZLayer[Console with TypesafeConfig, Throwable, AkkaContext] =
        ZLayer.fromManaged(ZManaged.make(create)(terminate))

      val any: ZLayer[AkkaContext, Nothing, AkkaContext] = ZLayer.requires[AkkaContext]
    }

    // Helper functions to access contextual values from the environment

    def actorSystem: ZIO[AkkaContext, Nothing, ActorSystem[_]] =
      ZIO.access(_.get.actorSystem)

    def classicActorSystem: ZIO[AkkaContext, Nothing, akka.actor.ActorSystem] =
      actorSystem.map(_.toClassic)

    def actorExecutionContext: ZIO[AkkaContext, Nothing, ExecutionContext] =
      actorSystem.map(_.executionContext)
  }

}