package com.prezi.services.demo.core

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import com.prezi.services.demo.serviceconfig.{TypesafeConfig, typesafeConfig}
import zio.logging.{Logging, log}
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

      private def terminate(context: AkkaContext.Service): ZIO[Logging, Nothing, Any] = {
        log.debug("Terminating actor system") *>
          ZIO
            .fromFuture { implicit ec =>
              context.actorSystem.toClassic.terminate()
            }
            .unit
            .catchAll(logFatalError)
      }

      private def logFatalError(reason: Throwable): ZIO[Logging, Nothing, Unit] =
        log.error(s"Fatal init/shutdown error: ${reason.getMessage}")

      val live: ZLayer[Logging with TypesafeConfig, Throwable, AkkaContext] =
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