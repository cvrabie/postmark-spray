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

import spray.json._
import scala.collection.mutable.ListBuffer
import spray.http.{MediaTypes, MediaType}
import com.postmark.Message.Attachment
import java.io._
import org.parboiled.common.Base64
import scala.Some
import java.util.Calendar
import java.text.SimpleDateFormat

/**
 * User: cvrabie
 * Date: 19/08/2013
 */
case class Message(
  From: String,
  To: Option[String],
  Cc: Option[String],
  Bcc: Option[String],
  Subject: Option[String],
  Tag: Option[String],
  HtmlBody: Option[String],
  TextBody: Option[String],
  ReplyTo: Option[String],
  Headers: Seq[(String,String)],
  Attachments: Option[Seq[Attachment]]
){
  if(To.isEmpty && Cc.isEmpty && Bcc.isEmpty)
    throw new InvalidMessage("You need at least one recipient!")

  if(TextBody.isEmpty && HtmlBody.isEmpty)
    throw new InvalidMessage("Provide either email TextBody or HtmlBody or both.")
}

object Message extends DefaultJsonProtocol{
  case class Attachment(
    Name: String,
    MediaType: MediaType,
    Content: String
  )

  implicit val contentTypeFormat = new JsonFormat[MediaType]{
    def write(obj: MediaType) = JsString(obj.value)
    def read(json: JsValue) = json match {
      case JsString(str) => Attachment.defaultTypeDetector(str)
      case js => throw new DeserializationException("Expecting MediaType as string but got "+js)
    }
  }

  implicit val headersJsonFormat = new JsonFormat[Seq[(String,String)]] {
    def write(obj: Seq[(String, String)]) = JsArray(obj.map{
      case (name,value) => new JsObject(Map[String,JsValue](
        "Name" -> JsString(name),
        "Value" -> JsString(value)
      ))
    }:_*)
    def read(json: JsValue) = json match {
      case JsArray(elems) => elems.map{
        case JsObject(fields) => (fields.get("Name"),fields.get("Value")) match {
          case (Some(JsString(name)),Some(JsString(value))) => (name,value)
          case (None,_) => throw new DeserializationException("Expecting header object but the 'Name' field is missing!")
          case (_,None) => throw new DeserializationException("Expecting header object but the 'Value' fields is missing!")
          case other => throw new DeserializationException("""Expecting {"Name":STRING,"Value":String} but got """+other)
        }
        case other => throw new DeserializationException("""Expecting {"Name":STRING,"Value":String} but got """+other)
      }
      case other => throw new DeserializationException("Expecting an array of header objects but got "+other)
    }
  }

  implicit val attachmentJsonFormat = jsonFormat3(Attachment.apply)

  val defaultMessageJsonFormat = jsonFormat11(Message.apply)

  implicit val messageJsonFormat = new RootJsonFormat[Message] {
    def write(obj: Message) = {
      val delegate = obj.toJson(defaultMessageJsonFormat)
      val fields = delegate.asJsObject.fields
      fields.get("Headers") match {
        //if the headers is an empty array don't serialize it
        case Some(JsArray(headers)) if headers.isEmpty => JsObject(fields - "Headers")
        case _ => delegate
      }

    }
    def read(json: JsValue) = json.convertTo[Message](defaultMessageJsonFormat)
  }

  implicit val receiptJsonFormat = jsonFormat5(Receipt.apply)

  implicit val rejectionJsonFormat = jsonFormat2(Rejection.apply)

  implicit val receiptAndRejectionArrayReader = new RootJsonFormat[Array[Either[Message.Rejection,Message.Receipt]]] {
    def read(json: JsValue) = json match {
      case JsArray(arr) => arr.toArray.map( _ match {
        case elem@JsObject(obj) if obj.get("MessageID").isDefined => Right(elem.convertTo[Message.Receipt])
        case elem@JsObject(obj) => Left(elem.convertTo[Message.Rejection])
        case elem => throw new DeserializationException("Expecting either Message.Rejection or Message.Receipt but got"+elem)
      })
      case other => throw new DeserializationException("Expecting Array of Message.Rejection/Message.Receipt but got"+other)
    }

    def write(obj: Array[Either[Rejection, Receipt]]) = JsNull
  }

  object Attachment{
    protected val extensionRegex = "\\.([a-zA-Z0-9-_]*)$".r
    protected def extension(file:File) = extensionRegex findFirstMatchIn file.getName map(_.group(1))

    def defaultTypeDetector:String=>MediaType = (ext:String) =>
      MediaTypes.forExtension(ext.toLowerCase)
      .getOrElse(MediaTypes.`application/octet-stream`)

    protected val Base64Encoder = Base64.custom()
    protected val BUFFER_SIZE = 1024*100

