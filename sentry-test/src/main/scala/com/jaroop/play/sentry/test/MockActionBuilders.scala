package com.jaroop.play.sentry.test

import com.jaroop.play.sentry._
import play.api.mvc._
import scala.concurrent.{ ExecutionContext, Future }

/**
 *  A fake [[AuthenticatedActionBuilder]] that will always invoke the given action block, and use the provided logged-in user.
 *  [[MockAuthenticatedActionBuilder#withAuthorization]] will always authorize the user and invoke the given block, as well.
 *
 *  @param user The user that will be logged-in for the test.
 */
case class MockAuthenticatedActionBuilder[E <: Env] (val user: E#User)(implicit ec: ExecutionContext)
    extends AuthenticatedActionBuilder[E](null, null, null)(ec) { self =>

    override final def withAuthorization(authority: E#Authority): ActionBuilder[AuthRequest[?, E#User], AnyContent] = this

    override def invokeBlock[A](request: Request[A], block: AuthRequest[A, E#User] => Future[Result]): Future[Result] =
        block(new AuthRequest(request, user))

}

/**
 *  A fake [[OptionalAuthenticatedActionBuilder]] that will always invoke the given block, and will use the given
 *  optional user within the request.
 *
 *  @param user The optional user that will be included in each request that invokes this action.
 */
case class MockOptionalAuthenticatedActionBuilder[E <: Env] (val user: Option[E#User])(implicit val rc: ExecutionContext)
    extends OptionalAuthenticatedActionBuilder[E](null, null) {

    override def invokeBlock[A](request: Request[A], block: OptionalAuthRequest[A, E#User] => Future[Result]) =
        block(new OptionalAuthRequest(request, user))

}
