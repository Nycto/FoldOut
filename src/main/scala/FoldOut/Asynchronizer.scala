package com.roundeights.foldout

import com.roundeights.scalon.{nElement, nParser, nException}

import java.util.concurrent.atomic.{AtomicReference, AtomicBoolean}
import scala.concurrent.{Promise, ExecutionException}
import scala.util.{Try, Success, Failure}

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
    private val request: Request,
    private val metrics: Metrics = new Metrics.Void
) extends AsyncHandler[Unit] {

    /** The timer for tracking metrics for this specific request */
    private val timer = metrics.start

    /** Indicates whether any data has been received */
    private val dataReceived = new AtomicBoolean(false)

    /** The promise that will contain the output of this request */
    private val result = new Result

    /** The future for accessing the result of this request */
    val future = result.future

    /** The error status, if one was encountered */
    private val status = new AtomicReference[Option[HttpResponseStatus]](None)

    /** Collects the body of the request as it is received */
    private val body = new StringBuilder()

    /** Manages the completion of this request */
    class Result {

        /** The promise to complete */
        private val promise = Promise[Option[nElement]]

        /** The future associated with this promise */
        val future = promise.future

        /** Fulfills a promise */
        def success( value: Option[nElement] ): Unit = {
            timer.success
            promise.success(value)
        }

        /** Fulfills a promise and marks the timer as 'notFound' */
        def notFound: Unit = {
            timer.notFound
            promise.success( None )
        }

        /** Fulfills a promise with an exception */
        def conflict: Unit = {
            timer.conflict
            promise.failure( new RevisionConflict( request.getUrl ) )
        }

        /** Fulfills a promise with an exception */
        def failure( err: Throwable ): Unit = {
            timer.failed
            promise.failure(err match {
                // The scala Promise lib tries to do this too, but doesn't
                // do a great job of maintaining the original messaging
                case _: Error => {
                    val error = new ExecutionException(
                        err.getClass.getName + ": " + err.getMessage, err )
                    error.setStackTrace( err.getStackTrace )
                    error
                }
                case _: Throwable => err
            })
        }

        /** Fulfills a promise with an exception */
        def complete( result: Try[Option[nElement]] ): Unit = result match {
            case Success(value) => success(value)
            case Failure(err) => failure(err)
        }
    }

    /** {@inheritDoc} */
    override def onThrowable( t: Throwable ): Unit = result.failure(t)

    /** {@inheritDoc} */
    override def onBodyPartReceived(
        bodyPart: HttpResponseBodyPart
    ): STATE = {
        if ( dataReceived.compareAndSet(false, true) ) {
            timer.dataReceived
        }
        body.append( new String(bodyPart.getBodyPartBytes, "UTF-8") )
        STATE.CONTINUE
    }

    /** {@inheritDoc} */
    override def onStatusReceived(
        responseStatus: HttpResponseStatus
    ): STATE = responseStatus.getStatusCode match {
        case 404 => {
            result.notFound
            STATE.ABORT
        }
        case 409 => {
            result.conflict
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
    override def onCompleted(): Unit = {
        timer.bodyComplete
        status.get match {
            case None => result.complete( Try {
                Some( nParser.json(body.toString) )
            } )
            case Some(status) => {
                try {
                    val json = nParser.jsonObj(body.toString)
                    result.failure( new RequestError( request,
                        "%s: %s".format(json.str("error"), json.str("reason"))
                    ) )
                }
                catch {
                    // If parsing the JSON fails, send a general request failure
                    case _: nException
                        => result.failure(new RequestError(request, status))
                    case err: Exception => result.failure(err)
                }
            }
        }
    }

}

