package com.prezi.services.demo.core

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import cats.instances.string._
import com.prezi.services.demo.Main.logFatalError
import com.prezi.services.demo.config.ServiceOptions._
import com.prezi.services.demo.config.{ServiceOptions, ServiceSpecificOptions}
import zio.{ZIO, ZManaged}
import zio.console.Console
import zio.interop.catz.console

import scala.concurrent.ExecutionContext

/** Akka specific contextual values */
trait AkkaContext {
  val actorSystem: ActorSystem[_]
}

object AkkaContext {

  object Default {
    private val create: ZIO[ServiceSpecificOptions, Nothing, AkkaContext] =
      for {
        opts <- options
      } yield new AkkaContext {
        override val actorSystem: ActorSystem[_] = akka.actor.ActorSystem("service", opts.config).toTyped
      }

    private def terminate(context: AkkaContext): ZIO[Console, Nothing, Unit] = {
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

    val managed = ZManaged.make[Console with ServiceSpecificOptions, Throwable, AkkaContext](create)(terminate)
  }

  // Helper functions to access contextual values from the environment

  def actorSystem: ZIO[AkkaContext, Nothing, ActorSystem[_]] =
    ZIO.environment[AkkaContext].map(_.actorSystem)
  def classicActorSystem: ZIO[AkkaContext, Nothing, akka.actor.ActorSystem] =
    actorSystem.map(_.toClassic)
  def actorExecutionContext: ZIO[AkkaContext, Nothing, ExecutionContext] =
    actorSystem.map(_.executionContext)
}

