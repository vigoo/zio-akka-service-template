package com.prezi.services.demo

import cats.effect.IO
import com.prezi.services.demo.core.Interop
import zio.ZIO

trait CatsSupport {
  this: ZioSupport =>

  def runIO[R, T](f: IO[T]): ZIO[BaseEnvironment, Throwable, T] = {
    Interop.create(runtime).ioToZio(f)
  }
}
