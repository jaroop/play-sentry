package com.jaroop.play.sentry

import java.security.SecureRandom
import javax.inject.Inject
import play.api.cache.AsyncCacheApi
import scala.util.Random
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scala.util.control.NonFatal

class CacheIdContainer[Id : ClassTag] @Inject() (
    cache: AsyncCacheApi,
    tokenGenerator: TokenGenerator
) extends IdContainer[Id] {

    private val tokenSuffix = ":token"
    private val userIdSuffix = ":userId"
    private val random = new Random(new SecureRandom())

    def startNewSession(userId: Id, timeout: Duration)(implicit ec: ExecutionContext): Future[AuthenticityToken] = {
        for {
            _ <- removeByUserId(userId)
            token <- generate
            _ <- store(token, userId, timeout)
        } yield token
    }

    private final def generate(implicit ec: ExecutionContext): Future[AuthenticityToken] = {
        val token = tokenGenerator.generate
        get(token).filter(_.isEmpty).map(_ => token).recoverWith {
            case NonFatal(_) => generate
        }
    }

    private def removeByUserId(userId: Id)(implicit ec: ExecutionContext): Future[Unit] = {
        for {
            token <- cache.get[String](userId.toString + userIdSuffix)
            _ <- unsetToken(token)
            _ <- unsetUserId(userId)
        } yield ()
    }

    def remove(token: AuthenticityToken)(implicit ec: ExecutionContext): Future[Unit] = {
        for {
            userId <- get(token)
            _ <- unsetUserId(userId)
            _ <- unsetToken(token)
        } yield ()
    }

    private def unsetToken(token: AuthenticityToken)(implicit ec: ExecutionContext): Future[Unit] = {
        cache.remove(token + tokenSuffix).map(_ => ())
    }

    private def unsetToken(token: Option[AuthenticityToken])(implicit ec: ExecutionContext): Future[Unit] =
        token.map(unsetToken).getOrElse(Future.successful(()))

    private def unsetUserId(userId: Id)(implicit ec: ExecutionContext): Future[Unit] =
        cache.remove(userId.toString + userIdSuffix).map(_ => ())

    private def unsetUserId(userId: Option[Id])(implicit ec: ExecutionContext): Future[Unit] =
        userId.map(unsetUserId).getOrElse(Future.successful(()))

    def get(token: AuthenticityToken)(implicit ec: ExecutionContext): Future[Option[Id]] = cache.get(token + tokenSuffix)

    private def store(token: AuthenticityToken, userId: Id, timeout: Duration)(implicit ec: ExecutionContext): Future[Unit] = {
        for {
            _ <- cache.set(token + tokenSuffix, userId, timeout)
            _ <- cache.set(userId.toString + userIdSuffix, token, timeout)
        } yield ()
    }

    def prolongTimeout(token: AuthenticityToken, timeout: Duration)(implicit ec: ExecutionContext): Future[Unit] = {
        for {
            Some(userId) <- get(token)
            _ <- store(token, userId, timeout)
        } yield ()
    }

}
