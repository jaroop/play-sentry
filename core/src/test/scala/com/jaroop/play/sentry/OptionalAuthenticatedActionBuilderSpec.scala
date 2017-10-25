package test

import com.jaroop.play.sentry._
import org.mockito.Matchers._
import org.specs2.concurrent._
import org.specs2.mock._
import org.specs2.mutable._
import play.api.mvc._
import scala.concurrent.Future
import play.api.test.Helpers._

class OptionalAuthenticatedActionBuilderSpec(implicit ee: ExecutionEnv) extends Specification with Mockito {

    "OptionalAuthenticatedActionBuilder" should {

        val block: OptionalAuthRequest[AnyContent, User] => Future[Result] = { request =>
            Future.successful(Results.Ok(request.user.map(_.email).getOrElse("No user")))
        }

        tag("invokeBlock")
        "invoke the block with a user in the request if the user is authenticated" in {
            val auth = mock[AsyncAuth[TestEnv]]
            val action = new OptionalAuthenticatedActionBuilder[TestEnv](
                auth
            )
            auth.restoreUser(anyObject, anyObject).returns(Future.successful((Option(User.test), identity _)))
            contentAsString(action.invokeBlock(mock[Request[AnyContent]], block)) must equalTo(User.test.email)
        }

        tag("invokeBlock")
        "invoke the block without a user if the user is not authenticated" in {
            val auth = mock[AsyncAuth[TestEnv]]
            val action = new OptionalAuthenticatedActionBuilder[TestEnv](
                auth
            )
            val request = mock[Request[AnyContent]]
            auth.restoreUser(anyObject, anyObject).returns(Future.successful((Option.empty[User], identity _)))
            contentAsString(action.invokeBlock(request, block)) must equalTo("No user")
        }

        tag("invokeBlock")
        "invoke the block without a user if restoring the user failed for some other reason" in {
            val auth = mock[AsyncAuth[TestEnv]]
            val action = new OptionalAuthenticatedActionBuilder[TestEnv](
                auth
            )
            val request = mock[Request[AnyContent]]
            auth.restoreUser(anyObject, anyObject).returns(Future.failed(new Exception))
            contentAsString(action.invokeBlock(request, block)) must equalTo("No user")
        }

    }

}
