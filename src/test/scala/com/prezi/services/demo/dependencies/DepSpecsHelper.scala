package com.prezi.services.demo.dependencies

import com.prezi.services.demo.{OptionsSupport, TestContextSupport, ZioSupport}
import com.prezi.services.demo.config.{ServiceOptions, ServiceSpecificOptions}
import com.prezi.services.demo.core.AkkaContext
import zio.delegate._
import zio.ZIO

trait DepSpecsHelper {
  this: ZioSupport with OptionsSupport with TestContextSupport =>

  protected def withPureDepBasedDep[T, Dep, DepS](addDep: BaseEnvironment with ServiceSpecificOptions with AkkaContext with PureDep => BaseEnvironment with ServiceSpecificOptions with AkkaContext with PureDep with Dep,
                                                  getDep: Dep => DepS)
                                                 (getOptions: ZIO[Any, Throwable, ServiceOptions])
                                                 (f: DepS => ZIO[BaseEnvironment with ServiceSpecificOptions with AkkaContext with PureDep with Dep, Throwable, T]): ZIO[BaseEnvironment, Throwable, T] =
    getOptions.flatMap { options =>
      withOptions(options) {
        withContext {
          val runF = for {
            dep <- ZIO.environment[Dep]
            result <- f(getDep(dep))
          } yield result

          runF.provideSomeM {
            for {
              env <- ZIO.environment[BaseEnvironment with ServiceSpecificOptions with AkkaContext]
              newEnv = addDep(PureDep.withPureDep[BaseEnvironment with ServiceSpecificOptions with AkkaContext](env))
            } yield newEnv
          }
        }
      }
    }
}
