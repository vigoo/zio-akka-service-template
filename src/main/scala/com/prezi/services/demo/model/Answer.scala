package com.prezi.services.demo.model

import io.circe._
import io.circe.generic.semiauto._

case class Answer(value: String)

object Answer {
  implicit val answerEncoder: Encoder[Answer] = deriveEncoder[Answer]
  implicit val answerDecoder: Decoder[Answer] = deriveDecoder[Answer]
}
