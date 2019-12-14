package com.prezi.services.demo

import com.prezi.services.demo.config.{ServiceOptions, ServiceSpecificOptions}
import zio.macros.delegate._
import zio.ZIO

trait OptionsSupport {
  this: ZioSupport =>

  def withOptions[T](options: ServiceOptions)(f: ZIO[BaseEnvironment with ServiceSpecificOptions, Throwable, T]): ZIO[BaseEnvironment, Throwable, T] = {
    f.provideSomeM(ZIO.environment[BaseEnvironment] @@ enrichWith[ServiceSpecificOptions](ServiceSpecificOptions.Static(options)))
  }

  val defaultOptions: ZIO[Any, Throwable, ServiceOptions] = ServiceOptions.defaultTestOptions
}
