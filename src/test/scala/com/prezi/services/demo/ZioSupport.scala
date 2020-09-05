package com.prezi.services.demo

import com.prezi.services.demo.core.Interop
import org.specs2.mutable.Specification
import zio._
import zio.internal.Platform

trait ZioSupport {
  this: Specification =>

  type BaseEnvironment = ZEnv
  protected val baseEnvironment: BaseEnvironment

  protected lazy val runtime = Runtime.default

  def run[T](test: ZIO[BaseEnvironment, Throwable, T]): T = {
    runtime.unsafeRun(test)
  }

  protected def createInteop[R]: ZIO[R, Nothing, Interop[R]] =
    ZIO.environment[R].map { env => Interop.create(Runtime(env, Platform.default)) }
}
