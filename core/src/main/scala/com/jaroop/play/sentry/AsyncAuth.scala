package com.jaroop.play.sentry

import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}

trait AsyncAuth[E <: Env] {

    def config: AuthConfig[E]

    def idContainer: IdContainer[E#Id]

    def tokenAccessor: TokenAccessor

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

    private def extractToken(request: RequestHeader): Option[AuthenticityToken] = tokenAccessor.extract(request)

}
