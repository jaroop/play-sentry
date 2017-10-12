package com.jaroop.play.sentry

import javax.inject.Inject
import play.api.mvc.{ Cookie, RequestHeader, Result }
import scala.concurrent.{ ExecutionContext, Future }

/**
 *  The `Logout` component provides the ability for a controller to destroy a logged-in user's session.
 *
 *  To use it, you simply inject `Logout` into your controller, and call `gotoLogoutSucceeded()` when you want the logged-in
 *  user to be logged-out.
 *
 *  {{{
 *      @Singleton
 *      class Application @Inject() (logout: Logout[MyEnv]) extends InjectedController {
 *          def logout = Action.async { implicit request =>
 *              gotoLogoutSucceeded()
 *          }
 *      }
 *  }}}
 *
 *  @param config Requires an [[AuthConfig]] to determine the default `Result` to return to the user once logged-out.
 *  @param idContainer The [[IdContainer]] that will forget the session server-side.
 *  @param tokenAccessor The [[TokenAccessor]] that will forget the session client-side.
 *
 *  @tparam E The environment type of your application.
 */
class Logout[E <: Env] @Inject() (
    config: AuthConfig[E],
    idContainer: IdContainer[E#Id],
    tokenAccessor: TokenAccessor
) {

    /**
     *  Destroys the session of the logged-in user that made a request, if any.
     *
     *  @return The default `Result` defined in the [[AuthConfig]] when the user is logged-out, with a header to discard
     *          any Play Sentry cookies.
     */
    def gotoLogoutSucceeded(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
        gotoLogoutSucceeded(config.logoutSucceeded(request))
    }

    /**
     *  Destroys the session of the logged-in user that made a request, if any, and returns the given `Result` regardless of
     *  success or failure.
     *
     *  @param result The `Result` to return the user once they are logged-out.
     *  @return The given `Result` with a header to discard any Play Sentry cookies.
     */
    def gotoLogoutSucceeded(result: => Future[Result])(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
        tokenAccessor.extract(request) foreach idContainer.remove
        result.map(tokenAccessor.delete)
    }

}
