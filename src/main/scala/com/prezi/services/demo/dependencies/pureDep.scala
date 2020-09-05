package com.prezi.services.demo.dependencies

import com.prezi.services.demo.model.Answer
import zio.{Has, Layer, ZLayer}

// An example dependency with a pure interface, also used to show an example of one dependency depending on another
// (as CatsDep, FutureDep and ZioDep are all depending on this one)

package object pureDep {

  type PureDep = Has[PureDep.Service]

  object PureDep {

    trait Service {
      def toAnswer(input: Int): Answer
    }

    object Live extends Service {
      override def toAnswer(input: Int): Answer = Answer(input.toString)
    }

    val live: Layer[Nothing, PureDep] = ZLayer.succeed(Live)
    val any: ZLayer[PureDep, Nothing, PureDep] = ZLayer.requires[PureDep]
  }
}