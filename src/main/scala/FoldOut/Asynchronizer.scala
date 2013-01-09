package com.roundeights.foldout

import com.roundeights.scalon.{nElement, nParser}

import scala.concurrent.Promise
import scala.util.Try

import com.ning.http.client.{ AsyncHandler, HttpResponseBodyPart }
import com.ning.http.client.{ HttpResponseStatus, HttpResponseHeaders }
import com.ning.http.client.AsyncHandler.STATE

/**
 * The asynchronous request handler that accumulates a request as it is
 * receied.
 */
private[foldout] class Asynchronizer (
    private val promise: Promise[Option[nElement]]
) extends AsyncHandler[Unit] {

    /** Collects the body of the request as it is received */
    private val body = new StringBuilder()

    /** {@inheritDoc} */
    override def onThrowable( t: Throwable ): Unit = promise.failure(t)

    /** {@inheritDoc} */
    override def onBodyPartReceived(
        bodyPart: HttpResponseBodyPart
    ): STATE = {
        body.append( new String(bodyPart.getBodyPartBytes, "UTF-8") )
        STATE.CONTINUE
    }

    /** {@inheritDoc} */
    override def onStatusReceived(
        responseStatus: HttpResponseStatus
    ): STATE = {
        if ( responseStatus.getStatusCode == 404 ) {
            promise.success( None )
            STATE.ABORT
        }
        else if (
            responseStatus.getStatusCode >= 300
            || responseStatus.getStatusCode < 200
        ) {
            promise.failure( new RequestError(responseStatus) )
            STATE.ABORT
        }
        else {
            STATE.CONTINUE
        }
    }

    /** {@inheritDoc} */
    override def onHeadersReceived( headers: HttpResponseHeaders ): STATE
        = STATE.CONTINUE

    /** {@inheritDoc} */
    override def onCompleted(): Unit = promise.complete( Try {
        Some( nParser.json(body.toString) )
    } )

}

