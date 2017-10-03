package test

import com.jaroop.play.sentry._
import org.mockito.Matchers._
import org.specs2.concurrent._
import org.specs2.mock._
import org.specs2.mutable._
import play.api.mvc._
import scala.concurrent.Future
import scala.concurrent.duration._

class AuthenticatedActionBuilderSpec(implicit ee: ExecutionEnv) extends Specification with Mockito {

    "AuthenticatedActionBuilder" should {

        tag("invokeBlock")
        "invoke the block when the user is authenticated" in {
            val auth = mock[AsyncAuth[TestEnv]]
            val action = new AuthenticatedActionBuilder[TestEnv](
                mock[BodyParsers.Default],
                mock[AuthConfig[TestEnv]],
                auth
            )
            auth.restoreUser(anyObject, anyObject).returns(Future.successful((Option(User.test), identity _)))
            val result: Result = Results.Ok
            val block: Request[AnyContent] => Future[Result] = { request =>
                Future.successful(result)
            }
            action.invokeBlock(mock[Request[AnyContent]], block) must equalTo(result).await
        }

        tag("invokeBlock")
        "not invoke the block when the user is not authenticated" in {
            val auth = mock[AsyncAuth[TestEnv]]
            val config = mock[AuthConfig[TestEnv]]
            val action = new AuthenticatedActionBuilder[TestEnv](
                mock[BodyParsers.Default],
                config,
                auth
            )
            val request = mock[Request[AnyContent]]
            auth.restoreUser(anyObject, anyObject).returns(Future.successful((Option.empty[User], identity _)))
            val result: Result = Results.Forbidden
            config.authenticationFailed(anyObject)(anyObject).returns(Future.successful(result))
            val block: Request[AnyContent] => Future[Result] = { request =>
                Future.successful(Results.Ok)
            }
            action.invokeBlock(request, block) must equalTo(result).await
        }

        tag("invokeBlock")
        "not invoke the block when there is an error restoring the user" in {
            val auth = mock[AsyncAuth[TestEnv]]
            val config = mock[AuthConfig[TestEnv]]
            val action = new AuthenticatedActionBuilder[TestEnv](
                mock[BodyParsers.Default],
                config,
                auth
            )
            val request = mock[Request[AnyContent]]
            auth.restoreUser(anyObject, anyObject).returns(Future.failed(new Exception))
            val result: Result = Results.Forbidden
            config.authenticationFailed(anyObject)(anyObject).returns(Future.successful(result))
            val block: Request[AnyContent] => Future[Result] = { request =>
                Future.successful(Results.Ok)
            }
            action.invokeBlock(request, block) must equalTo(result).await
        }

        tag("withAuthorization")
        "invoke the block when the user is authenticated and authorized" in {
            val auth = mock[AsyncAuth[TestEnv]]
            val action = new AuthenticatedActionBuilder[TestEnv](
                mock[BodyParsers.Default],
                mock[AuthConfig[TestEnv]],
                auth
            )
            auth.authorized(anyObject)(anyObject, anyObject).returns(Future.successful(Right((User.test, identity[Result] _))))
            val result: Result = Results.Ok
            val block: Request[AnyContent] => Future[Result] = { request =>
                Future.successful(result)
            }
            action.withAuthorization(Admin).invokeBlock(mock[Request[AnyContent]], block) must equalTo(result).await
        }

        tag("withAuthorization")
        "not invoke the block whent he user is authenticated but not authorized" in {
            val auth = mock[AsyncAuth[TestEnv]]
            val action = new AuthenticatedActionBuilder[TestEnv](
                mock[BodyParsers.Default],
                mock[AuthConfig[TestEnv]],
                auth
            )
            val result: Result = Results.Forbidden
            auth.authorized(anyObject)(anyObject, anyObject).returns(Future.successful(Left(result)))
            val block: Request[AnyContent] => Future[Result] = { request =>
                Future.successful(Results.Ok)
            }
            action.withAuthorization(Admin).invokeBlock(mock[Request[AnyContent]], block) must equalTo(result).await
        }

    }

}
