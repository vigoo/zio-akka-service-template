# zio-service-template

This is an example project for writing backend services by mixing ZIO, Cats-Effect IO and Akka libraries. 
The template of this is somewhat similar to our Prezi-specific template which currently does not take advantage 
of ZIO or Cats libraries.

This example implements the following goals:

- Use ZIO on top level to be able to bootstrap the service in a more controlled way
- Use ZIO's environment support for passing around the global dependencies in the whole service
- Be able to use any library with a cats-effect IO interface
- Use Akka-HTTP to implement the HTTP endpoint
- Ability to express concurrent logic with either akka actors or cats or zio effects
- Ability to interop between Akka Streams and ZIO streams
- Make this all testable
- Use Lightbend's config library for configuration, as it is used by all the Akka based libraries

In the README I will highlight some parts of the example but see the source code for the full example.

## Bootstrap

TODO: rewrite this part with info about layers
 
The `main` function builds up the environment, then creates the service API handler and a test actor and 
runs the service.

## Context
One of the dependencies is `AkkaContext`. This holds the actor system for Akka (note that a materializer is no longer needed as it is tied to the system since Akka 2.6).

This environment is added as a _managed resource_ that ensures that it gets properly terminated:

```scala
private val create: ZIO[ServiceSpecificOptions, Nothing, AkkaContext] =
  for {
    opts <- options
  } yield new AkkaContext {
    override val actorSystem: ActorSystem[_] = akka.actor.ActorSystem("service", opts.config).toTyped
  }

private def terminate(context: AkkaContext): ZIO[Console, Nothing, Unit] = {
  console.putStrLn("Terminating actor system").flatMap { _ =>
    ZIO
      .fromFuture { implicit ec =>
        context.actorSystem.toClassic.terminate()
      }
      .unit
      .catchAll(logFatalError)
  }
}
val managed = ZManaged.make[Console with ServiceSpecificOptions, Throwable, AkkaContext](create)(terminate)
```

## Working with actors
To work with actors I built some helper functions in a form of extension methods that makes it possible to do some
operations as ZIO effects:

- spawn top level actors
- perform an ask as an async ZIO operation
- run a ZIO effect and pipe back its result to an actor

To spawn an actor we need the `AkkaContext` trait in environment and the `Interop._` extension methods in scope:

```scala
    for {
      system <- actorSystem
      actor <- system.spawn(TestActor.create(), "test-actor")
    } yield actor
```

The idea is that the actor itself should also get its dependencies from ZIO, so the `TestActor.create()` function itself
is also an effectful function:

```scala
object TestActor {
  type Environment = ZioDep with PureDep // A subset of the final environment required by the actor

  def create[R <: Environment]()(implicit interop: Interop[R]): ZIO[Environment, Nothing, Behavior[Message]] =
    ZIO.environment.map(env => new TestActor(env).start())

  sealed trait Message
  // ...
}
```

Then we can use the ask pattern to call an actor from ZIO:

```scala
for {
    testAnswer <- actor.ask[FinalEnvironment, Try[Answer]](TestActor.Question(100, _), 1.second)
    _ <- console.putStrLn(s"Actor answered with: $testAnswer")
} yield ()
```

and we can run ZIO effects from the actor using the `pipeTo` extension method:

```scala
class TestActor(env: TestActor.Environment) // Subset of the ZIO environment required by the actor
               (implicit interop: Interop[TestActor.Environment]) { // needed for the ZIO pipeTo syntax
  import TestActor._

  def start(): Behavior[Message] =
    Behaviors.receive { (ctx, msg) =>
      implicit val ec: ExecutionContext = ctx.executionContext
      msg match {
        case Question(input, respondTo) =>
          // ZIO value is evaluated concurrently and the result is sent back to the actor as a message
          env.zioDep.provideAnswer(input).pipeTo(ctx.self, AnswerReady(_, respondTo))
          Behaviors.same
  // ...
}
```

