package com.jaroop.play.sentry

import scala.util.control.Exception._
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.Duration

class TransparentIdContainer[A : ToString : FromString] extends IdContainer[A] {

    def startNewSession(userId: A, timeout: Duration)(implicit ec: ExecutionContext): Future[AuthenticityToken] =
        Future.successful(implicitly[ToString[A]].apply(userId))

    def remove(token: AuthenticityToken)(implicit ec: ExecutionContext): Future[Unit] = Future.successful(())

    def get(token: AuthenticityToken)(implicit ec: ExecutionContext): Future[Option[A]] =
        Future.successful(implicitly[FromString[A]].apply(token))

    // Cookie Id Container does not support timeout.
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
