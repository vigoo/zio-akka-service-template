package com.prezi.services.demo

import com.typesafe.config.{Config, ConfigFactory}
import zio.config.ConfigDescriptor._
import zio.config._
import zio.config.derivation.describe
import zio.config.magnolia.DeriveConfigDescriptor._
import zio.config.typesafe.TypesafeConfigSource
import zio.system.System
import zio.{Has, TaskLayer, URIO, ZIO, ZLayer}

package object serviceconfig {

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

    def asString(value: ServiceEnvironment): String = value match {
      case Local => "local"
      case Prod => "prod"
    }
  }

  @describe("Configuration of the service's HTTP layer")
  case class HttpConfig(@describe("Port to bind the HTTP service to") port: Int)

  @describe("Service specific configuration block")
  case class ServiceConfig(@describe("Value used by PureDep to calculate the result") multiplier: Int)

  @describe("Top level configuration class")
  case class Configuration(http: HttpConfig,
                           service: ServiceConfig)

  private val environmentConfig: ConfigDescriptor[ServiceEnvironment] =
    string("environment").transformEither(
      ServiceEnvironment.fromString(_).toRight("Invalid service environment value"),
      (env: ServiceEnvironment) => Right(ServiceEnvironment.asString(env))
    ) ?? "Configures the environment the service runs in"

  val serviceEnvironment: ZLayer[System, Throwable, ZConfig[ServiceEnvironment]] =
    ZConfig.fromSystemProperties(environmentConfig)

  val environmentDependentConfigSource: ZLayer[ZConfig[ServiceEnvironment], Throwable, Has[Config]] =
    ZLayer.fromServiceM { env =>
      ZIO.effect(ConfigFactory.load().withFallback(ConfigFactory.load(configNameOf(env))))
    }

  val defaultTestConfigSource: TaskLayer[Has[Config]] =
    ZIO.effect(ConfigFactory.load().withFallback(ConfigFactory.load("test"))).toLayer

  val serviceConfig: ZLayer[Has[Config], ReadError[String], ZConfig[Configuration]] =
    ZLayer.fromServiceM { config =>
      for {
        source <- ZIO.fromEither(TypesafeConfigSource.fromTypesafeConfig(config))
        config <- ZIO.fromEither(read[Configuration](descriptor[Configuration] from source))
      } yield config
    }

  type TypesafeConfig = Has[Config]
  type TypesafeZConfig[A] = TypesafeConfig with ZConfig[A]

  val live: ZLayer[System, Throwable, TypesafeZConfig[Configuration]] = serviceEnvironment >>> environmentDependentConfigSource >+> serviceConfig
  val test: ZLayer[Any, Throwable, TypesafeZConfig[Configuration]] = defaultTestConfigSource >+> serviceConfig

  def typesafeConfig: URIO[TypesafeConfig, Config] = ZIO.service[Config]

  private def configNameOf(environment: ServiceEnvironment): String =
    environment match {
      case Local => "local.conf"
      case Prod => "prod.conf"
    }
}