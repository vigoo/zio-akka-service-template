package com.prezi.services.demo.core

import cats.effect.IO
import zio.{Runtime, ZIO}

import scala.concurrent.Future

trait Interop[R] {
  def ioToFuture[A](f: IO[A]): Future[A]

  def zioToFuture[A](f: ZIO[R, Throwable, A]): Future[A]
}

object Interop {
  def create[R](runtime: Runtime[R]): Interop[R] =
    new Interop[R] {
      override def ioToFuture[A](f: IO[A]): Future[A] =
        runtime.unsafeRunToFuture[Throwable, A](
          ZIO.effectAsync { callback =>
            f.unsafeRunAsync {
              case Left(failure) => callback(ZIO.fail(failure))
              case Right(value) => callback(ZIO.succeed(value))
            }
          }
        )

      override def zioToFuture[A](f: ZIO[R, Throwable, A]): Future[A] =
        runtime.unsafeRunToFuture[Throwable, A](f)
    }
}