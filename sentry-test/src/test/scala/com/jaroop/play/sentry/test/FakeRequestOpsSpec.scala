package com.jaroop.play.sentry.test

import com.jaroop.play.sentry._
import org.specs2.concurrent._
import org.specs2.mutable._
import play.api.Mode
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.routing._
import play.api.test._, Helpers._

class FakeRequestOpsSpec(implicit ee: ExecutionEnv) extends Specification {

    val application = new GuiceApplicationBuilder()
        .bindings(new TestAuthModule)
        .overrides(bind[Router].toProvider[ScalaRoutesProvider])
        .in(Mode.Test)
        .build

    "withLoggedIn" should {

        "create a test session for a user" in new WithApplication(application) {
            val request = FakeRequest(GET, "/test").withLoggedIn[TestEnv](1L)
            val Some(result) = route(app, request)
            status(result) must equalTo(OK)
            contentAsString(result) must equalTo("You are test@example.com")
        }

    }

}
