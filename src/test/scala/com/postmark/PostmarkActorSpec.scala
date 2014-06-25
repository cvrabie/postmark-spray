package com.postmark

import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.{Props, ActorSystem}
import org.specs2.mutable.SpecificationLike
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

/**
 * User: cvrabie
 * Date: 21/08/2013
 */
class PostmarkActorSpec extends TestKit(ActorSystem("postmark")) with SpecificationLike with ImplicitSender{
  import PostmarkFixture._
  val actor = system.actorOf(Props[PostmarkActor])
  val TIMEOUT = Duration(20, TimeUnit.SECONDS)

  sequential

  "PostmarkActor" >> {
    "handle a message" in{
      actor ! msg
      expectMsgPF(TIMEOUT){
        case Message.Receipt(to,_,id,err,_) =>
          to must_== msg.To.get
          id must not(beEmpty)
          err must_== 0
      }
      success
    }

    "handle a delivery error" in {
      actor ! bad
      expectMsgPF(TIMEOUT){
        case akka.actor.Status.Failure(err:InvalidMessage) =>
          success
      }
    }

    "handle a batch of messages" in{
      val msgs = Message.Batch(Seq(msg, msg2, bad))
      actor ! msgs
      expectMsgPF(TIMEOUT){
        case Array(
          Right(receipt1:Message.Receipt),
          Right(receipt2:Message.Receipt),
          Left(rejection1:Message.Rejection)
        ) =>
          receipt1.Message must_== "Test job accepted"
          receipt2.Message must_== "Test job accepted"
          rejection1.ErrorCode must_== 300
      }
    }
  }
}
