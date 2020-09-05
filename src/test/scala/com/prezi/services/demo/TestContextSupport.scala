//package com.prezi.services.demo
//
//import akka.actor.typed.scaladsl.adapter._
//import com.prezi.services.demo.config.ServiceSpecificOptions
//import com.prezi.services.demo.core.context.AkkaContext
//import zio.console.Console
//import zio.macros.delegate._
//import zio.{ZIO, console}
//
//trait TestContextSupport {
//  this: ZioSupport =>
//
//  private def logFatalError(reason: Throwable): ZIO[Console, Nothing, Unit] =
//    console.putStrLn(s"Fatal init/shutdown error: ${reason.getMessage}")
//
//  private def terminateActorSystem(akkaContext: AkkaContext.Service): ZIO[Console, Nothing, Unit] = {
//    console.putStrLn("Terminating actor system").flatMap { _ =>
//      ZIO
//        .fromFuture { implicit ec =>
//          akkaContext.actorSystem.toClassic.terminate()
//        }
//        .unit
//        .catchAll(logFatalError)
//    }
//  }
//
//  def withContext[T](f: ZIO[BaseEnvironment with ServiceSpecificOptions with AkkaContext, Throwable, T]): ZIO[BaseEnvironment with ServiceSpecificOptions, Throwable, T] = {
//    f.provideSomeLayer[AkkaContext](AkkaContext.Default.live)
//  }
//}
