package models

case class User(id: Option[Long], email: String)

class UserService {

    def read(id: Long): Option[User] = id match {
        case 1L => Option(User(Option(1L), "test@jaroop.com"))
        case 2L => Option(User(Option(2L), "bob@jaroop.com"))
        case _ => None
    }

}
