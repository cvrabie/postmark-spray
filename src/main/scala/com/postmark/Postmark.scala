/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Cristian Vrabie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.postmark

import akka.actor.ActorSystem
import akka.event.Logging
import spray.client.pipelining._
import scala.concurrent.Future
import spray.http._
import spray.json._
import spray.httpx.{UnsuccessfulResponseException, PipelineException, SprayJsonSupport}
import spray.httpx.unmarshalling._
import spray.http.HttpRequest
import spray.http.HttpResponse
import StatusCodes._

class Postmark(implicit val system:ActorSystem){
  import system.dispatcher // execution context for futures below
  import SprayJsonSupport._

  val log = Logging(system, getClass)

  val exceptionHandler:HttpResponse => Future[HttpResponse] = response => response.status match {
    case StatusCodes.OK => Future.successful(response)
    case StatusCodes.Unauthorized => Future.failed(ApiTokenException("unauthorized dude!"))
    case _ => Future.successful(response)
  }

  val pipeline: HttpRequest => Future[HttpResponse] = (
    addHeader("X-Postmark-Server-Token",PostmarkConfig.default.token) ~>
    addHeader(HttpHeaders.Accept(MediaTypes.`application/json`)) ~>
    logRequest(log) ~>
    sendReceive ~>
    logResponse(log)
  )

  protected def extractRejection(response:HttpResponse):Either[Throwable,Message.Rejection] =
      try{ Right( unmarshal[Message.Rejection].apply(response) ) }
      catch{ case e:Throwable => Left(e) }

  protected def receipt(response:HttpResponse):Future[Message.Receipt] =
    //if case the response is 200 OK try to deserialize a Message.Receipt out of it
    response.entity.as[Message.Receipt] match {
      //if deserialization is ok return the receipt as a successful future
      case Right(message) => Future.successful(message)
      //if we can't deserialize a receipt transform, the response to a failure
      case Left(error) => Future.failed(
        new InvalidMessageException(
          "Got 200 but response is not a Message.Receipt",
          Some(new DeserializationException(error.toString))
        )
      )
    }

  protected def translateExceptions(response:HttpResponse):Throwable =
    (response.status, extractRejection(response)) match {
        case (UnprocessableEntity, Right(rejection)) => new InvalidMessageException(rejection.Message)
        case (UnprocessableEntity, _) => new InvalidMessageException(("%d: Entity seems to be invalid but " +
          "we were not offered a cause! %s").format(UnprocessableEntity.intValue,response.toString))
        case _ => new Exception("Some kind of error occured")
    }


  def send(message:Message):Future[Message.Receipt] = pipeline(
    Post(PostmarkConfig.default.url, message)
  ).flatMap{
    case response if response.status == StatusCodes.OK => receipt(response)
    case response => Future.failed(translateExceptions(response))
  }
}