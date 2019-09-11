package com.prezi.services.demo.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.prezi.services.demo.Main
import com.prezi.services.demo.core.Interop
import com.prezi.services.demo.core.Interop._
import com.prezi.services.demo.dependencies.{PureDep, ZioDep}
import com.prezi.services.demo.model.Answer
import zio.ZIO

import scala.concurrent.ExecutionContext
import scala.util.Try

class TestActor(env: TestActor.Environment) // Subset of the ZIO environment required by the actor
               (implicit interop: Interop[Main.FinalEnvironment]) { // needed for the ZIO pipeTo syntax
  import TestActor._

  def start(): Behavior[Message] =
    Behaviors.receive { (ctx, msg) =>
      implicit val ec: ExecutionContext = ctx.executionContext
      msg match {
        case Question(input, respondTo) =>
          // ZIO value is evaluated concurrently and the result is sent back to the actor as a message
          env.zioDep.provideAnswer(input).pipeTo(ctx.self, AnswerReady(_, respondTo))
          Behaviors.same

        case AnswerReady(result, respondTo) =>
          respondTo ! result
          Behaviors.same
      }
    }
}

object TestActor {
  type Environment = ZioDep with PureDep // A subset of the final environment required by the actor

  def create()(implicit interop: Interop[Main.FinalEnvironment]): ZIO[Environment, Nothing, Behavior[Message]] =
    ZIO.environment.map(env => new TestActor(env).start())

  sealed trait Message

  final case class Question(input: Int, respondTo: ActorRef[Try[Answer]]) extends Message
  final case class AnswerReady(answer: Try[Answer], respondTo: ActorRef[Try[Answer]]) extends Message
}