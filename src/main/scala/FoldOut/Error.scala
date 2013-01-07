package com.roundeights.foldout

import com.ning.http.client.HttpResponseStatus

/**
 * An error during a request
 */
class RequestError ( message: String ) extends Exception ( message ) {

    /**
     * An alternate constructor for building from a Response
     */
    def this ( status: HttpResponseStatus ) = this("%d: %s".format(
        status.getStatusCode, status.getStatusText
    ))

}

/** MissingDocKey Companion */
object MissingKey {

    /** Constructor */
    def apply ( key: String ) = new MissingKey( key )

    /** A document missing the ID key */
    def id = MissingKey("_id")

    /** A document missing the revision key */
    def rev = MissingKey("_rev")

}

/**
 * An exception for when a document is missing a key
 */
class MissingKey ( key: String )
    extends Exception ( "Document is missing the '%s' key".format(key) )

