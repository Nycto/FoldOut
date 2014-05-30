package com.roundeights.foldout

import com.ning.http.client.{Request, HttpResponseStatus}

/**
 * Thrown when an update generates a conflict
 */
case class RevisionConflict( url: String ) extends Exception(
    "Revision Conflict for: %s".format(url)
)

/**
 * An error during a request
 */
case class RequestError ( message: String ) extends Exception ( message ) {

    /** Constructs an error for a given request */
    def this ( request: Request, message: String ) = this(
        "CouchDB Request Error - %s while fetching %s".format(
            message, request.getUrl
        )
    )

    /** Constructs an error for the given request/response pair */
    def this ( request: Request, status: HttpResponseStatus ) = this(
        request, "%d: %s".format( status.getStatusCode, status.getStatusText )
    )

}

