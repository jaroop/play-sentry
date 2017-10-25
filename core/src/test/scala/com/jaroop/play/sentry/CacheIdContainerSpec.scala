package test

import com.jaroop.play.sentry._
import org.mockito.Matchers._
import org.specs2.concurrent._
import org.specs2.mock._
import org.specs2.mutable._
import play.api.mvc._
import scala.reflect._
import scala.concurrent.Future
import scala.concurrent.duration._

class CacheIdContainerSpec(implicit ee: ExecutionEnv) extends Specification with Mockito {

    "CacheIdContainer" should {

        tag("startNewSession")
        "start a new session, returning an authenticity token, ensuring that the correct values are cached" in {
            val cache = mock[AsyncCacheApi]
            cache.set(anyObject, anyObject, anyObject).returns(Future.successful(()))
            cache.get[Long](anyObject)(anyObject).returns(Future.successful(Option.empty[Long]))
            cache.remove(anyObject).returns(Future.successful(()))
            val idContainer = new CacheIdContainer[Long](cache, StaticTokenGenerator("abcdef"))
            idContainer.startNewSession(1L, 1.hour) must equalTo("abcdef").await
            there was one(cache).get[String]("1:userId") andThen
                one(cache).remove("1:userId") andThen
                one(cache).get[Long]("abcdef:token") andThen
                one(cache).set("abcdef:token", 1L, 1.hour) andThen
                one(cache).set("1:userId", "abcdef", 1.hour)

        }

        tag("startNewSession")
        "start a new session, destroy any existing sessions for that user, and return an authenticity token" in {
            // This is essentially the same as the above test, but because the is a token already associated with the userId 1 in
            // the cache, there is an extra step where it is removed.
            val cache = mock[AsyncCacheApi]
            cache.set(anyObject, anyObject, anyObject).returns(Future.successful(()))
            cache.get("1:userId")(classTag[String]).returns(Future.successful(Option("abcdef")))
            cache.get("abcdef:token")(classTag[Long]).returns(Future.successful(Option.empty[Long]))
            cache.remove(anyObject).returns(Future.successful(()))
            val idContainer = new CacheIdContainer[Long](cache, StaticTokenGenerator("abcdef"))
            idContainer.startNewSession(1L, 1.hour) must equalTo("abcdef").await
            there was one(cache).get[String]("1:userId") andThen
                one(cache).remove("abcdef:token") andThen
                one(cache).remove("1:userId") andThen
                one(cache).get[Long]("abcdef:token") andThen
                one(cache).set("abcdef:token", 1L, 1.hour) andThen
                one(cache).set("1:userId", "abcdef", 1.hour)

        }

        tag("startNewSession")
        "fail to start a new session if there is a cache failure" in {
            val cache = mock[AsyncCacheApi]
            cache.set(anyObject, anyObject, anyObject).returns(Future.successful(()))
            cache.get("1:userId")(classTag[String]).returns(Future.failed(new Exception))
            cache.get("abcdef:token")(classTag[Long]).returns(Future.successful(Option.empty[Long]))
            cache.remove(anyObject).returns(Future.successful(()))
            val idContainer = new CacheIdContainer[Long](cache, StaticTokenGenerator("abcdef"))
            idContainer.startNewSession(1L, 1.hour) must throwA[Exception].await
        }

        tag("get")
        "get a user's ID by their authenticity token" in {
            val cache = mock[AsyncCacheApi]
            cache.get("abcdef:token")(classTag[Long]).returns(Future.successful(Option(1L)))
            val idContainer = new CacheIdContainer[Long](cache, StaticTokenGenerator("abcdef"))
            idContainer.get("abcdef") must beSome(1L).await
        }

        tag("get")
        "return None when a provided authenticity token is invalid" in {
            val cache = mock[AsyncCacheApi]
            cache.get("abcdef:token")(classTag[Long]).returns(Future.successful(Option.empty[Long]))
            val idContainer = new CacheIdContainer[Long](cache, StaticTokenGenerator("abcdef"))
            idContainer.get("abcdef") must beNone.await
        }

        tag("get")
        "fail to get a user's ID when there is a cache failure" in {
            val cache = mock[AsyncCacheApi]
            cache.get("abcdef:token")(classTag[Long]).returns(Future.failed(new Exception))
            val idContainer = new CacheIdContainer[Long](cache, StaticTokenGenerator("abcdef"))
            idContainer.get("abcdef") must throwA[Exception].await
        }

        tag("remove")
        "successfully destroy a user's session" in {
            val cache = mock[AsyncCacheApi]
            cache.get("abcdef:token")(classTag[Long]).returns(Future.successful(Option(1L)))
            cache.remove(anyObject).returns(Future.successful(()))
            val idContainer = new CacheIdContainer[Long](cache, StaticTokenGenerator("abcdef"))
            idContainer.remove("abcdef") must equalTo(()).await
            there was one(cache).remove("1:userId") andThen
                one(cache).remove("abcdef:token")
        }

        tag("remove")
        "destroy a user's session, even if the userId isn't kept in the cache" in {
            val cache = mock[AsyncCacheApi]
            cache.get("abcdef:token")(classTag[Long]).returns(Future.successful(Option.empty[Long]))
            cache.remove(anyObject).returns(Future.successful(()))
            val idContainer = new CacheIdContainer[Long](cache, StaticTokenGenerator("abcdef"))
            idContainer.remove("abcdef") must equalTo(()).await
            there was no(cache).remove("1:userId") andThen
                one(cache).remove("abcdef:token")
        }

        tag("remove")
        "fail to destroy a user's session if there is a cache failure" in {
            val cache = mock[AsyncCacheApi]
            cache.get("abcdef:token")(classTag[Long]).returns(Future.failed(new Exception))
            cache.remove(anyObject).returns(Future.successful(()))
            val idContainer = new CacheIdContainer[Long](cache, StaticTokenGenerator("abcdef"))
            idContainer.remove("abcdef") must throwA[Exception].await
        }

        tag("prolongTimeout")
        "prolong a user's session, if their session still exists" in {
            val cache = mock[AsyncCacheApi]
            cache.get("abcdef:token")(classTag[Long]).returns(Future.successful(Option(1L)))
            cache.set(anyObject, anyObject, anyObject).returns(Future.successful(()))
            val idContainer = new CacheIdContainer[Long](cache, StaticTokenGenerator("abcdef"))
            val timeout = 1.hour
            idContainer.prolongTimeout("abcdef", timeout) must equalTo(()).await
            there was one(cache).set("abcdef:token", 1L, timeout) andThen
                one(cache).set("1:userId", "abcdef", timeout)
        }

        tag("prolongTimeout")
        "fail to prolong a user's session if it is already expired or invalid" in {
            val cache = mock[AsyncCacheApi]
            cache.get("abcdef:token")(classTag[Long]).returns(Future.successful(Option.empty[Long]))
            cache.set(anyObject, anyObject, anyObject).returns(Future.successful(()))
            val idContainer = new CacheIdContainer[Long](cache, StaticTokenGenerator("abcdef"))
            val timeout = 1.hour
            idContainer.prolongTimeout("abcdef", timeout) must throwA[Exception].await
            there was no(cache).set("abcdef:token", 1L, timeout) andThen
                no(cache).set("1:userId", "abcdef", timeout)
        }

        tag("prolongTimeout")
        "fail to prolong a user's session if there is a cache failure" in {
            val cache = mock[AsyncCacheApi]
            cache.get("abcdef:token")(classTag[Long]).returns(Future.failed(new Exception))
            cache.set(anyObject, anyObject, anyObject).returns(Future.successful(()))
            val idContainer = new CacheIdContainer[Long](cache, StaticTokenGenerator("abcdef"))
            val timeout = 1.hour
            idContainer.prolongTimeout("abcdef", timeout) must throwA[Exception].await
            there was no(cache).set("abcdef:token", 1L, timeout) andThen
                no(cache).set("1:userId", "abcdef", timeout)
        }

    }

}
