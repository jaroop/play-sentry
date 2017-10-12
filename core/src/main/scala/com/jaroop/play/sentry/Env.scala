package com.jaroop.play.sentry

/**
 *  Defines the types that are implemented by client-code tied to user authentication and authorization. This provides
 *  the freedom to use any type of user, user ID, or authority key, without needing those types to implement any
 *  sort of interface to work. This type will allow you bundle up your type definitions into one place, and use it to
 *  specify what types are needed to the Play Sentry components when they are injected.
 */
trait Env {

    /** The type of the User's ID. For example: `Long`, `String`, etc. */
    type Id

    /** The type of the authenticated user. Implement this as the user type in your application. */
    type User

    /**
     *  The type used as an authority key. That is, the type that encodes different levels of authorization.
     *  Every `Action` that uses authorization must specify an authority key of this type. For example, this could be
     *  a role, a group, or something else a user can be associated with.
     */
    type Authority

}
