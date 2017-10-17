package test

import com.jaroop.play.sentry._
import org.mockito.Matchers._
import org.specs2.concurrent._
import org.specs2.mock._
import org.specs2.mutable._
import play.api.Environment
import play.api.mvc.{ RequestHeader, Result, Results }
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

class AsyncAuthSpec(implicit ee: ExecutionEnv) extends Specification with Mockito {

    class AsyncAuthWithUser[E <: Env] (
        config: AuthConfig[TestEnv],
        idContainer: IdContainer[Long],
        tokenAccessor: TokenAccessor
    ) extends AsyncAuth[TestEnv](config, idContainer, tokenAccessor, mock[Environment]) {
        override def restoreUser(implicit request: RequestHeader, ec: ExecutionContext): Future[(Option[User], ResultUpdater)] =
            Future.successful((Option(User.test), identity _))
    }

    "The AsyncAuth trait" should {

        tag("restoreUser")
        "successfully restore a user" in {
            val config = mock[AuthConfig[TestEnv]]
            val idContainer = mock[IdContainer[Long]]
            val tokenAccessor = mock[TokenAccessor]
            val auth = new AsyncAuth[TestEnv](config, idContainer, tokenAccessor, mock[Environment])
            implicit val request = mock[RequestHeader]
            val token = "secrettoken"
            val userId = 1L
            tokenAccessor.extract(request).returns(Option(token))
            idContainer.get(token).returns(Future.successful(Option(1L)))
            config.resolveUser(anyObject)(anyObject).returns(Future.successful(Option(User.test)))
            idContainer.prolongTimeout(anyObject, anyObject)(anyObject).returns(Future.successful(()))
            val result = mock[Result]
            tokenAccessor.put(token)(result).returns(result)
            auth.restoreUser.map(_._1) must beSome(User.test).await
        }

        tag("restoreUser")
        "fail to restore a user when they do not have a token" in {
            val config = mock[AuthConfig[TestEnv]]
            val idContainer = mock[IdContainer[Long]]
            val tokenAccessor = mock[TokenAccessor]
            val auth = new AsyncAuth[TestEnv](config, idContainer, tokenAccessor, mock[Environment])
            implicit val request = mock[RequestHeader]
            val token = "secrettoken"
            val userId = 1L
            tokenAccessor.extract(request).returns(Option.empty[AuthenticityToken])
            idContainer.get(token).returns(Future.successful(Option(1L)))
            config.resolveUser(anyObject)(anyObject).returns(Future.successful(Option(User.test)))
            idContainer.prolongTimeout(anyObject, anyObject)(anyObject).returns(Future.successful(()))
            val result = mock[Result]
            tokenAccessor.put(token)(result).returns(result)
            auth.restoreUser.map(_._1) must beNone.await
        }

        tag("restoreUser")
        "fail to restore a user when they have a valid token, but the IdContainer does not recognize it" in {
            val config = mock[AuthConfig[TestEnv]]
            val idContainer = mock[IdContainer[Long]]
            val tokenAccessor = mock[TokenAccessor]
            val auth = new AsyncAuth[TestEnv](config, idContainer, tokenAccessor, mock[Environment])
            implicit val request = mock[RequestHeader]
            val token = "secrettoken"
            val userId = 1L
            tokenAccessor.extract(request).returns(Option(token))
            idContainer.get(token).returns(Future.successful(Option.empty[Long]))
            config.resolveUser(anyObject)(anyObject).returns(Future.successful(Option(User.test)))
            idContainer.prolongTimeout(anyObject, anyObject)(anyObject).returns(Future.successful(()))
            val result = mock[Result]
            tokenAccessor.put(token)(result).returns(result)
            auth.restoreUser.map(_._1) must throwAn[Exception].await
        }

        tag("restoreUser")
        "fail to restore a user when they have a valid token, but the IdContainer completely fails" in {
            val config = mock[AuthConfig[TestEnv]]
            val idContainer = mock[IdContainer[Long]]
            val tokenAccessor = mock[TokenAccessor]
            val auth = new AsyncAuth[TestEnv](config, idContainer, tokenAccessor, mock[Environment])
            implicit val request = mock[RequestHeader]
            val token = "secrettoken"
            val userId = 1L
            tokenAccessor.extract(request).returns(Option(token))
            idContainer.get(token).returns(Future.failed(new Exception))
            config.resolveUser(anyObject)(anyObject).returns(Future.successful(Option(User.test)))
            idContainer.prolongTimeout(anyObject, anyObject)(anyObject).returns(Future.successful(()))
            val result = mock[Result]
            tokenAccessor.put(token)(result).returns(result)
            auth.restoreUser.map(_._1) must throwAn[Exception].await
        }

        tag("restoreUser")
        "fail to restore a user when the user cannot be resolved by ID" in {
            val config = mock[AuthConfig[TestEnv]]
            val idContainer = mock[IdContainer[Long]]
            val tokenAccessor = mock[TokenAccessor]
            val auth = new AsyncAuth[TestEnv](config, idContainer, tokenAccessor, mock[Environment])
            implicit val request = mock[RequestHeader]
            val token = "secrettoken"
            val userId = 1L
            tokenAccessor.extract(request).returns(Option(token))
            idContainer.get(token).returns(Future.successful(Option(userId)))
            config.resolveUser(anyObject)(anyObject).returns(Future.successful(Option.empty[User]))
            idContainer.prolongTimeout(anyObject, anyObject)(anyObject).returns(Future.successful(()))
            val result = mock[Result]
            tokenAccessor.put(token)(result).returns(result)
            auth.restoreUser.map(_._1) must throwAn[Exception].await
        }

        tag("restoreUser")
        "fail to restore a user when the resolver completely fails" in {
            val config = mock[AuthConfig[TestEnv]]
            val idContainer = mock[IdContainer[Long]]
            val tokenAccessor = mock[TokenAccessor]
            val auth = new AsyncAuth[TestEnv](config, idContainer, tokenAccessor, mock[Environment])
            implicit val request = mock[RequestHeader]
            val token = "secrettoken"
            val userId = 1L
            tokenAccessor.extract(request).returns(Option(token))
            idContainer.get(token).returns(Future.successful(Option(userId)))
            config.resolveUser(anyObject)(anyObject).returns(Future.failed(new Exception))
            idContainer.prolongTimeout(anyObject, anyObject)(anyObject).returns(Future.successful(()))
            val result = mock[Result]
            tokenAccessor.put(token)(result).returns(result)
            auth.restoreUser.map(_._1) must throwAn[Exception].await
        }

        tag("restoreUser")
        "fail to restore a user when the IdContainer fails to prolong the session" in {
            val config = mock[AuthConfig[TestEnv]]
            val idContainer = mock[IdContainer[Long]]
            val tokenAccessor = mock[TokenAccessor]
            val auth = new AsyncAuth[TestEnv](config, idContainer, tokenAccessor, mock[Environment])
            implicit val request = mock[RequestHeader]
            val token = "secrettoken"
            val userId = 1L
            tokenAccessor.extract(request).returns(Option(token))
            idContainer.get(token).returns(Future.successful(Option(1L)))
            config.resolveUser(anyObject)(anyObject).returns(Future.successful(Option(User.test)))
            idContainer.prolongTimeout(token, 1.minutes).returns(Future.failed(new Exception))
            val result = mock[Result]
            tokenAccessor.put(token)(result).returns(result)
            auth.restoreUser.map(_._1) must throwAn[Exception].await
        }

        tag("authorized")
        "successfully authorize a user with the proper credentials" in {
            val config = mock[AuthConfig[TestEnv]]
            val idContainer = mock[IdContainer[Long]]
            val tokenAccessor = mock[TokenAccessor]
            val auth = new AsyncAuthWithUser[TestEnv](config, idContainer, tokenAccessor)
            implicit val request = mock[RequestHeader]
            config.authorize(User.test, Admin).returns(Future.successful(true))
            auth.authorized(Admin).map(_.right.map(_._1)) must beRight(User.test).await
        }

        tag("authorized")
        "fail to authorize a user that is not authorized for an authority key" in {
            val config = mock[AuthConfig[TestEnv]]
            val idContainer = mock[IdContainer[Long]]
            val tokenAccessor = mock[TokenAccessor]
            val auth = new AsyncAuthWithUser[TestEnv](config, idContainer, tokenAccessor)
            implicit val request = mock[RequestHeader]
            val result: Result = Results.Forbidden
            config.authorize(User.test, Admin).returns(Future.successful(false))
            config.authorizationFailed(anyObject, anyObject, anyObject)(anyObject).returns(
                Future.successful(result)
            )
            auth.authorized(Admin) must beLeft(result).await
        }

        tag("authorized")
        "fail to authorize a user when the config authorize function fails completely" in {
            val config = mock[AuthConfig[TestEnv]]
            val idContainer = mock[IdContainer[Long]]
            val tokenAccessor = mock[TokenAccessor]
            val auth = new AsyncAuthWithUser[TestEnv](config, idContainer, tokenAccessor)
            implicit val request = mock[RequestHeader]
            val result: Result = Results.Forbidden
            config.authorize(User.test, Admin).returns(Future.failed(new Exception))
            config.authorizationFailed(anyObject, anyObject, anyObject)(anyObject).returns(
                Future.successful(result)
            )
            auth.authorized(Admin) must beLeft(result).await
        }

        tag("authorized")
        "fail to authorize a user when they are not authenticated" in {
            val config = mock[AuthConfig[TestEnv]]
            val idContainer = mock[IdContainer[Long]]
            val tokenAccessor = mock[TokenAccessor]

            val auth = new AsyncAuth[TestEnv](config, idContainer, tokenAccessor, mock[Environment]) {
                override def restoreUser(implicit request: RequestHeader, ec: ExecutionContext): Future[(Option[User], ResultUpdater)] =
                    Future.successful((Option.empty[User], identity _))
            }
            implicit val request = mock[RequestHeader]
            val result: Result = Results.Forbidden
            config.authenticationFailed(anyObject)(anyObject).returns(
                Future.successful(result)
            )
            auth.authorized(Admin) must beLeft(result).await
        }

        tag("authorized")
        "fail to authorize a user when session user restoration fails completely" in {
            val config = mock[AuthConfig[TestEnv]]
            val idContainer = mock[IdContainer[Long]]
            val tokenAccessor = mock[TokenAccessor]
            val auth = new AsyncAuth[TestEnv](config, idContainer, tokenAccessor, mock[Environment]) {
                override def restoreUser(implicit request: RequestHeader, ec: ExecutionContext): Future[(Option[User], ResultUpdater)] =
                    Future.failed(new Exception)
            }
            implicit val request = mock[RequestHeader]
            val result: Result = Results.Forbidden
            config.authenticationFailed(anyObject)(anyObject).returns(
                Future.successful(result)
            )
            auth.authorized(Admin) must beLeft(result).await
        }

    }

}
