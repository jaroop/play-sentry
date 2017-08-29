package models

sealed trait Permission

case object View extends Permission

case object List extends Permission

class PermissionService {

    def isAuthorized(user: User, permission: Permission): Boolean = (user.id, permission) match {
        case (Some(1L), View) => true
        case (Some(1L), List) => true
        case (Some(2L), View) => true
        case (Some(2L), List) => false
        case _ => false
    }
}
