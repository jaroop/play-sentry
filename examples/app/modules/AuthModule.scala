package modules

import com.google.inject.{ AbstractModule, TypeLiteral }
import com.jaroop.play.sentry._
import controllers.{ EnvImpl, SimpleAuthConfig }
import scala.reflect.{ ClassTag, classTag }

class AuthModule extends AbstractModule {

    def configure(): Unit = {
        bind(new TypeLiteral[AuthConfig[EnvImpl]]() {}).to(classOf[SimpleAuthConfig])
        bind(classOf[TokenAccessor]).to(classOf[CookieTokenAccessor])
        bind(new TypeLiteral[IdContainer[Long]] {}).to(new TypeLiteral[CacheIdContainer[Long]] {})
        bind(new TypeLiteral[ClassTag[Long]] {}).toInstance(classTag[Long])
    }

}
