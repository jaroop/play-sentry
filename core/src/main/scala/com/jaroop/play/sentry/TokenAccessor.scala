package com.jaroop.play.sentry

import play.api.mvc.{ RequestHeader, Result }
import play.api.libs.crypto.CookieSigner

/**
 *  Provides an interface for managing sessions client-side via requests and results. A [[TokenAccessor]] should be able to add
 *  or remove a [[SignedToken]] from a `Result`, as well as verify the signature of a [[SignedToken]] from a `RequestHeader`.
 *
 *  While it is not required to use them, token accessors should use the available signing methods in this trait to sign
 *  and verify tokens so that they cannot be tampered with by an attacker.
 */
trait TokenAccessor {

    /** Requires a `CookieSigner` to sign tokens and verify token signatures. */
    def signer: CookieSigner

    /**
     *  Attempts to extract an [[AuthenticityToken]] from a `RequestHeader`.
     *
     *  @param request The `RequestHeader` to extract the token from.
     *  @return An [[AuthenticityToken]] if the request contains a token with a valid signature. Otherwise, `None`.
     */
    def extract(request: RequestHeader): Option[AuthenticityToken]

    /**
     *  Puts an [[AuthenticityToken]] into a `Result` to return to a user.
     *
     *  @param token The [[AuthenticityToken]] to be issued to a user.
     *  @return A `Result` containing a [[SignedToken]] or [[AuthenticityToken]].
     */
    def put(token: AuthenticityToken)(result: Result)(implicit request: RequestHeader): Result

    /**
     *  Removes any issued [[AuthenticityToken]] or [[SignedToken]] from a `Result`.
     *
     *  @param result The `Result` to remove all issued tokens from.
     *  @return A new `Result` without any issued tokens.
     */
    def delete(result: Result)(implicit request: RequestHeader): Result

    /**
     *  Verifies that a [[SignedToken]] token is valid by comparing the stored signature in the [[SignedToken]] to the
     *  signature of the raw [[AuthenticityToken]] that is part of the [[SignedToken]]. In order for ''any'' [[SignedToken]]
     *  to be valid, it must be issued via the [[TokenAccessor#sign]] method.
     *
     *  @param token The [[SignedToken]] to validate.
     *  @return The contained [[AuthenticityToken]] if the signature is valid, otherwise `None`.
     */
    protected def verifyHmac(token: SignedToken): Option[AuthenticityToken] = {
        val (hmac, value) = token.splitAt(40)
        if (safeEquals(signer.sign(value), hmac)) Some(value) else None
    }

    /**
     *  Signs an [[AuthenticityToken]] and concatenates it with its signature. ("$${signature}$${token}") The resulting
     *  [[SignedToken]] is meant to be issued to a user (e.g., within a cookie).
     *
     *  @param token The [[AuthenticityToken]] to sign.
     *  @return The signature of the [[AuthenticityToken]] concatenated with the token itself.
     */
    protected def sign(token: AuthenticityToken): SignedToken = signer.sign(token) + token

    // Do not change this unless you understand the security issues behind timing attacks.
    // This method intentionally runs in constant time if the two strings have the same length.
    // If it didn't, it would be vulnerable to a timing attack.
    protected def safeEquals(a: String, b: String) = {
        if (a.length != b.length) {
            false
        } else {
            var equal = 0
            for (i <- Array.range(0, a.length)) {
                equal |= a(i) ^ b(i)
            }
            equal == 0
        }
    }

}
