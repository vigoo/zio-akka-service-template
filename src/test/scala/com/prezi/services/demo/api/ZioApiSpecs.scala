package com.prezi.services.demo.api

import akka.http.scaladsl.model.StatusCodes
import com.prezi.services.demo.model.Answer
import zio.ZIO

class ZioApiSpecs extends ServiceSpecs {
  "Zio API" should {
    "return the correct answer" in {
      run {
        withApi { api =>
          ZIO {
            Get("/zio?input=111") ~> api.route ~> check {
              status must beEqualTo(StatusCodes.OK)
              responseAs[Answer] must beEqualTo(Answer("111"))
            }
          }
        }
      }
    }
  }
}
