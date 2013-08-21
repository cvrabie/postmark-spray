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

/**
 * User: cvrabie
 * Date: 19/08/2013
 */
abstract class PostmarkException(val msg:String, val cause:Option[Throwable] = None)
extends Exception(msg, cause.getOrElse(null))

object PostmarkException{
  def unapply(e:PostmarkException) = Some((e.msg, e.cause))
}

class InvalidMessage(override val msg:String, override val cause:Option[Throwable] = None)
extends PostmarkException(msg, cause)

object InvalidMessage{
  def unapply(e:InvalidMessage) = Some((e.msg, e.cause))
}

/**
 * You ran out of credits.
 * @param msg
 */
case class NotAllowedToSend(override val msg:String) extends PostmarkException(msg)

/**
 * Validation failed for the email request JSON data that you provided.
 * @param msg
 */
case class InvalidEmailRequest(override val msg:String) extends InvalidMessage(msg)

/**
 * You are trying to send email with a From address that does not have a sender signature.
 * OR
 * You are trying to send email with a From address that does not have a corresponding confirmed sender signature.
 * @param msg
 */
case class SenderSignatureNotFound(override val msg:String) extends InvalidMessage(msg)

/**
 * The JSON input you provided is syntactically incorrect.
 * OR
 * The JSON input you provided is syntactically correct, but still not the one we expect.
 * OR
 * Your HTTP request does not have the Accept and Content-Type headers set to application/json.
 * @param msg
 */
case class InvalidJson(override val msg:String) extends InvalidMessage(msg)

/**
 * You tried to send to a recipient that has been marked as inactive. Inactive recipients are ones that have
 * generated a hard bounce or a spam complaint.
 * @param msg
 */
case class InactiveRecipient(override val msg:String) extends InvalidMessage(msg)

/**
 * Your request did not submit the correct API token in the X-Postmark-Server-Token header.
 * @param msg
 */
case class ApiTokenException(override val msg:String)
extends PostmarkException(msg, None)

/**
 * You requested a bounce by ID, but we could not find an entry in our database.
 * OR
 * You provided bad arguments as a bounces filter.
 * @param msg
 */
case class BounceException(override val msg:String) extends InvalidMessage(msg)

/**
 * Your batched request contains more than 500 messages.
 * @param msg
 */
case class TooManyBatchMessages(override val msg:String) extends InvalidMessage(msg)

case class ErrorResponse(val errorCode:Int, val message:String)

object ErrorResponse{
  import spray.json.DefaultJsonProtocol._
  implicit val errorResponseJsonFormat = jsonFormat2(ErrorResponse.apply)
}
