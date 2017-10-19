package com.jaroop.play.sentry

import scala.util.control.Exception._
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.Duration

/**
 *  Provides a stateless implementation of [[IdContainer]], where the session is meant to be stored in cookies only. This means
 *  that the server has no way of invalidating an individual user session, and the only way to do so would be to change the
 *  secret key of the application to invalidate ''all'' signed tokens.
 *
 *  The [[AuthenticityToken]] in this case is just the user ID, so it is required that the `Id` type has an available
 *  implementation of the [[ToString]] and [[FromString]] type classes. [[scala.Int]], [[scala.Long]], and [[java.lang.String]]
 *  are supported by default to cover most use-cases.
 *
 *  @tparam A The type of user ID that will be stored in the session.
 */
class TransparentIdContainer[A : ToString : FromString] extends IdContainer[A] {

    /** @inheritdoc */
    def startNewSession(userId: A, timeout: Duration)(implicit ec: ExecutionContext): Future[AuthenticityToken] =
        Future.successful(implicitly[ToString[A]].apply(userId))

    /** Does nothing, as the session is not stored server-side. */
    def remove(token: AuthenticityToken)(implicit ec: ExecutionContext): Future[Unit] = Future.successful(())

    /** @inheritdoc */
    def get(token: AuthenticityToken)(implicit ec: ExecutionContext): Future[Option[A]] =
        Future.successful(implicitly[FromString[A]].apply(token))

    /** Does nothing, as the session is not stored server-side. */
    def prolongTimeout(token: AuthenticityToken, timeout: Duration)(implicit ec: ExecutionContext): Future[Unit] =
        Future.successful(())

}

trait ToString[A] {
    def apply(id: A): String
}

object ToString {
    def apply[A](f: A => String) = new ToString[A] {
        def apply(id: A) = f(id)
    }
    implicit val string = ToString[String](identity)
    implicit val int = ToString[Int](_.toString)
    implicit val long = ToString[Long](_.toString)
}

trait FromString[A] {
    def apply(id: String): Option[A]
}

object FromString {
    def apply[A](f: String => A) = new FromString[A] {
        def apply(id: String) = allCatch opt f(id)
    }
    implicit val string = FromString[String](identity)
    implicit val int = FromString[Int](_.toInt)
    implicit val long = FromString[Long](_.toLong)
}
