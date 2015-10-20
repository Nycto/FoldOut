package com.roundeights.foldout

import com.roundeights.scalon.nElement
import scala.concurrent.{Future, ExecutionContext}
import com.ning.http.client.Request
import java.util.concurrent.atomic.AtomicInteger
import org.slf4j.Logger

/** @see Interceptor */
private[foldout] object Interceptor {

    /** Creates a new preconfigured interceptor */
    def create (
        logger: Logger
    )(
        implicit context: ExecutionContext
    ): Interceptor = {
        new RequestLogger( logger )
    }
}

/** Intercepts the given request to operate on it in some way */
private[foldout] trait Interceptor {

    /** Logs the given request */
    def apply(
        request: Request, executor: => Future[Option[nElement]]
    ): Future[Option[nElement]]
}

/** Internal logger for requests */
private[foldout] class RequestLogger
    ( private val logger: Logger )
    ( implicit context: ExecutionContext )
extends Interceptor {

    /**
     * An internal tracker for generating request IDs. These are used to
     * associate multiple log entires with the same request
     */
    private val counter = new AtomicInteger( 0 )

    /** Logs the given request */
    def apply(
        request: Request, executor: => Future[Option[nElement]]
    ): Future[Option[nElement]] = {
        val requestID = counter.incrementAndGet

        logger.debug("R#%d %s %s".format(
            requestID, request.getMethod, request.getUrl
        ))

        val future = executor

        future.onSuccess {
            case Some(_) => logger.debug("R#%d Doc found".format(requestID))
            case None => logger.debug("R#%d Doc Missing".format(requestID))
        }

        future.onFailure {
            case _: RevisionConflict
                => logger.debug( "R#%d Revision Conflict".format(requestID) )
            case err: Throwable => logger.error(
                "R#%d Error: %s %s %s".format(
                    requestID,
                    request.getMethod, request.getUrl,
                    err
                )
            )
        }

        future
    }

}

