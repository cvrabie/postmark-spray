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

import org.specs2.mutable.Specification
import akka.actor.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.event.Logging

/**
 * User: cvrabie
 * Date: 20/08/2013
 */
object PostmarkFixture{
  implicit val system = ActorSystem("postmark")
  val client = new Postmark()
  val msg = Message.Builder().from("1@example.com").to("2@example.com").textBody("Hello").build
  val msg2 = msg.copy(TextBody = Some("bcd"))
  val bad = msg.copy(To = Some(
    "1@example.com,2@example.com,3@example.com,4@example.com,5@example.com,6@example.com,7@example.com,8@example.com" +
      ",9@example.com,10@example.com,11@example.com,12@example.com,13@example.com,14@example.com,15@example.com" +
      ",16@example.com,17@example.com,18@example.com,19@example.com,20@example.com,21@example.com"
  ))
  val log = Logging(system, getClass)
  val WAIT = 5000 milli
}

class PostmarkSpec extends Specification{
  import PostmarkFixture._

  "\nPostmark" should {

    "send a message to Postmark" in {
      Await.result(client.send(msg),WAIT) must beLike{
        case Message.Receipt(to,_,id,err,msg) =>
          msg must_== "Test job accepted"
          to must_== "2@example.com"
          err must_== 0
      }
    }

    "translate InvalidMessageExceptions" in {
      Await.result(client.send(bad).failed,WAIT) must beLike{
        case InvalidMessage(msg,_) =>
          log.debug("Got InvalidMessageException(%s)".format(msg))
          msg must not(beEmpty)
      }
    }

    "send a batch to Postmark" in {
      Await.result(client.sendBatch(msg,msg2,bad),WAIT) must beLike{
        case Array(Right(receipt1),Right(receipt2),Left(rejection1)) =>
          receipt1.Message must_== "Test job accepted"
          receipt2.Message must_== "Test job accepted"
          rejection1.ErrorCode must_== 300
      }
    }

  }
}
