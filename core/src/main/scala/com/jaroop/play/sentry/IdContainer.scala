package com.jaroop.play.sentry

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.Duration

/**
 *  An `IdContainer`'s job is to manage user sessions server-side. It is responsible for creating, destroying, validating,
 *  and prolonging existing sessions.
 *
 *  The basic workflow is:
 *
 *  1.) A user ID is passed to start a new session, and the `IdContainer` gives the user an [[AuthenticityToken]] in return.
 *  2.) The user presents the [[AuthenticityToken]] as evidence that they are authenticated, and the `IdContainer`
 *      confirms the validity of the token.
 *  3.) When presenting a valid [[AuthenticityToken]], that token's session can be prolonged.
 *  4.) When the user logs out, the `IdContainer` will no longer recognize their token as valid.
 *
 *  @tparam Id The type of the user's ID, which is a primary identifier of a session.
 */
trait IdContainer[Id] {

    /**
     *  Opens a new session for a user, and generates an [[AuthenticityToken]] that is paired with that user's ID.
     *
     *  @param userId The ID of the session the session will be created for.
     *  @param timeout How long the session will be valid before expiring in a stateful `IdContainer`.
     *  @return An [[AuthenticityToken]] to be given to the user to later verify their session is valid.
     */
    def startNewSession(userId: Id, timeout: Duration)(implicit ec: ExecutionContext): Future[AuthenticityToken]

    /**
     *  Destroys the session identified by a given token.
     *
     *  @param token The [[AuthenticityToken]] that is paired with the session to be destroyed.
     *  @return A successful `Future` if the session was destroyed, or a failed `Future` if an error occurred.
     */
    def remove(token: AuthenticityToken)(implicit ec: ExecutionContext): Future[Unit]

    /**
     *  Finds the user ID that is associated with an [[AuthenticityToken]]. If `Some[Id]` is returned, that is sufficient to
     *  show that a user's session is valid.
     *
     *  @param token The [[AuthenticityToken]] to verify.
     *  @return The user ID associated with the [[AuthenticityToken]] within the `IdContainer`, if there is one.
     *          Otherwise, `None`.
     */
    def get(token: AuthenticityToken)(implicit ec: ExecutionContext): Future[Option[Id]]

    /**
     *  Delays the expiration of a session by a given duration.
     *
     *  @param token The [[AuthenticityToken]] of the session to prolong.
     *  @param timeout The new duration of the session. e.g. if there is 30 minutes left in a session, and a timeout of one hour
     *                 is passed, the session will expire in one hour.
     *  @return A successful `Future` if the session was prolonged, or a failed `Future` if the session isn't valid, or
     *          some other error occurred.
     */
    def prolongTimeout(token: AuthenticityToken, timeout: Duration)(implicit ec: ExecutionContext): Future[Unit]

}
