package com.prezi.services.demo.core

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.stream.Materializer
import zio.ZIO
import zio.delegate._

import scala.concurrent.ExecutionContext

trait AkkaContext {
  val actorSystem: ActorSystem[_]
  val materializer: Materializer
}

trait Context extends AkkaContext

object Context {

  def withAkkaContext[A](a: A, sys: ActorSystem[_], mat: Materializer)
                        (implicit ev: A Mix AkkaContext): A with AkkaContext = {
    class Instance(@delegate underlying: Any) extends AkkaContext {
      override val actorSystem: ActorSystem[_] = sys
      override val materializer: Materializer = mat
    }
    ev.mix(a, new Instance(a))
  }

  def actorSystem: ZIO[AkkaContext, Nothing, ActorSystem[_]] =
    ZIO.environment[AkkaContext].map(_.actorSystem)
  def untypedActorSystem: ZIO[AkkaContext, Nothing, akka.actor.ActorSystem] =
    actorSystem.map(_.toUntyped)
  def materializer: ZIO[AkkaContext, Nothing, Materializer] =
    ZIO.environment[AkkaContext].map(_.materializer)
  def actorExecutionContext: ZIO[AkkaContext, Nothing, ExecutionContext] =
    actorSystem.map(_.executionContext)
}

