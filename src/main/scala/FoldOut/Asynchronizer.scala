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
    ): STATE = responseStatus.getStatusCode match {
        case 404 => {
            promise.success( None )
            STATE.ABORT
        }
        case 409 => {
            promise.failure( new RevisionConflict )
            STATE.ABORT
        }
        case code if code >= 300 || code < 200 => {
            promise.failure( new RequestError(responseStatus) )
            STATE.ABORT
        }
        case _ => STATE.CONTINUE
    }

    /** {@inheritDoc} */
    override def onHeadersReceived( headers: HttpResponseHeaders ): STATE
        = STATE.CONTINUE

    /** {@inheritDoc} */
    override def onCompleted(): Unit = promise.complete( Try {
        Some( nParser.json(body.toString) )
    } )

}

