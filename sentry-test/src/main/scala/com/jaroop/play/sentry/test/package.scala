package com.jaroop.play.sentry

import com.google.inject.{ AbstractModule, Injector => GuiceInjector, Key, TypeLiteral }
import play.api.Application
import play.api.test.FakeRequest
import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration._

/**
 *  Provides helpers for testing controllers that user Play Sentry components.
 */
package object test {

    implicit class FakeRequestOps[A](request: FakeRequest[A])(implicit ec: ExecutionContext, app: Application) {

        /**
         *  Adds a special header to a [[play.api.test.FakeRequest FakeRequest]] in order to allow grant a user with a specific
         *  ID a test session. Use this method for tests that use an injected router.
         *
         *  @param userId The ID of the user to grant the session to.
         *  @tparam E The environment type of your application.
         *  @return A new [[play.api.test.FakeRequest FakeRequest]] with an authenticity token for the user included.
         */
        def withLoggedIn[E <: Env](userId: E#Id): FakeRequest[A] = {
            val cache = Application.instanceCache[GuiceInjector]
            val guiceInjector = cache(app)
            val idContainer = guiceInjector.getInstance(Key.get(new TypeLiteral[IdContainer[E#Id]] {}))
            val token = Await.result(idContainer.startNewSession(userId, 1.hour), 10.seconds)
            request.withHeaders("SENTRY_TEST_TOKEN" -> token)
        }

    }

}
