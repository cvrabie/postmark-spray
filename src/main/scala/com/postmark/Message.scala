package com.postmark

import spray.json._
import scala.collection.mutable.ListBuffer
import spray.http.{MediaTypes, MediaType}
import com.postmark.Message.Attachment
import java.io._
import org.parboiled.common.Base64
import scala.Some

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
)

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

  implicit val messageJsonFormat = jsonFormat11(Message.apply)


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
      if(From.isEmpty) throw new InvalidMessageException("Message needs From field!")
      else From.mkString(",")

    private def buildTo =
      if(To.isEmpty && Cc.isEmpty && Bcc.isEmpty)
        throw new InvalidMessageException("You need at least one recipient!")
      else if(To.size + From.size + Bcc.size > Builder.MAX_RECIPIENTS)
        throw new InvalidMessageException("Postmark accepts maximum %d recipients!".format(Builder.MAX_RECIPIENTS))
      else if(To.isEmpty) None else Some(To.mkString(","))

    private def buildBody =
      if(HtmlBody.isEmpty && HtmlBody.isEmpty)
        throw new InvalidMessageException("Provide either email TextBody or HtmlBody or both.")
      else HtmlBody

    def build = new Message(
      buildFrom, buildTo, if(Cc.isEmpty) None else Some(Cc.mkString(",")),
      if(Bcc.isEmpty) None else Some(Bcc.mkString(",")), Subject,
      if(Tag.isEmpty) None else Some(Tag.mkString(",")),
      buildBody, TextBody, ReplyTo, Headers.toList,
      if(Attachments.isEmpty) None else Some(Attachments.toList)
    )
  }

  object Builder{
    val MAX_RECIPIENTS = 20
    def apply() =  new Builder()
    implicit def builderToMessage(builder:Builder) = builder.build
  }
}