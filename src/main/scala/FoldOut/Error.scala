package com.roundeights.foldout

import com.ning.http.client.HttpResponseStatus

/**
 * Thrown when an update generates a conflict
 */
class RevisionConflict extends Exception

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

