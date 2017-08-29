package com.jaroop.play.sentry

import javax.inject.Inject
import play.api.Configuration
import play.api.libs.crypto.CookieSigner
import play.api.mvc.{ DiscardingCookie, Cookie, Result, RequestHeader }

class CookieTokenAccessor @Inject() (config: Configuration, val signer: CookieSigner) extends TokenAccessor {

    protected val name: String = config.getOptional[String]("sentry.cookie.name").getOrElse("SENTRY_SESS_ID")

    protected val secure: Boolean = config.getOptional[Boolean]("sentry.cookie.secure").getOrElse(false)

    protected val httpOnly: Boolean = config.getOptional[Boolean]("sentry.cookie.httpOnly").getOrElse(false)

    protected val domain: Option[String] = config.getOptional[String]("sentry.cookie.domain")

    protected val path: String = config.getOptional[String]("sentry.cookie.path").getOrElse("/")

    protected val maxAge: Option[Int] = config.getOptional[Int]("sentry.cookie.maxAge")

    def put(token: AuthenticityToken)(result: Result)(implicit request: RequestHeader): Result = {
        val c = Cookie(name, sign(token), maxAge, path, domain, secure, httpOnly)
        result.withCookies(c)
    }

    def extract(request: RequestHeader): Option[AuthenticityToken] = {
        request.cookies.get(name).flatMap(c => verifyHmac(c.value))
    }

    def delete(result: Result)(implicit request: RequestHeader): Result = {
        result.discardingCookies(DiscardingCookie(name))
    }

}
