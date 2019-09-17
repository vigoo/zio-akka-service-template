package com.prezi.services.demo.api.directives

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.prezi.services.demo.core.Interop
import zio._

import scala.util.Try

/**
 * Provides onComplete style directives to respond with running ZIO effects
 */
trait ZioDirectives[+R] {
  implicit val interop: Interop[R]

  def onZIOComplete[E, A](f: ZIO[R, E, A]): Directive1[Try[Either[E, A]]] =
    onComplete(interop.zioToFutureEither(f))

  def onRIOComplete[A](f: RIO[R, A]): Directive1[Try[A]] =
    onComplete(interop.zioToFuture(f))
}
