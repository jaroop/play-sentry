package test

import com.jaroop.play.sentry._
import org.mockito.Matchers._
import org.specs2.concurrent._
import org.specs2.mock._
import org.specs2.mutable._
import play.api.mvc._
import scala.concurrent.Future
import scala.concurrent.duration._

class LogoutSpec(implicit ee: ExecutionEnv) extends Specification with Mockito {

    "Logout" should {

        tag("gotoLogoutSucceeded")
        "destroy a users's session both the id container and token accessor" in {
            implicit val request = mock[RequestHeader]
            val token = "abcdef"
            val originalResult = Results.Accepted
            val expectedResult = Results.Ok
            val config = mock[AuthConfig[TestEnv]]
            val idContainer = mock[IdContainer[Long]]
            idContainer.remove(token).returns(Future.successful(()))
            val tokenAccessor = mock[TokenAccessor]
            tokenAccessor.extract(request).returns(Option(token))
            tokenAccessor.delete(originalResult).returns(expectedResult)
            val logout = new Logout[TestEnv](config, idContainer, tokenAccessor)
            logout.gotoLogoutSucceeded(Future.successful(originalResult)) must equalTo(expectedResult).await
            there was one(idContainer).remove(token)
        }

        tag("gotoLogoutSucceeded")
        "do nothing when the user has already been logged out" in {
            implicit val request = mock[RequestHeader]
            val token = "abcdef"
            val expectedResult = Results.Ok
            val config = mock[AuthConfig[TestEnv]]
            val idContainer = mock[IdContainer[Long]]
            idContainer.remove(token).returns(Future.successful(()))
            val tokenAccessor = mock[TokenAccessor]
            tokenAccessor.extract(request).returns(Option.empty[AuthenticityToken])
            tokenAccessor.delete(expectedResult).returns(expectedResult)
            val logout = new Logout[TestEnv](config, idContainer, tokenAccessor)
            logout.gotoLogoutSucceeded(Future.successful(expectedResult)) must equalTo(expectedResult).await
            there was no(idContainer).remove(token)
        }

        tag("gotoLogoutSucceeded")
        "return an error response when the given result fails" in {
            // The user will keep their cookie, but the cache will no longer know they have a session
            implicit val request = mock[RequestHeader]
            val token = "abcdef"
            val config = mock[AuthConfig[TestEnv]]
            val idContainer = mock[IdContainer[Long]]
            idContainer.remove(token).returns(Future.successful(()))
            val tokenAccessor = mock[TokenAccessor]
            tokenAccessor.extract(request).returns(Option(token))
            tokenAccessor.delete(Results.Ok).returns(Results.Ok)
            val logout = new Logout[TestEnv](config, idContainer, tokenAccessor)
            logout.gotoLogoutSucceeded(Future.failed(new Exception)) must throwA[Exception].await
            there was one(idContainer).remove(token)
            there was no(tokenAccessor).delete(Results.Ok)
        }

    }

}
