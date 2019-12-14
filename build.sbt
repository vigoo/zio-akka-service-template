name := "zio-service-template"

version := "0.1"

scalaVersion := "2.13.1"

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % "2.6.1",
  "com.typesafe.akka" %% "akka-stream" % "2.6.1",
  "com.typesafe.akka" %% "akka-http" % "10.1.11",
  "org.typelevel" %% "cats-effect" % "2.0.0",
  "dev.zio" %% "zio" % "1.0.0-RC17",
  "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC10",
  "dev.zio" %% "zio-interop-reactivestreams" % "1.0.3.5-RC2",
  "dev.zio" %% "zio-macros-core" % "0.6.0",
  "io.circe" %% "circe-core" % "0.12.3",
  "io.circe" %% "circe-generic" % "0.12.3",
  "de.heikoseeberger" %% "akka-http-circe" % "1.29.1",

  "dev.zio" %% "zio-test" % "1.0.0-RC17" % Test,
  "org.specs2" %% "specs2-core" % "4.8.1" % Test,
  "org.specs2" %% "specs2-junit" % "4.8.1" % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.6.1" % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.11" % Test,
)

scalacOptions += "-Ymacro-annotations"

//addCompilerPlugin("org.scalamacros" % "paradise"  % "2.1.1" cross CrossVersion.full)
