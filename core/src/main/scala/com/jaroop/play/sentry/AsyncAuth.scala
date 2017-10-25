package com.jaroop.play.sentry

import javax.inject.Inject
import play.api.Environment
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}

/**
 *  Provides the ability to asychronously authenticate and authorize a logged-in user based on the contents of a request.
 *  There should be little need to use this class directly, as its main purpose is to share code between the action builders.
 *
 *  @param config Requires an [[AuthConfig]] for authorization and user resolution.
 *  @param idContainer Requires an [[IdContainer]] to validate the user's session and prolong it, if successful.
 *  @param tokenAccessor Requires a [[TokenAccessor]] to retrieve an [[AuthenticityToken]] from the request,
 *                       or to set a new one when prolonging the session timeout.
 *
 *  @tparam E The environment type.
 */
class AsyncAuth[E <: Env] @Inject() (
    config: AuthConfig[E],
    idContainer: IdContainer[E#Id],
    tokenAccessor: TokenAccessor,
    env: Environment
) {

    /**
     *  Restores a user from a request, and determines whether or not that user is authorized to perform an action identified
     *  with a specific authority key.
     *
     *  @param authority The authority key with which to determine whether or not the user is authorized.
     *  @param request The request used to identify a user by their cookies.
     *  @return If the user is authorized for the given authority key, the logged-in user is returned tupled with a function
     *          that will update a result with the user's new cookie (within an `ActionBuilder`).
     *          If the user is not authorized, then the `Result` from [[AuthConfig#authorizationFailed]] will be returned.
     */
    def authorized(authority: E#Authority)
        (implicit request: RequestHeader, ec: ExecutionContext): Future[Either[Result, (E#User, ResultUpdater)]] = {
        restoreUser collect {
            case (Some(user), resultUpdater) => Right(user -> resultUpdater)
        } recoverWith {
            case _ => config.authenticationFailed(request).map(Left.apply)
        } flatMap {
            case Right((user, resultUpdater)) => config.authorize(user, authority) collect {
                case true => Right(user -> resultUpdater)
            } recoverWith {
                case _ => config.authorizationFailed(request, user, Some(authority)).map(Left.apply)
            }
            case Left(result) => Future.successful(Left(result))
        }
    }

    /**
     *  Authenticates a user from a request and prolongs their session if successful.
     *
     *  @param request The request used to identify a user by their cookies.
     *  @return An optional user (`Some` if authentication is successful) tupled with a function that will update a result with
     *          the user's new cookie (if they are actually logged-in, otherwise it does nothing).
     */
    def restoreUser(implicit request: RequestHeader, ec: ExecutionContext): Future[(Option[E#User], ResultUpdater)] = {
        (for {
            token  <- extractToken(request)
        } yield for {
            Some(userId) <- idContainer.get(token)
            Some(user) <- config.resolveUser(userId)
            _ <- idContainer.prolongTimeout(token, config.sessionTimeout)
        } yield {
            Option(user) -> tokenAccessor.put(token) _
        }) getOrElse {
            Future.successful(Option.empty -> identity)
        }
    }

    private def extractToken(request: RequestHeader): Option[AuthenticityToken] = {
        if(env.mode == play.api.Mode.Test)
            request.headers.get("SENTRY_TEST_TOKEN")
        else
            tokenAccessor.extract(request)
    }

}
