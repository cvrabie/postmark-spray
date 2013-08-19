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
