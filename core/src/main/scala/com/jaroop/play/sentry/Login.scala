package com.jaroop.play.sentry

import javax.inject.Inject
import play.api.mvc.{ Cookie, RequestHeader, Result }
import scala.concurrent.{ ExecutionContext, Future }

/**
 *  The `Login` component provides the user's first entry point into Play Sentry via your application.
 *
 *  It is up to your application to decide when a user is to be logged-in (i.e. provides the correct credentials to a login
 *  form of some kind), and from there your controller can use this component to create a session for that user by their ID.
 *  To use it, you simply inject `Login` into your controller, and call `Login#apply(userId)` when you want to grant
 *  a session to a user linked to a particular `userId`.
 *
 *  {{{
 *      @Singleton
 *      class Application @Inject() (sentryLogin: Login[MyEnv], userService: UserService) extends InjectedController {
 *          def login = Action.async(parse.urlEncodedForm) { implicit request =>
 *              // logic to determine to handle user name and password..
 *              sentryLogin(userId)
 *          }
 *      }
 *  }}}
 *
 *  @param config Requires an [[AuthConfig]] to determine the default `Result` to return to the user once logged-in.
 *  @param idContainer The [[IdContainer]] that will store the session server-side.
 *  @param tokenAccessor The [[TokenAccessor]] that will store the session client-side.
 *
 *  @tparam E The environment type of your application.
 */
class Login[E <: Env] @Inject() (
    config: AuthConfig[E],
    idContainer: IdContainer[E#Id],
    tokenAccessor: TokenAccessor
) {

    /**
     *  Creates a new session for a user.
     *
     *  @param userId The ID of the user to grant the session to.
     *  @return The default `Result` defined in the [[AuthConfig]] when the user is logged-in, with an [[AuthenticityToken]]
     *          included.
     */
    def apply(userId: E#Id)(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
        apply(userId, config.loginSucceeded(request))
    }

    @deprecated("Use `apply` instead, `gotoLoginSucceeded` will be removed in 1.2.0", "1.1.0")
    def gotoLoginSucceeded(userId: E#Id)(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = apply(userId)

    /**
     *  Creates a new session for a user, and allows a custom result (different than from the [[AuthConfig]]) to be returned
     *  to them once the session has been created.
     *
     *  @param userId The ID of the user to grant the session to.
     *  @param result The `Result` to return to the user once their session is created.
     *  @return The given `Result` with an [[AuthenticityToken]] included.
     */
    def apply(userId: E#Id, result: => Future[Result])(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
        for {
            token <- idContainer.startNewSession(userId, config.sessionTimeout)
            r     <- result
        } yield tokenAccessor.put(token)(r)
    }

    @deprecated("Use `apply` instead, `gotoLoginSucceeded` will be removed in 1.2.0", "1.1.0")
    def gotoLoginSucceeded(userId: E#Id, result: => Future[Result])
        (implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = apply(userId, result)

}
