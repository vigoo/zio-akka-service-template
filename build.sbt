name := "zio-service-template"

version := "0.1"

scalaVersion := "2.13.3"

scalacOptions += "-deprecation"

val akkaVersion = "2.6.8"
val akkaHttpVersion = "10.2.0"
val zioVersion = "1.0.1"
val catsEffectVersion = "2.1.4"

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,

  "org.typelevel" %% "cats-effect" % catsEffectVersion,

  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-streams" % zioVersion,
  "dev.zio" %% "zio-interop-cats" % "2.1.4.0",
  "dev.zio" %% "zio-interop-reactivestreams" % "1.0.3.5",
  "dev.zio" %% "zio-config" % "1.0.0-RC26",
  "dev.zio" %% "zio-config-typesafe" % "1.0.0-RC26",
  "dev.zio" %% "zio-config-magnolia" % "1.0.0-RC26",

  "io.circe" %% "circe-core" % "0.13.0",
  "io.circe" %% "circe-generic" % "0.13.0",
  "de.heikoseeberger" %% "akka-http-circe" % "1.34.0",

  "dev.zio" %% "zio-test" % zioVersion % Test,
  "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
  "dev.zio" %% "zio-test-magnolia" % zioVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")