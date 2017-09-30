package com.jaroop.play.sentry

import javax.inject.Inject
import play.api.mvc._
import scala.concurrent.{ ExecutionContext, Future }

class OptionalAuthRequest[A, User](request: Request[A], val user: Option[User]) extends WrappedRequest[A](request)

class OptionalAuthenticatedActionBuilder[E <: Env] @Inject() (
    val parser: BodyParsers.Default,
    auth: AsyncAuth[E]
)(implicit val executionContext: ExecutionContext) extends ActionBuilder[OptionalAuthRequest[?, E#User], AnyContent] {

    override def invokeBlock[A](request: Request[A], block: OptionalAuthRequest[A, E#User] => Future[Result]) = {
        implicit val r = request
        val maybeUserFuture = auth.restoreUser.recover { case _ => None -> identity[Result] _ }
        maybeUserFuture.flatMap { case (maybeUser, cookieUpdater) =>
            block(new OptionalAuthRequest(request, maybeUser)).map(cookieUpdater)
        }
    }

}
