//package com.prezi.services.demo.dependencies
//
//import com.prezi.services.demo.{OptionsSupport, TestContextSupport, ZioSupport}
//import com.prezi.services.demo.config.{ServiceOptions, ServiceSpecificOptions}
//import com.prezi.services.demo.core.AkkaContext
//import zio.macros.delegate._
//import zio.ZIO
//
//trait DepSpecsHelper {
//  this: ZioSupport with OptionsSupport with TestContextSupport =>
//
//  protected def withPureDepBasedDep[T, Dep, DepS](create: ZIO[BaseEnvironment with ServiceSpecificOptions with AkkaContext with PureDep, Nothing, Dep],
//                                                  getDep: Dep => DepS)
//                                                 (getOptions: ZIO[Any, Throwable, ServiceOptions])
//                                                 (f: DepS => ZIO[BaseEnvironment with ServiceSpecificOptions with AkkaContext with PureDep with Dep, Throwable, T])
//                                                 (implicit ev: Mix[BaseEnvironment with ServiceSpecificOptions with AkkaContext with PureDep, Dep]): ZIO[BaseEnvironment, Throwable, T] =
//    getOptions.flatMap { options =>
//      withOptions(options) {
//        withContext {
//          val runF = for {
//            dep <- ZIO.environment[Dep]
//            result <- f(getDep(dep))
//          } yield result
//
//          runF.provideSomeM {
//            (for {
//              base <- ZIO.environment[BaseEnvironment with ServiceSpecificOptions with AkkaContext with PureDep]
//              dep <- create
//            } yield ev.mix(base, dep)).provideSomeM(ZIO.environment[BaseEnvironment with ServiceSpecificOptions with AkkaContext] @@ enrichWith[PureDep](PureDep.Live))
//          }
//        }
//      }
//    }
//}
