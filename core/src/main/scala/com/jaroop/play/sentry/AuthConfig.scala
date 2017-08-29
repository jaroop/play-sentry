package com.jaroop.play.sentry

import play.api.mvc.{ RequestHeader, Result }
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.Duration

trait AuthConfig[E <: Env] {

    def sessionTimeout: Duration

    def resolveUser(id: E#Id)(implicit context: ExecutionContext): Future[Option[E#User]]

    def loginSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[Result]

    def logoutSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[Result]

    def authenticationFailed(request: RequestHeader)(implicit context: ExecutionContext): Future[Result]

    def authorizationFailed(request: RequestHeader, user: E#User, authority: Option[E#Authority])
        (implicit context: ExecutionContext): Future[Result]

    def authorize(user: E#User, authority: E#Authority)(implicit context: ExecutionContext): Future[Boolean]

}
