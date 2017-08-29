package com.jaroop.play.sentry

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.Duration

trait IdContainer[Id] {

    def startNewSession(userId: Id, timeout: Duration)(implicit ec: ExecutionContext): Future[AuthenticityToken]

    def remove(token: AuthenticityToken)(implicit ec: ExecutionContext): Future[Unit]

    def get(token: AuthenticityToken)(implicit ec: ExecutionContext): Future[Option[Id]]

    def prolongTimeout(token: AuthenticityToken, timeout: Duration)(implicit ec: ExecutionContext): Future[Unit]

}
