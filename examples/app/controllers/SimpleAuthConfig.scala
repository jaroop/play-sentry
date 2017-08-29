package controllers

import com.jaroop.play.sentry._
import javax.inject._
import models.{ Permission, PermissionService, User, UserService }
import play.api.mvc._, Results._
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.reflect._

class SimpleAuthConfig @Inject() (ps: PermissionService, us: UserService) extends AuthConfig[EnvImpl] {

    def sessionTimeout = 120.seconds

    def resolveUser(id: Long)(implicit context: ExecutionContext): Future[Option[User]] = Future.successful(us.read(id))

    def loginSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[Result] =
        Future.successful(Redirect("/"))

    def logoutSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[Result] =
        Future.successful(Redirect("/"))

    def authenticationFailed(request: RequestHeader)(implicit context: ExecutionContext): Future[Result] =
        Future.successful(Forbidden("You are not logged in."))

    def authorizationFailed(request: RequestHeader, user: User, authority: Option[Permission])
        (implicit context: ExecutionContext): Future[Result] =
        Future.successful(Forbidden("Unauthorized."))

    def authorize(user: User, authority: Permission)(implicit context: ExecutionContext): Future[Boolean] =
        Future.successful(ps.isAuthorized(user, authority))

}

trait EnvImpl extends Env {
    type Id = Long
    type User = models.User
    type Authority = Permission
}
