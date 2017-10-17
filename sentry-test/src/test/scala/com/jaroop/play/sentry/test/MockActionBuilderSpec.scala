package com.jaroop.play.sentry.test

import com.jaroop.play.sentry._
import org.specs2.concurrent._
import org.specs2.mock._
import org.specs2.mutable._
import play.api.Application
import play.api.libs.json._
import play.api.mvc._
import play.api.test._, Helpers._
import scala.concurrent.Future
import scala.concurrent.duration._

class MockActionBuilderSpec(implicit ee: ExecutionEnv) extends Specification {

    def parse(implicit app: Application): DefaultPlayBodyParsers = {
        val cache = Application.instanceCache[DefaultPlayBodyParsers]
        cache(app)
    }

    "The mock action builders" should {

        "invoke a simple action block" in {
            val builder = MockAuthenticatedActionBuilder[TestEnv](Account.test)
            val action = builder { request =>
                Results.Ok(s"Email: ${request.user.email}")
            }
            val request = FakeRequest()
            val result = action(request)
            status(result) must equalTo(OK)
            contentAsString(result) must equalTo("Email: test@example.com")
        }

        "invoke an action block asynchronously" in {
            val builder = MockAuthenticatedActionBuilder[TestEnv](Account.test)
            val action = builder.async { request =>
                Future.successful(Results.Ok(s"Email: ${request.user.email}"))
            }
            val request = FakeRequest()
            val result = action(request)
            status(result) must equalTo(OK)
            contentAsString(result) must equalTo("Email: test@example.com")
        }

        "invoke an action with a json body parser" in new WithApplication {
            val builder = MockAuthenticatedActionBuilder[TestEnv](Account.test)
            val action = builder(parse.json) { request =>
                Results.Ok(s"Email: ${request.user.email}")
            }
            val request = FakeRequest().withBody(Json.obj("key" -> "value"))
            val result = action(request)
            status(result) must equalTo(OK)
            contentAsString(result) must equalTo("Email: test@example.com")
        }

        "invoke a simple action block with an optional user" in {
            val builder = MockOptionalAuthenticatedActionBuilder[TestEnv](Option(Account.test))
            val action = builder { request =>
                Results.Ok(s"""Email: ${request.user.map(_.email).getOrElse("n/a")}""")
            }
            val request = FakeRequest()
            val result = action(request)
            status(result) must equalTo(OK)
            contentAsString(result) must equalTo("Email: test@example.com")
        }

        "invoke a simple action block asynchronously" in {
            val builder = MockOptionalAuthenticatedActionBuilder[TestEnv](Option(Account.test))
            val action = builder.async { request =>
                Future.successful(Results.Ok(s"""Email: ${request.user.map(_.email).getOrElse("n/a")}"""))
            }
            val request = FakeRequest()
            val result = action(request)
            status(result) must equalTo(OK)
            contentAsString(result) must equalTo("Email: test@example.com")
        }

        "invoke an action with a json body parser" in new WithApplication {
            val builder = MockOptionalAuthenticatedActionBuilder[TestEnv](Option(Account.test))
            val action = builder(parse.json) { request =>
                Results.Ok(s"""Email: ${request.user.map(_.email).getOrElse("n/a")}""")
            }
            val request = FakeRequest().withBody(Json.obj("key" -> "value"))
            val result = action(request)
            status(result) must equalTo(OK)
            contentAsString(result) must equalTo("Email: test@example.com")
        }

    }

}
