package com.jaroop.play.sentry

import java.security.SecureRandom
import scala.util.Random

/** Generates secure randomly generated tokens. */
class TokenGenerator {

    private val random = new Random(new SecureRandom())

    private val table = "abcdefghijklmnopqrstuvwxyz1234567890_.~*'()"

    /** Generates a randomly generated string to use as an authenticity token. */
    def generate: AuthenticityToken =
        Iterator.continually(random.nextInt(table.size)).map(table).take(64).mkString

}
