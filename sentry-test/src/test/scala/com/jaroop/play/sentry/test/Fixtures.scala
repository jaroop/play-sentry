package com.jaroop.play.sentry.test

import com.google.inject.{ AbstractModule, TypeLiteral }
import com.jaroop.play.sentry._
import javax.inject.{ Inject, Provider, Singleton }
import play.api.http.HttpConfiguration
import play.api.inject._
import play.api.mvc._, Results._
import play.api.routing._
import play.api.routing.sird._
import scala.concurrent._, duration._
import scala.reflect.{ ClassTag, classTag }

trait TestEnv extends Env {
    type Id = Long
    type User = Account
    type Authority = Role
}

case class Account(id: Option[Long], email: String)

object Account {

    val test = Account(Option(1L), "test@example.com")

}

sealed trait Role
case object Admin extends Role
case object Manager extends Role
case object Employee extends Role

class TestAuthModule extends AbstractModule {

    override def configure(): Unit = {
        bind(new TypeLiteral[AuthConfig[TestEnv]]() {}).to(classOf[TestAuthConfig])
        bind(classOf[TokenAccessor]).to(classOf[CookieTokenAccessor])
        bind(new TypeLiteral[IdContainer[Long]] {}).to(new TypeLiteral[CacheIdContainer[Long]] {})
        bind(new TypeLiteral[ClassTag[Long]] {}).toInstance(classTag[Long])
    }

}

class TestController @Inject() (action: AuthenticatedActionBuilder[TestEnv]) extends InjectedController {
    def test = action { implicit request =>
        Results.Ok(s"You are ${request.user.email}")
    }
}

class TestRouter @Inject() (controller: TestController) extends SimpleRouter {
    def routes = {
        case GET(p"/test") => controller.test
    }
}

@Singleton
class ScalaRoutesProvider @Inject()(testRouter: TestRouter, httpConfig: HttpConfiguration) extends Provider[Router] {
    lazy val get = testRouter.withPrefix(httpConfig.context)
}

class TestAuthConfig extends AuthConfig[TestEnv] {

    def sessionTimeout = 120.seconds

    def resolveUser(id: Long)(implicit context: ExecutionContext): Future[Option[Account]] =
        Future.successful(Option(Account.test))

    def loginSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[Result] =
        Future.successful(Redirect("/"))

    def logoutSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[Result] =
        Future.successful(Redirect("/"))

    def authenticationFailed(request: RequestHeader)(implicit context: ExecutionContext): Future[Result] =
        Future.successful(Forbidden("You are not logged in."))

    def authorizationFailed(request: RequestHeader, user: Account, authority: Option[Role])
        (implicit context: ExecutionContext): Future[Result] =
        Future.successful(Forbidden("Unauthorized."))

    def authorize(user: Account, authority: Role)(implicit context: ExecutionContext): Future[Boolean] =
        Future.successful(true)

}