    //TODO this reads the entire file in memory. we should stream files with a custom Marshaller if possible!
    protected def readFile(file:File):Array[Byte] = (file.exists(), file.length()) match {
      case (false, _) => throw new IOException("File %s does not exist!".format(file.getAbsolutePath))
      case (true, length) =>
        val stream = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE)
        try readFile(stream, new Array[Byte](length.toInt), 0) finally {
          stream.close()
        }
    }

    protected def readFile(stream:BufferedInputStream, buf:Array[Byte], offset:Int):Array[Byte] = {
      val read = stream.read(buf, offset, BUFFER_SIZE)
      if(read <= 0) buf else readFile(stream, buf, offset+read)
    }

    def fromFile(file:File)(implicit typeDetector:String=>MediaType = defaultTypeDetector) = Attachment(
      file.getName, typeDetector(extension(file).getOrElse("")), Base64Encoder.encodeToString(readFile(file),false)
    )
  }

  class Builder{
    private var From:Option[String] = None
    private val To:ListBuffer[String] = ListBuffer.empty
    private val Cc:ListBuffer[String] = ListBuffer.empty
    private val Bcc:ListBuffer[String] = ListBuffer.empty
    private var Subject:Option[String] = None
    private val Tag:ListBuffer[String] = ListBuffer.empty
    private var HtmlBody:Option[String] = None
    private var TextBody:Option[String] = None
    private var ReplyTo:Option[String] = None
    private val Headers:ListBuffer[(String,String)] = ListBuffer.empty
    private val Attachments:ListBuffer[Attachment] = ListBuffer.empty

    def from(from:String):Builder = { From = Some(from); this }
    def to(to:String):Builder = { To += to; this }
    def cc(cc:String):Builder = { Cc += cc; this }
    def bcc(bcc:String):Builder = { Bcc += bcc; this }
    def subject(subject:String):Builder = { Subject = Some(subject); this }
    def tag(tag:String):Builder = { Tag += tag; this }
    def htmlBody(htmlBody:String):Builder = { HtmlBody = Some(htmlBody); this }
    def textBody(textBody:String):Builder = { TextBody= Some(textBody); this }
    def replyTo(replyTo:String):Builder = { ReplyTo = Some(replyTo); this }
    def header(header:(String,String)):Builder = { Headers += header; this }
    def attachment(attachment:Attachment):Builder = { Attachments += attachment; this }
    def attachment(file:File):Builder = { Attachments += Attachment.fromFile(file); this }

    def to(to:String*):Builder = { To ++= to; this }
    def cc(cc:String*):Builder = { Cc ++= cc; this }
    def bcc(bcc:String*):Builder = { Bcc ++= bcc; this }
    def tags(tags:String*):Builder = { Tag ++= tags; this }
    def headers(headers:(String,String)*):Builder = { Headers ++= headers; this }
    def attachments(attachments:Attachment*):Builder = { Attachments ++= attachments; this }

    def clearFrom = { From = None; this }
    def clearTo = { To.clear(); this }
    def clearCc = { Cc.clear(); this }
    def clearBcc = { Bcc.clear(); this }
    def clearSubject = { Subject = None; this }
    def clearTag = { Tag.clear(); this }
    def clearHtmlBody = { HtmlBody = None; this }
    def clearTextBody = { TextBody = None; this }
    def clearReplyTo = { ReplyTo = None; this }
    def clearHeaders = { Headers.clear(); this }
    def clearAttachments = { Attachments.clear(); this }

    private def buildFrom =
      if(From.isEmpty) throw new InvalidMessage("Message needs From field!")
      else From.mkString(",")

    private def buildTo =
      if(To.size + From.size + Bcc.size > Message.Builder.MAX_RECIPIENTS)
        throw new InvalidMessage("Postmark accepts maximum %d recipients!".format(Message.Builder.MAX_RECIPIENTS))
      else if(To.isEmpty) None else Some(To.mkString(","))

    def build = new Message(
      buildFrom, buildTo,
      if(Cc.isEmpty) None else Some(Cc.mkString(",")),
      if(Bcc.isEmpty) None else Some(Bcc.mkString(",")),
      Subject,
      if(Tag.isEmpty) None else Some(Tag.mkString(",")),
      HtmlBody, TextBody, ReplyTo, Headers.toList,
      if(Attachments.isEmpty) None else Some(Attachments.toList)
    )
  }

  object Builder{
    val MAX_RECIPIENTS = 20
    def apply() =  new Builder()
    implicit def builderToMessage(builder:Builder) = builder.build
  }

  case class Receipt(
    val To:String,
    val SubmittedAt:String,
    val MessageID:String,
    val ErrorCode:Int,
    val Message:String
  )

  case class Rejection(
    val ErrorCode: Int,
    val Message: String
  )

  case class Batch(val msgs:Seq[Message])
}