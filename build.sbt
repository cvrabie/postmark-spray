organization := "com.postmark"

name := "postmark-client"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.2"

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "io.spray" % "spray-client" % "1.2-M8",
  "com.typesafe.akka" %% "akka-actor" % "2.2.0",
  "io.spray"        %% "spray-json"     % "1.2.5",
  "org.mockito"         % "mockito-all"      % "1.9.0"        % "test",
  "org.specs2"         %% "specs2"           % "2.1"          % "test"
)
