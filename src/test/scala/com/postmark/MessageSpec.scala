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
      Message(Some("cristian@example.com"),Some("a@example.com,b@example.com"),None,None,None,Some("tag1,tag2"),
        Some("""<h1 color="red">Hello</h1>"""),None,None,Seq(("a","b"),("c","d")),None).toJson.compactPrint must_==
        """{
          |"From":"cristian@example.com",
          |"To":"a@example.com,b@example.com",
          |"Tag":"tag1,tag2",
          |"HtmlBody":"<h1 color=\"red\">Hello</h1>",
          |"Headers":[{"Name":"a","Value":"b"},{"Name":"c","Value":"d"}]
          |}""".stripMargin.replaceAll("\n","")
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
        case Message( Some(from), Some(to), None, None, None, Some(tag), Some(body),  None, None, head, None) =>
          from must_== "cristian@example.com"
          to must_== "a@example.com,b@example.com"
          tag must_== "tag1,tag2"
          body must_== "<h1>Hello</h1>"
          head must_==  Seq(("a","b"),("c","d"))
      }
    }
  }
}
