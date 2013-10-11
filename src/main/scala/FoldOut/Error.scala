package com.roundeights.foldout

import com.ning.http.client.{Request, HttpResponseStatus}

/**
 * Thrown when an update generates a conflict
 */
class RevisionConflict extends Exception

/**
 * An error during a request
 */
class RequestError ( message: String ) extends Exception ( message ) {

    /** An alternate constructor for building from a Response */
    def this ( request: Request, status: HttpResponseStatus ) = {
        this("CouchDB Request Error - %d: %s while fetching %s".format(
            status.getStatusCode,
            status.getStatusText,
            request.getUrl
        ))
    }

}

