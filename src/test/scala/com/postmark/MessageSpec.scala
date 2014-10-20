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
import com.postmark.Message.Attachment
import java.io.File

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
        """{"HtmlBody":"<h1 color=\"red\">Hello</h1>","Headers":[{"Name":"a","Value":"b"},{"Name":"c","Value":"d"}],"Tag":"tag1,tag2","To":"a@example.com,b@example.com","From":"cristian@example.com"}"""
    }

    "not serialize optional fields that were not set" in{
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
      .build must throwA[InvalidMessage]
    }

    "fail if no recipient was set" in {
      Message.Builder().from("cristian@example.com").htmlBody("<h1>Hello</h1>")
        .build must throwA[InvalidMessage]
    }

    "fail if there are more than 20 recipients" in {
      val builder =  Message.Builder().from("cristian@example.com").htmlBody("<h1>Hello</h1>")
      val emails = (1 to 21).map(i=>"i"+i+"@example.com")
      builder.to(emails:_*).build must throwA[InvalidMessage]
    }

    "fail if no body was set"in {
      Message.Builder().from("cristian@example.com").to("b@example.com")
        .build must throwA[InvalidMessage]
    }

    "construct a message with minimum data" in {
      Message.Builder().from("cristian@example.com").to("b@example.com").textBody("abc")
        .build must beLike{
        case m:Message => m.TextBody must beSome.which(_=="abc")
      }
    }

    "properly encode and attachment" in{
      val path = this.getClass.getResource("/baboon.jpg")
      val file = new File(path.toURI)
      val msg = Message.Builder()
      .from("cristian@example.com")
      .to("a@example.com")
      .htmlBody("<h1>Hello</h1>")
      .attachment(file)
      .build

      msg.Attachments must beLike{
        case Some(Seq(Attachment(name, mediatype, content)))=>
          name must_== "baboon.jpg"
          mediatype.toString() must_== "image/jpeg"
          content must_==
"""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
  |Sae5tFJo/9k=""".stripMargin.replace("\n","")
      }
    }

  }
}
