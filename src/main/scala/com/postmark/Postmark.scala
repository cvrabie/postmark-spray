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
import spray.httpx.SprayJsonSupport
import spray.httpx.unmarshalling._
import spray.http.HttpRequest
import spray.http.HttpResponse
import StatusCodes._

class Postmark(implicit val system:ActorSystem){
  import system.dispatcher // execution context for futures below
  import SprayJsonSupport._
  import Postmark._

  protected val log = Logging(system, getClass)

  protected val pipeline: HttpRequest => Future[HttpResponse] = (
    addHeader(HEADER_AUTH,PostmarkConfig.default.token) ~>
    addHeader(HttpHeaders.Accept(MediaTypes.`application/json`)) ~>
    logRequest(log) ~>
    sendReceive ~>
    logResponse(log)
  )

  protected def receipt(response:HttpResponse):Future[Message.Receipt] =
    //if case the response is 200 OK try to deserialize a Message.Receipt out of it
    response.entity.as[Message.Receipt] match {
      //if deserialization is ok return the receipt as a successful future
      case Right(message) => Future.successful(message)
      //if we can't deserialize a receipt transform, the response to a failure
      case Left(error) => Future.failed(
        new InvalidMessage(MSG_NO_RECEIPT, Some(new DeserializationException(error.toString))))
    }

  protected def receipts(response:HttpResponse):Future[Array[Either[Message.Rejection,Message.Receipt]]] =
    response.entity.as[Array[Either[Message.Rejection,Message.Receipt]]] match {
      case Right(arr) => Future.successful(arr)
      case Left(error) => Future.failed(
        new InvalidMessage(MSG_NO_BATCH_RECEIPT, Some(new DeserializationException(error.toString)))
      )
    }

  protected def translateExceptions(response:HttpResponse):Throwable =
    (response.status, response.entity.as[Message.Rejection]) match {

      case (UnprocessableEntity, Right(Message.Rejection(0,msg))) =>
        new ApiTokenException(msg)

      case (UnprocessableEntity, Right(Message.Rejection(300,msg))) =>
        new InvalidEmailRequest(msg)

      case (UnprocessableEntity, Right(Message.Rejection(code,msg))) if code==400 || code==401 =>
        new SenderSignatureNotFound(msg)

      case (UnprocessableEntity, Right(Message.Rejection(code,msg))) if code==402 || code==403 || code==409 =>
        new InvalidJson(msg)

      case (UnprocessableEntity, Right(Message.Rejection(405,msg)))=>
        new NotAllowedToSend(msg)

      case (UnprocessableEntity, Right(Message.Rejection(406,msg)))=>
        new InactiveRecipient(msg)

      case (UnprocessableEntity, Right(Message.Rejection(code,msg))) if code==407 || code==408 =>
        new BounceException(msg)

      case (UnprocessableEntity, Right(Message.Rejection(410,msg)))=>
        new TooManyBatchMessages(msg)

      case (UnprocessableEntity, Right(Message.Rejection(code,msg)))=>
        new InvalidMessage("%d: %s".format(code,msg))

      case (UnprocessableEntity, _) => new InvalidMessage(
        MSG_UNPROCESSABLE.format(UnprocessableEntity.intValue,response.toString))

      case (Unauthorized, Right(rejection)) =>
        new ApiTokenException(rejection.Message)

      case (Unauthorized, _) => new ApiTokenException(
        MSG_UNAUTHORIZED.format(Unauthorized.intValue))

      case (InternalServerError, _) =>
        new RuntimeException(MSG_INTERNAL)

      case _ =>
        new RuntimeException(MSG_UNKNOWN.format(response))
    }


  def send(message:Message):Future[Message.Receipt] = pipeline(
    Post(PostmarkConfig.default.sendUrl, message)
  ).flatMap{
    case response if response.status == StatusCodes.OK => receipt(response)
    case response => Future.failed(translateExceptions(response))
  }

  def sendBatch(messages:Message*):Future[Array[Either[Message.Rejection,Message.Receipt]]] = pipeline(
    Post(PostmarkConfig.default.batchUrl, messages)
  ).flatMap{
    case response if response.status == StatusCodes.OK => receipts(response)
    case response => Future.failed(translateExceptions(response))
  }
}

object Postmark{
  val HEADER_AUTH = "X-Postmark-Server-Token"
  val MSG_NO_RECEIPT = "Got 200 but response is not a Message.Receipt"
  val MSG_NO_BATCH_RECEIPT = "Got 200 but response is not an Array of Either[Message.Rejection,Message.Receipt]"
  val MSG_UNPROCESSABLE = "%d: Entity seems to be invalid but we were not offered a cause! %s"
  val MSG_UNAUTHORIZED = "%d: You are not authorized to do that! Set your API Key in application.conf"
  val MSG_UNKNOWN = "Unknown error has occurred! Response: %s"
  val MSG_INTERNAL = "500 Internal Postmark Error!"
}