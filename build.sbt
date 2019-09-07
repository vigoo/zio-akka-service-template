name := "zio-service-template"

version := "0.1"

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % "2.5.25",
  "com.typesafe.akka" %% "akka-http" % "10.1.9",
  "org.typelevel" %% "cats-effect" % "2.0.0-RC2",
  "dev.zio" %% "zio" % "1.0.0-RC11-1",
  "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC2",
  "io.circe" %% "circe-core" % "0.11.1",
  "de.heikoseeberger" %% "akka-http-circe" % "1.27.0"
)
