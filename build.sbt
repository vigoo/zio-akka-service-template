name := "zio-service-template"

version := "0.1"

scalaVersion := "2.12.9"

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % "2.5.25",
  "com.typesafe.akka" %% "akka-http" % "10.1.9",
  "org.typelevel" %% "cats-effect" % "2.0.0",
  "dev.zio" %% "zio" % "1.0.0-RC12-1",
  "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC3",
  "dev.zio" %% "zio-delegate" % "0.0.3",
  "io.circe" %% "circe-core" % "0.11.1",
  "io.circe" %% "circe-generic" % "0.11.1",
  "de.heikoseeberger" %% "akka-http-circe" % "1.27.0"
)

addCompilerPlugin("org.scalamacros" % "paradise"  % "2.1.1" cross CrossVersion.full)
