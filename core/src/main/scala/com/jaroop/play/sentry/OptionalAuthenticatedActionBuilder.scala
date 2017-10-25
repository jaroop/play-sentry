package com.jaroop.play.sentry

import javax.inject.Inject
import play.api.mvc._
import scala.concurrent.{ ExecutionContext, Future }

/**
 *  An optionally authenticated request. It contains both the underlying Play request, and an optional logged-in user.
 *  (If the user is authenticated.)
 *
 *  @param request The underlying request.
 *  @param user The logged-in user that made the request, if they are authenticated. Otherwise, `None`.
 *
 *  @tparam A The type of the request body.
 *  @tparam User The type of the user.
 */
class OptionalAuthRequest[A, User](request: Request[A], val user: Option[User]) extends WrappedRequest[A](request)

/**
 *  An `ActionBuilder` that can be used for actions where authentication is optional. The endpoints that use this action
 *  will be publically accessible, but you may alter the behavior depending on whether or not a user is present.
 *
 *  To use, you can simply just inject this component into your controller with a specified [[Env]] type.
 *
 *  {{{
 *      @Singleton
 *      class HomeController @Inject() (
 *          action: OptionalAuthenticatedActionBuilder[EnvImpl]
 *      ) extends InjectedController {
 *          def index = action { request =>
 *              request.user match {
 *                  case Some(user) => Ok(s"You are logged-in as ${user}!")
 *                  case None => Ok("You are not logged-in, but you can view this page, anyway.")
 *              }
 *          }
 *      }
 *  }}}
 *
 *  @param parser Uses the default `BodyParser`, but can be overridden with the `ActionBuilder` interface.
 *  @param auth Requires the [[AsyncAuth]] component for resolving a user.
 *
 *  @tparam E The environment type of your application.
 */
class OptionalAuthenticatedActionBuilder[E <: Env] @Inject() (
    auth: AsyncAuth[E]
)(override implicit val executionContext: ExecutionContext) extends ActionBuilder[OptionalAuthRequest[?, E#User]] {

    /**
     *  Attempts to verify if the user is authenticated before invoking the `block` function.
     *
     *  @param request The incoming request from the user.
     *  @param block A function to invoke. e.g. The body of a controller method.
     *  @return Returns the result of the `block` function, with updated cookies if the user is authenticated.
     */
    override def invokeBlock[A](request: Request[A], block: OptionalAuthRequest[A, E#User] => Future[Result]) = {
        implicit val r = request
        val maybeUserFuture = auth.restoreUser.recover { case _ => None -> identity[Result] _ }
        maybeUserFuture.flatMap { case (maybeUser, cookieUpdater) =>
            block(new OptionalAuthRequest(request, maybeUser)).map(cookieUpdater)
        }
    }

}
