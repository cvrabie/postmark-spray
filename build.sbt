//
// The MIT License (MIT)
//
// Copyright (c) 2013 Cristian Vrabie
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of
// this software and associated documentation files (the "Software"), to deal in
// the Software without restriction, including without limitation the rights to
// use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
// the Software, and to permit persons to whom the Software is furnished to do so,
// subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
// IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//

organization := "com.postmark"

name := "postmark-spray"

version := "0.3"

scalaVersion := "2.10.2"

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io",
  "spray nightly" at "http://nightlies.spray.io",
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "io.spray" 		% "spray-client"     % "1.2-20131004",
  "com.typesafe.akka"   %% "akka-actor"      % "2.2.0",
  "io.spray"            %% "spray-json"      % "1.2.5",
  "com.typesafe.akka"   %% "akka-slf4j"      % "2.2.0",
  "ch.qos.logback"      % "logback-classic"  % "1.0.13",
  "org.mockito"         % "mockito-all"      % "1.9.0"        % "test",
  "org.specs2"          %% "specs2"          % "2.1"          % "test",
  "com.typesafe.akka"   %% "akka-testkit"    % "2.2.0"        % "test",
  "junit"               % "junit"            % "4.7"          % "test",
  "com.novocode"        % "junit-interface"  % "0.7"          % "test->default"
)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Ywarn-dead-code",
  "-language:_",
  "-target:jvm-1.6",
  "-encoding", "UTF-8"
)

testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "junitxml", "console")

net.virtualvoid.sbt.graph.Plugin.graphSettings
