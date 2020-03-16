//package com.prezi.services.demo
//
//import com.prezi.services.demo.config.{ServiceOptions, ServiceSpecificOptions}
//import zio.macros.delegate._
//import zio.{ZIO, ZLayer}
//
//trait OptionsSupport {
//  this: ZioSupport =>
//
//  def withOptions[T](options: ServiceOptions)(f: ZIO[BaseEnvironment with ServiceSpecificOptions, Throwable, T]): ZIO[BaseEnvironment, Throwable, T] = {
//    f.provideSomeLayer[ServiceSpecificOptions](defaultOptions)
//  }
//
//  val defaultOptions: ZLayer[Any, Throwable, ServiceSpecificOptions] = ServiceOptions.defaultTestOptions
//}
