package test

import com.jaroop.play.sentry._

trait TestEnv extends Env {
    type Id = Long
    type User = test.User
    type Authority = Role
}

case class User(id: Option[Long], email: String)

object User {

    val test = User(Option(1L), "test@example.com")

}

sealed trait Role
case object Admin extends Role
case object Manager extends Role
case object Employee extends Role

case class StaticTokenGenerator(override val generate: AuthenticityToken) extends TokenGenerator
