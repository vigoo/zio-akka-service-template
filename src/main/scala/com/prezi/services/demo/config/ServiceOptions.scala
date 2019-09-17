package com.prezi.services.demo.config

import com.typesafe.config.{Config, ConfigFactory}
import zio.system
import zio.system.System
import zio.ZIO
import zio.delegate.{Mix, delegate}

/** Different service environments (determining its configuration) */
sealed trait ServiceEnvironment

case object Local extends ServiceEnvironment

case object Prod extends ServiceEnvironment

object ServiceEnvironment {
  def fromString(value: String): Option[ServiceEnvironment] =
    value match {
      case "local" => Some(Local)
      case "prod" => Some(Prod)
      case _ => None
    }
}

/** Generic options mixin */
trait Options[S <: Options.Service] {
  val options: S
}

object Options {
  /** Base requirements for service options */
  trait Service {
    val config: Config
    val environment: ServiceEnvironment
  }
}

/** Service specific service mixin. We have to use this currently as zio-delegate cannot deal with parametric types */
trait ServiceSpecificOptions extends Options[ServiceOptions] {
  val options: ServiceOptions
}

/** Service specific options */
trait ServiceOptions extends Options.Service {
  val port: Int
}

/** Implementation of service specific options read from a Lightbend config node */
class ConfiguredServiceOptions(override val config: Config,
                               override val environment: ServiceEnvironment) extends ServiceOptions {
  override val port: Int = config.getInt("service.port")
}

object ServiceOptions {

  case class NoValidEnvironmentSpecified(specified: Option[String]) extends Exception(s"No valid environment was specified ($specified)")

  /** Loads service specific options by determining the environment from a system property */
  def environmentDependentOptions: ZIO[System, Throwable, ServiceOptions] =
    for {
      optEnvName <- system.property("environment")
      optEnv = optEnvName.flatMap(ServiceEnvironment.fromString)
      env <- optEnv match {
        case Some(value) => ZIO.succeed(value)
        case None =>
          ZIO.fail(NoValidEnvironmentSpecified(optEnvName))
      }
      configName = configNameOf(env)
      baseConfig <- ZIO.effect(ConfigFactory.load())
      envSpecificConfig <- ZIO.effect(ConfigFactory.load(configName))
      finalConfig = baseConfig.withFallback(envSpecificConfig)
    } yield new ConfiguredServiceOptions(finalConfig, env)

  /** Loads service specific options defined for test running */
  def defaultTestOptions: ZIO[Any, Throwable, ServiceOptions] =
    for {
      baseConfig <- ZIO.effect(ConfigFactory.load())
      envSpecificConfig <- ZIO.effect(ConfigFactory.load("test"))
      finalConfig = baseConfig.withFallback(envSpecificConfig)
    } yield new ConfiguredServiceOptions(finalConfig, Local)

  /** Mixin function */
  def withServiceOptions[A](a: A, opt: ServiceOptions)(implicit ev: A Mix ServiceSpecificOptions): A with ServiceSpecificOptions = {
    class Instance(@delegate underlying: Any) extends ServiceSpecificOptions {
      override val options: ServiceOptions = opt
    }
    ev.mix(a, new Instance(a))
  }

  /** Helper to access the options from the environment */
  def options: ZIO[ServiceSpecificOptions, Nothing, ServiceOptions] = ZIO.environment[ServiceSpecificOptions].map(_.options)

  private def configNameOf(environment: ServiceEnvironment): String =
    environment match {
      case Local => "local.conf"
      case Prod => "prod.conf"
    }
}