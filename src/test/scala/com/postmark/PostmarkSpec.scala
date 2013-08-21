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
  val msg = Message.Builder().from("cristian.vrabie@gmail.com").to("cristian@vrabie.info").textBody("Hello").build
  val bad = msg.copy(To = Some(
    "1@vrabie.info,2@vrabie.info,3@vrabie.info,4@vrabie.info,5@vrabie.info,6@vrabie.info,7@vrabie.info,8@vrabie.info" +
      ",9@vrabie.info,10@vrabie.info,11@vrabie.info,12@vrabie.info,13@vrabie.info,14@vrabie.info,15@vrabie.info" +
      ",16@vrabie.info,17@vrabie.info,18@vrabie.info,19@vrabie.info,20@vrabie.info,21@vrabie.info"
  ))
  val log = Logging(system, getClass)
  val WAIT = 5000 milli
}

class PostmarkSpec extends Specification{
  import PostmarkFixture._

  "\nPostmark" should {

    "send a call to Postmark" in {
      Await.result(client.send(msg),WAIT) must beLike{
        case Message.Receipt(to,_,id,err,msg) =>
          msg must_== "Test job accepted"
          to must_== "cristian@vrabie.info"
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

  }
}
