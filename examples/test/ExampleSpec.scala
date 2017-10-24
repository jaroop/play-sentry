package test

import com.jaroop.play.sentry._
import com.jaroop.play.sentry.test._
import controllers._
import models._
import org.specs2.mock._
import org.specs2.mutable._
import play.api.test._, Helpers._
import scala.concurrent.ExecutionContext.Implicits.global

class ExampleSpec extends Specification with Mockito {

    "The Examples" should {

        // Use the old withLoggedIn for tests
        "return the index with the logged-in user printed out" in new WithApplication {
            val user = User(Option(1L), "test@jaroop.com")
            val request = FakeRequest(GET, "/").withLoggedIn[EnvImpl](1L)
            val Some(result) = route(app, request)
            status(result) must equalTo(OK)
            contentAsString(result) must contain(s"You are logged in as: $user")
        }

        "return the index with a message stating the user is not logged-in" in new WithApplication {
            val request = FakeRequest(GET, "/")
            val Some(result) = route(app, request)
            status(result) must equalTo(OK)
            contentAsString(result) must contain(s"You are not logged in")
        }

        // Use mocked action builders that return a static user, instead (no application required)
        "display the user on a page where authentication is required" in {
            val user = User(Option(1L), "test@jaroop.com")
            val request = FakeRequest(GET, "/priv")
            val controller = new HomeController(
                MockAuthenticatedActionBuilder[EnvImpl](user),
                MockOptionalAuthenticatedActionBuilder[EnvImpl](None),
                mock[Login[EnvImpl]],
                mock[Logout[EnvImpl]],
                mock[UserService]
            )
            val result = controller.priv(request)
            status(result) must equalTo(OK)
            contentAsString(result) must contain(s"Success! You are logged in as $user")
        }

        "display the user on a page where authorization is required" in {
            val user = User(Option(1L), "test@jaroop.com")
            val request = FakeRequest(GET, "/priv")
            val controller = new HomeController(
                MockAuthenticatedActionBuilder[EnvImpl](user),
                MockOptionalAuthenticatedActionBuilder[EnvImpl](None),
                mock[Login[EnvImpl]],
                mock[Logout[EnvImpl]],
                mock[UserService]
            )
            val result = controller.lockedList(request)
            status(result) must equalTo(OK)
            contentAsString(result) must contain(
                s"You have the required permissions (List) to see this page, and are logged in as: $user"
            )
        }

        "return the index with the logged-in user printed out" in new WithApplication {
            val user = User(Option(1L), "test@jaroop.com")
            val optionalAuthAction = MockOptionalAuthenticatedActionBuilder[EnvImpl](Option(user))
            val request = FakeRequest(GET, "/")
            val controller = new HomeController(
                MockAuthenticatedActionBuilder[EnvImpl](user),
                optionalAuthAction,
                mock[Login[EnvImpl]],
                mock[Logout[EnvImpl]],
                mock[UserService]
            )
            val result = controller.index(request)
            status(result) must equalTo(OK)
            contentAsString(result) must contain(s"You are logged in as: $user")
        }

        "return the index with a message stating the user is not logged-in" in new WithApplication {
            val optionalAuthAction = MockOptionalAuthenticatedActionBuilder[EnvImpl](None)
            val request = FakeRequest(GET, "/")
            val controller = new HomeController(
                MockAuthenticatedActionBuilder[EnvImpl](null),
                optionalAuthAction,
                mock[Login[EnvImpl]],
                mock[Logout[EnvImpl]],
                mock[UserService]
            )
            val result = controller.index(request)
            status(result) must equalTo(OK)
            contentAsString(result) must contain(s"You are not logged in")
        }

    }

}
