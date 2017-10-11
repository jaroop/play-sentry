package test

import com.jaroop.play.sentry._
import org.mockito.Matchers._
import org.specs2.concurrent._
import org.specs2.mock._
import org.specs2.mutable._
import play.api.mvc._
import scala.reflect._
import scala.concurrent.Future
import scala.concurrent.duration._

class TransparentIdContainerSpec(implicit ee: ExecutionEnv) extends Specification with Mockito {

    "TransparentIdContainer" should {

        tag("startNewSession")
        "return a String user id as a String" in {
            new TransparentIdContainer[String].startNewSession("TestUser", Duration.Inf) must equalTo("TestUser").await
        }

        tag("startNewSession")
        "return an Int user id as a String" in {
            new TransparentIdContainer[Int].startNewSession(12345, Duration.Inf) must equalTo("12345").await
        }

        tag("startNewSession")
        "return a Long user id as a String" in {
            new TransparentIdContainer[Long].startNewSession(1234567890L, Duration.Inf) must equalTo("1234567890").await
        }

        tag("get")
        "return the user token as String" in {
            // This can never really fail
            new TransparentIdContainer[String].get("TestUser") must beSome("TestUser").await
        }

        tag("get")
        "return the user token as an Int" in {
            new TransparentIdContainer[Int].get("1234") must beSome(1234).await
        }

        tag("get")
        "return None when the stored token isn't a valid Int" in {
            new TransparentIdContainer[Int].get("1234adfsd") must beNone.await
        }

        tag("get")
        "return the user token as a Long" in {
            new TransparentIdContainer[Long].get("1234324872983") must beSome(1234324872983L).await
        }

        tag("get")
        "return None when the stored token isn't a valid Long" in {
            new TransparentIdContainer[Long].get("1234324872x983") must beNone.await
        }

    }

}