Note the implicit `Interop` value. This is created during bootstrap and must be passed around to non-ZIO places as
it cannot be part of the environment (in fact it is the thing holding the `runtime` that holds the environment).

## Akka-HTTP
Akka-HTTP is integrated in a very similar way. Dependencies are injected by having a ZIO function that creates the
`Api` value, holding the Akka-HTTP _route_:

```scala
def createHttpApi(interopImpl: Interop[FinalEnvironment],
                  testActor: ActorRef[TestActor.Message]): ZIO[FinalEnvironment, Nothing, Api]
```

`Api` uses the structure that we are using in our current Akka-HTTP services too where different route fragments are
mixed in together:

```scala
val route: Route = futureRoute ~ catsRoute ~ zioRoute ~ actorRoute ~ streamingRoute
```

There is an example for:
- Completing a route with a future
- Completing a route with a cats-effect IO effect
- Completing a route with a ZIO effect
- Completing a route by asking the actor
- Streaming

For the future use case we can use the `onComplete` directive.
For ZIO the `ZIODirectives` trait defines similar helper directives that are using the already mentioned implicit `Interop`
class to run the effect:

```scala
trait ZioDirectives[R] {
  implicit val interop: Interop[R]

  def onZIOComplete[E, A](f: ZIO[R, E, A]): Directive1[Try[Either[E, A]]] =
    onComplete(interop.zioToFutureEither(f))

  def onRIOComplete[A](f: RIO[R, A]): Directive1[Try[A]] =
    onComplete(interop.zioToFuture(f))
}
```

For running cats-effect IO values, `Interop` contains a helper too:

```scala
val ioAnswer = catsDep.provideAnswer(input)
val futureAnswer = interop.ioToFuture(ioAnswer) // or
val zioAnswer = interop.ioToZio(ioAnswer)
```

## ZIO with cats-effect abstractions
Working with libraries built on cats-effect abstractions (such as _prox_) are possible by the `zio-interop-cats` library
which is included in the example. 

## Streaming
Akka-HTTP is a streaming library but ZIO also has a stream type so the need to connect the two may arise. This is possible
with the combination of `zio-interop-reactivestreams` and Akka Streams's built-in reactive streams support.

For example to respond to a request with a ZIO stream:

```scala
val randomChunk: UIO[Chunk[Byte]] = random.nextBytes(16)
val responseStream: ZStream[Any, Nothing, Chunk[Byte]] = zio.stream.Stream.repeatEffect(randomChunk).take(8)

val createResponse: UIO[HttpResponse] = for {
  publisher <- responseStream.toPublisher
  akkaResponseStream = Source.fromPublisher(publisher).map(_.toByteString)
  response = HttpResponse(
    entity = HttpEntity(ContentTypes.`application/octet-stream`, akkaResponseStream)
  )
} yield response
```

To make this smoother there are some extension methods to convert between `ByteString` and `Chunk`.

The opposite direction is similar, handling an incoming request in a ZIO stream:

```scala
  // Defining the ZIO sink that produces the result for the response (counts its length)
  val zioSink = zio.stream.Sink.foldLeft[Chunk[Byte], Int](0)((sum, chunk) => sum + chunk.length)

  // Defining the ZIO effect to create the Akka-HTTP response by running the Akka-HTTP request stream into
  // the ZIO sink.
  val createResponse: IO[Throwable, HttpResponse] = zioSink.toSubscriber().use { case (subscriber, result) =>
    val akkaSink = akka.stream.scaladsl.Sink.fromSubscriber(subscriber)
    bodyStream
      .map(_.toChunk)
      .runWith(akkaSink)

    result.map { finalCount =>
      HttpResponse(entity = HttpEntity(finalCount.toString))
    }
  }
```

## Options
Service options are still based on the Lightbend `config` library as it is used for Akka and other Akka based libraries.
It is loaded as a ZIO effect though and provided to other parts of the application as part of the ZIO environment.
 
## Testing with zio-test
TODO
