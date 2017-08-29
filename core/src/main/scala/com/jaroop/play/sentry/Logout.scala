package com.jaroop.play.sentry

import javax.inject.Inject
import play.api.mvc.{ Cookie, RequestHeader, Result }
import scala.concurrent.{ ExecutionContext, Future }

class Logout[E <: Env] @Inject() (
    config: AuthConfig[E],
    idContainer: IdContainer[E#Id],
    tokenAccessor: TokenAccessor
) {

    def gotoLogoutSucceeded(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
        gotoLogoutSucceeded(config.logoutSucceeded(request))
    }

    def gotoLogoutSucceeded(result: => Future[Result])(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
        tokenAccessor.extract(request) foreach idContainer.remove
        result.map(tokenAccessor.delete)
    }

}
