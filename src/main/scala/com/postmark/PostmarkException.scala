package com.postmark

/**
 * User: cvrabie
 * Date: 19/08/2013
 */
abstract class PostmarkException(val msg:String, val cause:Option[Throwable] = None)
extends Exception(msg, cause.getOrElse(null))

object PostmarkException{
  def unapply(e:PostmarkException) = Some(e.msg, e.cause)
}

case class InvalidMessageException(override val msg:String, override val cause:Option[Throwable] = None)
extends PostmarkException(msg, cause)

