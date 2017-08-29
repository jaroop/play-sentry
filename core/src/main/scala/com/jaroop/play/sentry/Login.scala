package com.jaroop.play.sentry

import javax.inject.Inject
import play.api.mvc.{ Cookie, RequestHeader, Result }
import scala.concurrent.{ ExecutionContext, Future }

class Login[E <: Env] @Inject() (
    config: AuthConfig[E],
    idContainer: IdContainer[E#Id],
    tokenAccessor: TokenAccessor
) {

    def gotoLoginSucceeded(userId: E#Id)(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
        gotoLoginSucceeded(userId, config.loginSucceeded(request))
    }

    def gotoLoginSucceeded(userId: E#Id, result: => Future[Result])
        (implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {

        for {
            token <- idContainer.startNewSession(userId, config.sessionTimeout)
            r     <- result
        } yield tokenAccessor.put(token)(r)
    }

}
