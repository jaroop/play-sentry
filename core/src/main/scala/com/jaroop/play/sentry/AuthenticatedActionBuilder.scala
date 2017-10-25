package com.jaroop.play.sentry

import javax.inject.Inject
import play.api.mvc._
import scala.concurrent.{ ExecutionContext, Future }

/**
 *  An authenticated request. It contains both the underlying Play request, and the logged-in user.
 *
 *  @param request The underlying request.
 *  @param user The logged-in user that made the request.
 *
 *  @tparam A The type of the request body.
 *  @tparam User The type of the user.
 */
class AuthRequest[A, User](request: Request[A], val user: User) extends WrappedRequest[A](request)

/**
 *  An `ActionBuilder` for endpoints where authentication or authorization is required.
 *
 *  This is one of the main components that will be used in an application. To use, you can simply just inject this component
 *  into your controller with a specified [[Env]] type.
 *
 *  {{{
 *      @Singleton
 *      class HomeController @Inject() (
 *          action: AuthenticatedActionBuilder[EnvImpl]
 *      ) extends InjectedController {
 *          def index = action { request =>
 *              Ok(s"You are logged-in as ${request.user}!")
 *          }
 *      }
 *  }}}
 *
 *  @param parser Uses the default `BodyParser`, but can be overridden with the `ActionBuilder` interface.
 *  @param config Requires an [[AuthConfig]] for success and failure behavior for authentication.
 *  @param auth Requires the [[AsyncAuth]] component for resolving a user and authorizing.
 *
 *  @tparam E The environment type of your application.
 */
class AuthenticatedActionBuilder[E <: Env] @Inject() (
    config: AuthConfig[E],
    auth: AsyncAuth[E]
)(override implicit val executionContext: ExecutionContext) extends ActionBuilder[AuthRequest[?, E#User]] { self =>

    /**
     *  Creates an `ActionBuilder` with enforced authorization. First, it verifies that the user is authenticated, then it checks
     *  whether or not the user is authorized against the given authority key.
     *
     *  @param authority The authority key the user should be authorized against.
     *  @return An `ActionBuilder` whose invoke block will only be executed if the user is authenticated and authorized for
     *          for the given authority key. If the user is not authorized, then they receive the `Result` as configured
     *          by the available [[AuthConfig]].
     */
    final def withAuthorization(authority: E#Authority): ActionBuilder[AuthRequest[?, E#User]] = {
        new ActionBuilder[AuthRequest[?, E#User]] {
            override protected implicit def executionContext = self.executionContext
            override protected def composeParser[A](bodyParser: BodyParser[A]): BodyParser[A] = self.composeParser(bodyParser)
            override protected def composeAction[A](action: Action[A]): Action[A] = self.composeAction(action)

            override def invokeBlock[A](request: Request[A], block: AuthRequest[A, E#User] => Future[Result]): Future[Result] = {
                implicit val r = request
                auth.authorized(authority) flatMap {
                    case Right((user, resultUpdater)) => block(new AuthRequest(request, user)).map(resultUpdater)
                    case Left(result) => Future.successful(result)
                }
            }
        }
    }

    /**
     *  The default implementation of an `ActionBuilder` with enforced authentication. It attempts to verify that the user is
     *  logged-in based on the `Request`, and allows the action to proceed if they are. The body of the produced `Action`
     *  accepts a `AuthRequest[A, User] => Future[Result]`, which will allow the action to access the user's details,
     *  if needed.
     *
     *  @param request The incoming request from the user.
     *  @param block A function to invoke if the user is authenticated. e.g. The body of a controller method.
     *  @return If the user is authenticated, then the `Result` of the `block` function is returned, along with updated cookies
     *          to prolong their session. If the user is not authenticated, then they receive the `Result` as configured by the
     *          available [[AuthConfig]].
     */
    override def invokeBlock[A](request: Request[A], block: AuthRequest[A, E#User] => Future[Result]): Future[Result] = {
        implicit val r = request

        auth.restoreUser recover {
            case _ => None -> identity[Result] _
        } flatMap {
            case (Some(user), cookieUpdater) => block(new AuthRequest(request, user)).map(cookieUpdater)
            case (None, _) => config.authenticationFailed(request)
        }
    }

}
