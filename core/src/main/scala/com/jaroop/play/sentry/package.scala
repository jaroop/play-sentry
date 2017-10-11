package com.jaroop.play

import play.api.mvc.Result

/** The Play Sentry API. */
package object sentry {

    /** Play Sentry tokens are generated as strings. */
    type AuthenticityToken = String

    /** Signed Play Sentry tokens are strings. */
    type SignedToken = String

    /** An alias to a function that updates a `Result` with a new `Result`. */
    type ResultUpdater = Result => Result

}
