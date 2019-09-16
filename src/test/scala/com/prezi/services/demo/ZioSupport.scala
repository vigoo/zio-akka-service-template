package com.prezi.services.demo

import com.prezi.services.demo.core.Interop
import org.specs2.mutable.Specification
import zio._
import zio.internal.PlatformLive

trait ZioSupport {
  this: Specification =>

  type BaseEnvironment = Main.Environment
  protected val baseEnvironment: BaseEnvironment = Main.Environment

  private lazy val platform = PlatformLive.Default
  protected lazy val runtime = Runtime(baseEnvironment, platform)

  def run[T](test: ZIO[BaseEnvironment, Throwable, T]): T = {
    runtime.unsafeRun(test)
  }

  protected def createInteop[R]: ZIO[R, Nothing, Interop[R]] =
    ZIO.environment[R].map { env => Interop.create(Runtime(env, platform)) }
}
