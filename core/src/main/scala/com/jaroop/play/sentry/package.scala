package com.jaroop.play

import play.api.mvc.Result

package object sentry {

    type AuthenticityToken = String

    type SignedToken = String

    type ResultUpdater = Result => Result

}
