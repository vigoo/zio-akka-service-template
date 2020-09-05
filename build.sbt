name := "zio-service-template"

version := "0.1"

scalaVersion := "2.13.1"

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % "2.6.3",
  "com.typesafe.akka" %% "akka-stream" % "2.6.3",
  "com.typesafe.akka" %% "akka-http" % "10.1.11",
  "org.typelevel" %% "cats-effect" % "2.1.2",
  "dev.zio" %% "zio" % "1.0.0-RC18-2",
  "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC12",
  "dev.zio" %% "zio-interop-reactivestreams" % "1.0.3.5-RC6",
  "dev.zio" %% "zio-macros-core" % "0.6.2",
  "io.circe" %% "circe-core" % "0.13.0",
  "io.circe" %% "circe-generic" % "0.13.0",
  "de.heikoseeberger" %% "akka-http-circe" % "1.31.0",

  "dev.zio" %% "zio-test" % "1.0.0-RC18-2" % Test,
  "org.specs2" %% "specs2-core" % "4.9.2" % Test,
  "org.specs2" %% "specs2-junit" % "4.9.2" % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.6.3" % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.11" % Test,
)

scalacOptions += "-Ymacro-annotations"

//addCompilerPlugin("org.scalamacros" % "paradise"  % "2.1.1" cross CrossVersion.full)
