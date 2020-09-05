package com.prezi.services.demo.core

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.{ByteString, Timeout}
import cats.effect.IO
import com.prezi.services.demo.core.context.AkkaContext
import zio.{Chunk, Runtime, ZIO, clock}
import zio.clock.Clock

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

/** Interface for non-ZIO code segments to run ZIO or cats-effect values */
trait Interop[+R] {
  def ioToZio[A](f: IO[A]): ZIO[Any, Throwable, A]

  def ioToFuture[A](f: IO[A]): Future[A]

  def zioToFuture[A](f: ZIO[R, Throwable, A]): Future[A]

  def zioToFutureEither[E, A](f: ZIO[R, E, A]): Future[Either[E, A]]
}

object Interop {
  // Creates an implementation based on a ZIO runtime
  def create[R](runtime: Runtime[R]): Interop[R] =
    new Interop[R] {
      override def ioToFuture[A](f: IO[A]): Future[A] =
        zioToFuture(ioToZio(f))

      override def ioToZio[A](f: IO[A]): ZIO[Any, Throwable, A] =
        ZIO.effectAsync { callback =>
          f.unsafeRunAsync {
            case Left(failure) => callback(ZIO.fail(failure))
            case Right(value) => callback(ZIO.succeed(value))
          }
        }

      override def zioToFuture[A](f: ZIO[R, Throwable, A]): Future[A] =
        runtime.unsafeRunToFuture[Throwable, A](f)

      override def zioToFutureEither[E, A](f: ZIO[R, E, A]): Future[Either[E, A]] = {
        val mappedF: ZIO[R, Throwable, Either[E, A]] =
          f.foldM(
            err => ZIO[Either[E, A]](Left.apply[E, A](err)),
            res => ZIO[Either[E, A]](Right.apply[E, A](res)))
        runtime.unsafeRunToFuture[Throwable, Either[E, A]](mappedF)
      }
    }

  // Extension methods supporting the cooperation of Akka and ZIO code

  implicit class ZioOps[R, A](f: ZIO[R, Throwable, A]) {
    def pipeTo[M, RI <: R](actor: ActorRef[M], createMessage: Try[A] => M)
                          (implicit interop: Interop[RI],
                           executionContext: ExecutionContext): Unit = {
      interop.zioToFuture(f).onComplete { result =>
        actor ! createMessage(result)
      }
    }
  }

  implicit class ActorSystemOps[T](system: ActorSystem[T]) {
    def spawn[R <: Clock, M](createInitialBehavior: ZIO[R, Nothing, Behavior[M]], namePrefix: String): ZIO[R, Throwable, ActorRef[M]] = {
      for {
        initialBehavior <- createInitialBehavior
        time <- clock.nanoTime // TODO: UUID would be better
        uniqueName = s"${namePrefix}_$time"
        actor <- ZIO.effect(system.toClassic.spawn(initialBehavior, uniqueName))
      } yield actor
    }
  }

  implicit class ActorRefOps[T](actor: ActorRef[T]) {
    def ask[R <: AkkaContext, A](createMessage: ActorRef[A] => T, timeout: Timeout): ZIO[R, Throwable, A] = {
      AkkaContext.actorSystem.flatMap { system =>
        implicit val t: Timeout = timeout
        implicit val scheduler: Scheduler = system.scheduler

        val futureAnswer = actor ? createMessage
        ZIO.fromFuture { implicit ec => futureAnswer }
      }
    }
  }

  implicit class ByteStringOps(bs: ByteString) {
    def toChunk: Chunk[Byte] = Chunk.fromArray(bs.toArray)
  }

  implicit class ByteChunkOps(chunk: Chunk[Byte]) {
    def toByteString: ByteString = ByteString(chunk.toArray)
  }

}