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
import org.specs2.mock.Mockito

/**
 * User: cvrabie
 * Date: 19/08/2013
 */
class MessageSpec extends Specification with Mockito{
  "Message" should {

    "be serializable to JSON" in {
      import spray.json._
      Message("cristian@example.com",Some("a@example.com,b@example.com"),None,None,None,Some("tag1,tag2"),
        Some("""<h1 color="red">Hello</h1>"""),None,None,Seq(("a","b"),("c","d")),None).toJson.compactPrint must_==
        """{
          |"From":"cristian@example.com",
          |"To":"a@example.com,b@example.com",
          |"Tag":"tag1,tag2",
          |"HtmlBody":"<h1 color=\"red\">Hello</h1>",
          |"Headers":[{"Name":"a","Value":"b"},{"Name":"c","Value":"d"}]
          |}""".stripMargin.replaceAll("\n","")
    }

    "not serialize optional fields tat were not set" in{
      import spray.json._
      val msg = Message.Builder().from("cristian@example.com").to("a@example.com").textBody("abc").build
      msg.toJson.compactPrint must_== """{"From":"cristian@example.com","To":"a@example.com","TextBody":"abc"}"""
    }

  }

  "Message.Builder" should {

    "properly construct a Message" in {
      val msg:Message = Message.Builder()
        .from("cristian@example.com")
        .to("a@example.com").to("b@example.com")
        .tags("tag1", "tag2").htmlBody("<h1>Hello</h1>")
        .headers("a"->"b","c"->"d")

      msg must beLike{
        case Message( from, Some(to), None, None, None, Some(tag), Some(body),  None, None, head, None) =>
          from must_== "cristian@example.com"
          to must_== "a@example.com,b@example.com"
          tag must_== "tag1,tag2"
          body must_== "<h1>Hello</h1>"
          head must_==  Seq(("a","b"),("c","d"))
      }
    }

    "fail if the From field was not set" in {
      Message.Builder().to("b@example.com").htmlBody("<h1>Hello</h1>")
      .build must throwA[InvalidMessageException]
    }

    "fail if no recipient was set" in {
      Message.Builder().from("cristian@example.com").htmlBody("<h1>Hello</h1>")
        .build must throwA[InvalidMessageException]
    }

    "fail if there are more than 20 recipients" in {
      val builder =  Message.Builder().from("cristian@example.com").htmlBody("<h1>Hello</h1>")
      val emails = (1 to 21).map(i=>"i"+i+"@example.com")
      builder.to(emails:_*).build must throwA[InvalidMessageException]
    }

    "fail if no body was set"in {
      Message.Builder().from("cristian@example.com").to("b@example.com")
        .build must throwA[InvalidMessageException]
    }

    "construct a message with minimum data" in {
      Message.Builder().from("cristian@example.com").to("b@example.com").textBody("abc")
        .build must beLike{
        case m:Message => m.TextBody must beSome.which(_=="abc")
      }
    }
  }
}
