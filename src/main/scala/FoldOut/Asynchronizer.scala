package com.roundeights.foldout

import com.roundeights.scalon.{nElement, nParser, nException}

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.Promise
import scala.util.Try

import com.ning.http.client.{ Request, AsyncHandler, HttpResponseBodyPart }
import com.ning.http.client.{ HttpResponseStatus, HttpResponseHeaders }
import com.ning.http.client.AsyncHandler.STATE

/**
 * The asynchronous request handler that accumulates a request as it is
 * receied.
 *
 * This implementation is NOT thread safe. It assumes that the calling code
 * is handling the synchronization. If needed, this could easily be fixed by
 * using an actor.
 */
private[foldout] class Asynchronizer (
    private val request: Request
) extends AsyncHandler[Unit] {

    /** The promise that will contain the output of this request */
    private val promise = Promise[Option[nElement]]

    /** The future for accessing the result of this request */
    val future = promise.future

    /** The error status, if one was encountered */
    private val status = new AtomicReference[Option[HttpResponseStatus]](None)

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
            status.set( Some(responseStatus) )
            STATE.CONTINUE
        }
        case _ => STATE.CONTINUE
    }

    /** {@inheritDoc} */
    override def onHeadersReceived( headers: HttpResponseHeaders ): STATE
        = STATE.CONTINUE

    /** {@inheritDoc} */
    override def onCompleted(): Unit = status.get match {
        case None => promise.complete( Try {
            Some( nParser.json(body.toString) )
        } )
        case Some(status) => {
            try {
                val json = nParser.jsonObj(body.toString)
                promise.failure( new RequestError( request,
                    "%s: %s".format( json.str("error"), json.str("reason") )
                ) )
            }
            catch {
                // If parsing the JSON fails, send a general request failure
                case _: nException
                    => promise.failure( new RequestError( request, status ) )
                case err: Exception => promise.failure(err)
            }
        }
    }

}

