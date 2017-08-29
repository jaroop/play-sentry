package com.jaroop.play.sentry

import javax.inject.Inject
import play.api.mvc._
import scala.concurrent.{ ExecutionContext, Future }

class AuthRequest[A, User](request: Request[A], val user: User) extends WrappedRequest[A](request)

class AuthenticatedActionBuilder[E <: Env] @Inject() (
    val parser: BodyParsers.Default,
    val config: AuthConfig[E],
    val idContainer: IdContainer[E#Id],
    val tokenAccessor: TokenAccessor
)(implicit val executionContext: ExecutionContext) extends ActionBuilder[AuthRequest[?, E#User], AnyContent]
    with AsyncAuth[E] { self =>

    final def withAuthorization(authority: E#Authority): ActionBuilder[AuthRequest[?, E#User], AnyContent] = {
        new ActionBuilder[AuthRequest[?, E#User], AnyContent] {
            override def parser = self.parser
            override protected implicit def executionContext = self.executionContext
            override protected def composeParser[A](bodyParser: BodyParser[A]): BodyParser[A] = self.composeParser(bodyParser)
            override protected def composeAction[A](action: Action[A]): Action[A] = self.composeAction(action)

            override def invokeBlock[A](request: Request[A], block: AuthRequest[A, E#User] => Future[Result]) = {
                implicit val r = request
                authorized(authority) flatMap {
                    case Right((user, resultUpdater)) => block(new AuthRequest(request, user)).map(resultUpdater)
                    case Left(result) => Future.successful(result)
                }
            }
        }
    }

    override def invokeBlock[A](request: Request[A], block: AuthRequest[A, E#User] => Future[Result]) = {
        implicit val r = request

        restoreUser recover {
            case _ => None -> identity[Result] _
        } flatMap {
            case (Some(user), cookieUpdater) => block(new AuthRequest(request, user)).map(cookieUpdater)
            case (None, _) => config.authenticationFailed(request)
        }
    }

}
