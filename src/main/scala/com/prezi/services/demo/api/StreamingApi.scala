package com.prezi.services.demo.api

import akka.actor.typed
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.Materializer
import akka.stream.scaladsl._
import com.prezi.services.demo.Main
import com.prezi.services.demo.api.directives.ZioDirectives
import com.prezi.services.demo.core.Interop
import com.prezi.services.demo.core.Interop._
import zio._
import zio.interop.reactivestreams._
import zio.random.Random
import zio.stream._

import scala.util.{Failure, Success}

/**
 * Example of interopability between Akka Streams and ZIO streams in request handlers
 */
trait StreamingApi extends ZioDirectives[Main.FinalEnvironment] {
  this: ErrorResponses =>

  // dependencies
  val random: Random.Service
  implicit val interop: Interop[Main.FinalEnvironment]
  val actorSystem: typed.ActorSystem[_]

  val streamingRoute: Route =
    path("streaming") {
      get {
        // Example for taking a ZIO stream and using it as a Akka-HTTP response stream

        val randomChunk: UIO[Chunk[Byte]] = random.nextBytes(16)
        val responseStream: ZStream[Any, Nothing, Chunk[Byte]] = zio.stream.Stream.repeatEffect(randomChunk).take(8)

        val createResponse: UIO[HttpResponse] = for {
          publisher <- responseStream.toPublisher
          akkaResponseStream = Source.fromPublisher(publisher).map(_.toByteString)
          response = HttpResponse(
            entity = HttpEntity(ContentTypes.`application/octet-stream`, akkaResponseStream)
          )
        } yield response

        onRIOComplete(createResponse) {
          case Failure(reason) =>
            respondWithError(reason)
          case Success(answer) =>
            complete(answer)
        }
      } ~
      post {
        // Example of taking an Akka-HTTP request in a streaming manner and processing it as a ZIO stream
        extractDataBytes { bodyStream =>

          // Defining the ZIO sink that produces the result for the response (counts its length)
          val zioSink = zio.stream.Sink.foldLeft[Chunk[Byte], Int](0)((sum, chunk) => sum + chunk.length)

          implicit val sys: ActorSystem[_] = actorSystem

          // Defining the ZIO effect to create the Akka-HTTP response by running the Akka-HTTP request stream into
          // the ZIO sink.
          val createResponse: IO[Throwable, HttpResponse] = zioSink.toSubscriber().flatMap { case (subscriber, result) =>
            val akkaSink = akka.stream.scaladsl.Sink.fromSubscriber(subscriber)
            bodyStream
              .map(_.toChunk)
              .runWith(akkaSink)

            result.map { finalCount =>
              HttpResponse(entity = HttpEntity(finalCount.toString))
            }
          }

          onRIOComplete(createResponse) {
            case Failure(reason) =>
              respondWithError(reason)
            case Success(answer) =>
              complete(answer)
          }
        }
      }
    }
}
