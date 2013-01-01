package com.roundeights.foldout

import org.apache.commons.codec.binary.Base64
import scala.language.reflectiveCalls

/**
 * Authentication credentials
 */
case class Auth (
    val username: String,
    private val password: String
) {

    /** Returns the auth data as a basic auth http header value */
    lazy val basicAuth: String = "Basic " + Base64.encodeBase64String(
        "%s:%s".format( username, password ).getBytes("UTF-8")
    )

    /** Adds an authorization header to the given object */
    def addHeader [T] (
        req: { def addHeader( name: String, value: String ): T }
    ): T = req.addHeader( "Authorization", basicAuth )

    /** {@inheritDoc} */
    override def toString: String = "Auth(" + username + ")"

}


