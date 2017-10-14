package com.jaroop.play.sentry

import java.security.SecureRandom
import javax.inject.Inject
import play.api.cache.CacheApi
import scala.util.Random
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scala.util.control.NonFatal

/**
 *  An [[IdContainer]] implementation that stores session data in a cache with [[play.api.cache.AsyncCacheApi AsyncCacheApi]].
 *  For each session, two values are stored in the cache. One has a key that is an [[AuthenticityToken]] with a value of the user
 *  ID that has been issued that token (in order to verify the validity of a session by a token). And the other has a key that is
 *  a user ID and value of the [[AuthenticityToken]] that user has been granted (in order to destroy sessions by user ID).
 *
 *  @param cache A [[play.api.cache.AsyncCacheApi AsyncCacheApi]] that will be used to cache session data.
 *  @param tokenGenerator A random token generator.
 *
 *  @tparam Id The type of user's ID in an application.
 */
class CacheIdContainer[Id : ClassTag] @Inject() (
    cache: AsyncCacheApi,
    tokenGenerator: TokenGenerator
) extends IdContainer[Id] {

    private val tokenSuffix = ":token"
    private val userIdSuffix = ":userId"
    private val random = new Random(new SecureRandom())

    /** @inheritdoc */
    def startNewSession(userId: Id, timeout: Duration)(implicit ec: ExecutionContext): Future[AuthenticityToken] = {
        for {
            _ <- removeByUserId(userId)
            token <- generate
            _ <- store(token, userId, timeout)
        } yield token
    }

    /** Generates a unique [[AuthenticityToken]]. */
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

    /** @inheritdoc */
    def remove(token: AuthenticityToken)(implicit ec: ExecutionContext): Future[Unit] = {
        for {
            userId <- get(token)
            _ <- unsetUserId(userId)
            _ <- unsetToken(token)
        } yield ()
    }

    /** Removes an [[AuthenticityToken]] from the cache. */
    private def unsetToken(token: AuthenticityToken)(implicit ec: ExecutionContext): Future[Unit] = {
        cache.remove(token + tokenSuffix)
    }

    /** Convenience method for removing an [[AuthenticityToken]] if the `Option` has a value, but is successful if empty. */
    private def unsetToken(token: Option[AuthenticityToken])(implicit ec: ExecutionContext): Future[Unit] =
        token.map(unsetToken).getOrElse(Future.successful(()))

    /** Removes a user ID from the cache. */
    private def unsetUserId(userId: Id)(implicit ec: ExecutionContext): Future[Unit] =
        cache.remove(userId.toString + userIdSuffix)

    /** Convenience method for removing a user ID if the `Option` has a value, but is successful if empty. */
    private def unsetUserId(userId: Option[Id])(implicit ec: ExecutionContext): Future[Unit] =
        userId.map(unsetUserId).getOrElse(Future.successful(()))

    /** @inheritdoc */
    def get(token: AuthenticityToken)(implicit ec: ExecutionContext): Future[Option[Id]] = cache.get(token + tokenSuffix)

    /** Stores both the user ID and [[AuthenticityToken]] in the cache with two entries, user ID -> token and token -> user ID. */
    private def store(token: AuthenticityToken, userId: Id, timeout: Duration)(implicit ec: ExecutionContext): Future[Unit] = {
        for {
            _ <- cache.set(token + tokenSuffix, userId, timeout)
            _ <- cache.set(userId.toString + userIdSuffix, token, timeout)
        } yield ()
    }

    /** @inheritdoc */
    def prolongTimeout(token: AuthenticityToken, timeout: Duration)(implicit ec: ExecutionContext): Future[Unit] = {
        for {
            Some(userId) <- get(token)
            _ <- store(token, userId, timeout)
        } yield ()
    }

}
