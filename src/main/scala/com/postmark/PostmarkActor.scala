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

import akka.actor.{ActorLogging, Actor}

/**
 * User: cvrabie
 * Date: 21/08/2013
 */
class PostmarkActor extends Actor with ActorLogging{
  import context.dispatcher
  val postmark = new Postmark()(context.system)

  log.info("Postmark Actor is up")

  def receive = {
    case msg:Message =>
      val theSender = sender //we need to close on the sender because in the future callback it will be different
      log.debug("Sending message to {}",msg.recipients)
      val promise = postmark.send(msg)
      promise.onSuccess{
        case receipt:Message.Receipt => 
			theSender ! receipt
			log.info("Sent message to {}", msg.recipients)
      }
      promise.onFailure{
        case err:Throwable => 
			theSender ! akka.actor.Status.Failure(err)
			log.error(err, "Failed to send message to {}", msg.recipients)
      }

    case Message.Batch(msgs) =>
      val theSender = sender
      log.debug("Sending batch of {} messages",msgs.size)
      val promise = postmark.sendBatch(msgs:_*)
      promise.onSuccess{
        case receipts:Array[Either[Message.Rejection,Message.Receipt]] => 
			theSender ! receipts
			log.info("Sent batch of {} messages", msgs.size)
      }
      promise.onFailure{
        case err:Throwable =>
			theSender ! akka.actor.Status.Failure(err)
			log.error(err, "Failed to send batch of {} messages", msgs.size)
      }
  }
}
