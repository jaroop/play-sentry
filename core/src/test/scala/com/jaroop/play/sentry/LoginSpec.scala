package test

import com.jaroop.play.sentry._
import org.mockito.Matchers._
import org.specs2.concurrent._
import org.specs2.mock._
import org.specs2.mutable._
import play.api.mvc._
import scala.concurrent.Future
import scala.concurrent.duration._

class LoginSpec(implicit ee: ExecutionEnv) extends Specification with Mockito {

    "Login" should {

        tag("gotoLoginSucceeded")
        "start a new session for a user ID and return a result containing the session token" in {
            implicit val request = mock[RequestHeader]
            val token = "abcdef"
            val userId = 1L
            val originalResult = Results.Accepted
            val expectedResult = Results.Ok
            val config = mock[AuthConfig[TestEnv]]
            config.sessionTimeout.returns(1.hour)
            val idContainer = mock[IdContainer[Long]]
            idContainer.startNewSession(userId, 1.hour).returns(Future.successful(token))
            val tokenAccessor = mock[TokenAccessor]
            tokenAccessor.put(token)(originalResult).returns(expectedResult)
            val login = new Login[TestEnv](config, idContainer, tokenAccessor)
            login.gotoLoginSucceeded(userId, Future.successful(originalResult)) must equalTo(expectedResult).await
        }

        tag("gotoLoginSucceeded")
        "return an error response when the IdContainer fails to start a new session" in {
            implicit val request = mock[RequestHeader]
            val config = mock[AuthConfig[TestEnv]]
            config.sessionTimeout.returns(1.hour)
            val idContainer = mock[IdContainer[Long]]
            idContainer.startNewSession(anyObject, anyObject)(anyObject).returns(Future.failed(new Exception))
            val tokenAccessor = mock[TokenAccessor]
            tokenAccessor.put(anyObject)(anyObject)(anyObject).returns(Results.Ok)
            val login = new Login[TestEnv](config, idContainer, tokenAccessor)
            login.gotoLoginSucceeded(1L, Future.successful(Results.Ok)) must throwA[Exception].await
        }

        tag("gotoLoginSucceeded")
        "return an error response when the given result fails" in {
            implicit val request = mock[RequestHeader]
            val config = mock[AuthConfig[TestEnv]]
            config.sessionTimeout.returns(1.hour)
            val idContainer = mock[IdContainer[Long]]
            idContainer.startNewSession(anyObject, anyObject)(anyObject).returns(Future.successful("abcdef"))
            val tokenAccessor = mock[TokenAccessor]
            tokenAccessor.put(anyObject)(anyObject)(anyObject).returns(Results.Ok)
            val login = new Login[TestEnv](config, idContainer, tokenAccessor)
            login.gotoLoginSucceeded(1L, Future.failed(new Exception)) must throwA[Exception].await
        }

    }

}
