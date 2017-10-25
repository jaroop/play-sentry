package test

import com.jaroop.play.sentry._
import org.specs2.mock._
import org.specs2.mutable._
import play.api.Configuration
import play.api.http.HeaderNames
import play.api.libs.crypto.CookieSigner
import play.api.mvc._

class CookieTokenAccessorSpec extends Specification with Mockito {

    val config = mock[Configuration]
    config.getString("sentry.cookie.name").returns(Option("SENTRY_TEST_ID"))
    config.getBoolean("sentry.cookie.secure").returns(Option(false))
    config.getBoolean("sentry.cookie.httpOnly").returns(Option(false))
    config.getString("sentry.cookie.domain").returns(Option("www.test.com"))
    config.getString("sentry.cookie.path").returns(Option("/"))
    config.getInt("sentry.cookie.maxAge").returns(Option(200000))

    "CookieTokenAccessor" should {

        tag("put")
        "issue a cookie with a valid authenticity token" in {
            val token = "abcdef"
            implicit val request = mock[RequestHeader]
            val signer = mock[CookieSigner]
            signer.sign(token).returns("0" * 40)
            val tokenAccessor = new CookieTokenAccessor(config, signer)
            val result = tokenAccessor.put(token)(Results.Ok)
            val cookie = Cookie(
                name = "SENTRY_TEST_ID",
                value = "0" * 40 + token,
                maxAge = Option(200000),
                path = "/",
                domain = Option("www.test.com"),
                secure = false,
                httpOnly = false
            )
            result.header.headers.get(HeaderNames.SET_COOKIE) must beSome(Cookies.encodeSetCookieHeader(Seq(cookie)))
        }

        tag("extract")
        "extract a valid authenticity token from a request header" in {
            val token = "abcdef"
            val cookie = Cookie(
                name = "SENTRY_TEST_ID",
                value = "0" * 40 + token
            )
            val cookies = mock[Cookies]
            cookies.get("SENTRY_TEST_ID").returns(Option(cookie))
            val request = mock[RequestHeader]
            request.cookies.returns(cookies)

            val signer = mock[CookieSigner]
            signer.sign(token).returns("0" * 40)
            val tokenAccessor = new CookieTokenAccessor(config, signer)
            tokenAccessor.extract(request) must beSome("abcdef")
        }

        tag("extract")
        "fail to extract an authenticity token where the signature does not match" in {
            val token = "abcdef"
            val cookie = Cookie(
                name = "SENTRY_TEST_ID",
                value = "x" * 40 + token // 40 wrong characters, plus the token
            )
            val cookies = mock[Cookies]
            cookies.get("SENTRY_TEST_ID").returns(Option(cookie))
            val request = mock[RequestHeader]
            request.cookies.returns(cookies)

            val signer = mock[CookieSigner]
            signer.sign(token).returns("0" * 40)
            val tokenAccessor = new CookieTokenAccessor(config, signer)
            tokenAccessor.extract(request) must beNone
        }

        tag("extract")
        "fail to extract an authenticity token when the request header does not have one" in {
            val token = "abcdef"
            val cookies = mock[Cookies]
            cookies.get("SENTRY_TEST_ID").returns(Option.empty[Cookie])
            val request = mock[RequestHeader]
            request.cookies.returns(cookies)
            val tokenAccessor = new CookieTokenAccessor(config, mock[CookieSigner])
            tokenAccessor.extract(request) must beNone
        }

        tag("delete")
        "remove sentry cookies from the result" in {
            implicit val request = mock[RequestHeader]
            val expectedResult = Results.Ok
            val r = mock[Result]
            r.discardingCookies(DiscardingCookie("SENTRY_TEST_ID")).returns(expectedResult)
            val tokenAccessor = new CookieTokenAccessor(config, mock[CookieSigner])
            tokenAccessor.delete(r) must equalTo(expectedResult)
        }

    }

}
