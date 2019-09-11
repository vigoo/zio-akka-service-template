package com.prezi.services.demo.dependencies

import com.prezi.services.demo.Main
import com.prezi.services.demo.core.AkkaContext
import com.prezi.services.demo.model.Answer
import zio.ZIO
import zio.delegate._

import scala.concurrent.{ExecutionContext, Future}

trait FutureDep {
  val futureDep: FutureDep.Service
}

object FutureDep {
  trait Service {
    def provideAnswer(input: Int): Future[Answer]
  }

  trait Live extends FutureDep {
    this: PureDep with AkkaContext =>

    override val futureDep: Service = new Service {
      override def provideAnswer(input: Int): Future[Answer] = {
        implicit val ec: ExecutionContext = actorSystem.executionContext
        Future(pureDep.toAnswer(input))
      }
    }
  }

  def withFutureDep[A <: PureDep with AkkaContext](a: A)(implicit ev: A Mix FutureDep): A with FutureDep = {
    class Instance(@delegate underlying: PureDep with AkkaContext) extends Live
    ev.mix(a, new Instance(a))
  }
}