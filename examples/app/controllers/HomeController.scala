package controllers

import com.jaroop.play.sentry._
import models._
import javax.inject._
import play.api._
import play.api.mvc._, Results._
import scala.concurrent._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject() (
    authenticatedAction: AuthenticatedActionBuilder[EnvImpl],
    optionalAuthAction: OptionalAuthenticatedActionBuilder[EnvImpl],
    loginService: Login[EnvImpl],
    logoutService: Logout[EnvImpl],
    us: UserService
)(implicit val ec: ExecutionContext) extends Controller {

    def index = optionalAuthAction { implicit request =>
        val msg = request.user.map(user => s"You are logged in as: $user").getOrElse("You are not logged in.")
        Ok(views.html.index(s"Authentication on this page is optional: $msg"))
    }

    def login(id: Long) = Action.async { implicit request =>
        us.read(id).map(user => loginService.gotoLoginSucceeded(id))
            .getOrElse(Future.successful(NotFound("Invalid credentials.")))
    }

    def logout() = Action.async { implicit request =>
        logoutService.gotoLogoutSucceeded(Future.successful(Redirect("/")))
    }

    def priv = authenticatedAction { implicit request =>
        Ok(views.html.index(s"Success! You are logged in as ${request.user}"))
    }

    def lockedView = authenticatedAction.withAuthorization(View) { implicit request =>
        Ok(views.html.index(s"You have the required permissions (View) to see this page, and are logged in as: ${request.user}"))
    }

    def lockedList = authenticatedAction.withAuthorization(List) { implicit request =>
        Ok(views.html.index(s"You have the required permissions (List) to see this page, and are logged in as: ${request.user}"))
    }

}
