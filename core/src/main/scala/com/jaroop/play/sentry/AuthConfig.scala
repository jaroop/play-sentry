package com.jaroop.play.sentry

import play.api.mvc.{ RequestHeader, Result }
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.Duration

/**
 *  The [[AuthConfig]] defines the behavior of an application where it intersects with the authentication and authorization
 *  system. This will allow Sentry to know how to find a user in your application, how to authorize them, and where to direct
 *  them when these actions succeed or fail.
 *
 *  Most of the work involved in integrating Play Sentry into your application is implementing your own [[AuthConfig]]. Your own
 *  [[AuthConfig]] should be a class that extends this type, fixes the `Env` type, and implements all of the methods to
 *  customize it to your application's desired behavior.
 *
 *  @tparam E The environment type of your application.
 */
trait AuthConfig[E <: Env] {

    /**
     *  Defines the maximum lifespan of a session. Each session's timeout is reset to this value every time
     *  a request from them is successfully authentiated.
     */
    def sessionTimeout: Duration

    /**
     *  Resolves a user by ID. Implement this method to connect the user type from your own application.
     *
     *  @param id The ID of the user to find.
     *  @return The user, if found, otherwise `None`.
     */
    def resolveUser(id: E#Id)(implicit context: ExecutionContext): Future[Option[E#User]]

    /**
     *  Determines where to redirect the user by default after successfully logging in. Implement this method to specify
     *  where to direct a user after `Login#apply` is called.
     *
     *  @param request The original request used to authenticate.
     *  @return A `Result` typically directing the user to a default URL to be seen after logging in, which will have
     *          additional headers applied to set cookies on top of the provided `Result`.
     */
    def loginSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[Result]

    /**
     *  Determines where to redirect the user by default after logging out. Implement this method to specify where to direct
     *  a user after `Logout#apply` is called.
     *
     *  @param request The request that initiated the logout action.
     *  @return A `Result` typically directing the user to a default URL to be seen after logging out, which will additionally
     *          contain headers to discard any Play Sentry cookies on top of the provided `Result`.
     */
    def logoutSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[Result]

    /**
     *  Called when a user attempts to access an action that requires authentication and they are not properly authenticated.
     *  Implement this method to specify what happens when a user is not logged in. For example, return 403 Forbidden,
     *  or redirect them to a login page.
     *
     *  @param request The unauthenticated request.
     *  @return The `Result` you would like to return to the user when they are unauthenticated.
     */
    def authenticationFailed(request: RequestHeader)(implicit context: ExecutionContext): Future[Result]

    /**
     *  Called when a user attempts to access an action that requires authorization, but they are not authorized via
     *  [[AuthConfig#authorize]]. Implement this method to specify what happens when a user is not authorized to access
     *  a resource. For example, return 403 Forbidden.
     *
     *  @param request The unauthorized request.
     *  @param user The user that initiated the unauthorized request.
     *  @param authority The authority key the user was denied from accessing.
     *  @return The `Result` you would like to return to the user when they are unauthorized.
     */
    def authorizationFailed(request: RequestHeader, user: E#User, authority: Option[E#Authority])
        (implicit context: ExecutionContext): Future[Result]

    /**
     *  Determines whether or not a user is authorized to perform a certain action by authority key. Implement this method
     *  to connect your own authorization scheme from your application.
     *
     *  @param user The user requesting authorization to perform an action.
     *  @param authority The authority key associated with the action.
     *  @return True if the user is authorized, which will allow the action to proceed. Otherwise false, and the user will be
     *          denied access and informed via [[AuthConfig#authorizationFailed]].
     */
    def authorize(user: E#User, authority: E#Authority)(implicit context: ExecutionContext): Future[Boolean]

}
