package com.prezi.services.demo

import com.prezi.services.demo.config.{ServiceOptions, ServiceSpecificOptions}
import zio.ZIO
import zio.delegate._

trait OptionsSupport {
  this: ZioSupport =>

  def withOptions[T](options: ServiceOptions)(f: ZIO[BaseEnvironment with ServiceSpecificOptions, Throwable, T]): ZIO[BaseEnvironment, Throwable, T] = {
    f.provideSomeM {
      for {
        env <- ZIO.environment[BaseEnvironment]
      } yield ServiceOptions.withServiceOptions[BaseEnvironment](env, options)
    }
  }

  val defaultOptions: ZIO[Any, Throwable, ServiceOptions] = ServiceOptions.defaultTestOptions
}
