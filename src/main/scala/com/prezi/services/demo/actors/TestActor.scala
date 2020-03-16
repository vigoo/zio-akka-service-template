package com.prezi.services.demo.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.prezi.services.demo.core.Interop
import com.prezi.services.demo.core.Interop._
import com.prezi.services.demo.dependencies.pureDep.PureDep
import com.prezi.services.demo.dependencies.zioDep.ZioDep
import com.prezi.services.demo.model.Answer
import zio.ZIO

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
 * Example of an actor that gets its dependencies from the ZIO environment
 *
 * @param env The actor's required environment
 * @param interop Interop support, must be passed around from bootstrap
 */
class TestActor(env: TestActor.Environment) // Subset of the ZIO environment required by the actor
               (implicit interop: Interop[TestActor.Environment]) { // needed for the ZIO pipeTo syntax
  import TestActor._

  def start(): Behavior[Message] =
    Behaviors.receive { (ctx, msg) =>
      implicit val ec: ExecutionContext = ctx.executionContext
      msg match {
        case Question(input, respondTo) =>
          // ZIO value is evaluated concurrently and the result is sent back to the actor as a message
          env.get[ZioDep.Service].provideAnswer(input).pipeTo(ctx.self, AnswerReady(_, respondTo))
          Behaviors.same

        case AnswerReady(result, respondTo) =>
          respondTo ! result
          Behaviors.same
      }
    }
}

object TestActor {
  type Environment = ZioDep with PureDep // A subset of the final environment required by the actor

  /**
   * ZIO effect that creates TestActor's initial behavior by reading its dependencies from the environment
   */
  def create[R <: Environment]()(implicit interop: Interop[R]): ZIO[Environment, Nothing, Behavior[Message]] =
    ZIO.environment.map(env => new TestActor(env).start())

  sealed trait Message

  final case class Question(input: Int, respondTo: ActorRef[Try[Answer]]) extends Message
  final case class AnswerReady(answer: Try[Answer], respondTo: ActorRef[Try[Answer]]) extends Message
}