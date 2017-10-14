package com.jaroop.play.sentry

import javax.inject.Inject
import play.api.cache.CacheApi
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

/** A helper to wrap the synchronous [[play.api.cache.CacheApi CacheApi]] from Play 2.5.x */
class AsyncCacheApi @Inject() (cache: CacheApi) {

    def set(key: String, value: Any, expiration: Duration = Duration.Inf): Future[Unit] =
        Future.successful(cache.set(key, value, expiration))

    def remove(key: String): Future[Unit] = Future.successful(cache.remove(key))

    def get[A: ClassTag](key: String): Future[Option[A]] = Future.successful(cache.get(key))

}
