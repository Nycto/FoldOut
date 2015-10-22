package com.roundeights.foldout

import com.roundeights.scalon.nElement
import scala.concurrent.{Future, Promise, ExecutionContext}
import scala.concurrent.duration.Duration
import scala.annotation.tailrec
import com.ning.http.client.Request
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent._
import org.slf4j.Logger

/** @see Interceptor */
private[foldout] object Interceptor {

    /** A shared scheduler for all CouchDB instances */
    private[foldout] lazy val scheduler = Executors.newScheduledThreadPool(1)

    /** Creates a new preconfigured interceptor */
    def create (
        logger: Logger,
        timeout: Duration,
        maxConnections: Int
    )(
        implicit context: ExecutionContext
    ): Interceptor = {
        val log = new RequestLogger( logger )
        val timer = new Timeout(timeout)
        val limiter = new RateLimiter(maxConnections)

        return new Interceptor {
            def apply(
                request: Request, executor: => Future[Option[nElement]]
            ): Future[Option[nElement]] = {
                log(request, timer(request, limiter(request, executor)))
            }
        }
    }
}

/** Intercepts the given request to operate on it in some way */
private[foldout] trait Interceptor {

    /** Logs the given request */
    def apply(
        request: Request, executor: => Future[Option[nElement]]
    ): Future[Option[nElement]]
}

/** Inflicts a timeout on the request */
private[foldout] class Timeout (
    private val timeout: Duration
)(
    implicit context: ExecutionContext
) extends Interceptor {

    /** {@inheritDoc} */
    def apply(
        request: Request, executor: => Future[Option[nElement]]
    ): Future[Option[nElement]] = {
        val timer = Promise[Option[nElement]]()

        Interceptor.scheduler.schedule(new Runnable {
            override def run(): Unit = timer.failure(
                new TimeoutException("Request timed out")
            )
        }, timeout.length, timeout.unit)

        Future.firstCompletedOf(timer.future :: executor :: Nil)
    }
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

    /** {@inheritDoc} */
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

/** Throttles requests so only the specified number are active at a time */
private[foldout] class RateLimiter
    ( private val max: Int )
    ( implicit context: ExecutionContext )
extends Interceptor {

    /** A list of requests to run */
    private val queue = new ConcurrentLinkedQueue[() => Unit]

    /** The number of active requests */
    private val inflight = new AtomicInteger(0)

    /** Attempts to execute the next function */
    @tailrec private def runNext(): Unit = {
        val active = inflight.get
        val next = queue.peek

        if ( active >= max || next == null ) {
            return
        }

        if ( inflight.compareAndSet(active, active + 1) ) {
            if ( queue.remove(next) ) {
                return next()
            }
            else {
                inflight.getAndDecrement
            }
        }

        runNext()
    }

    /** {@inheritDoc} */
    def apply(
        request: Request, executor: => Future[Option[nElement]]
    ): Future[Option[nElement]] = {

        val result = Promise[Option[nElement]]()

        // When this future is done, succesful or not, check to see if there
        // is another request to execute
        result.future.onComplete {
            case _ => {
                inflight.decrementAndGet
                runNext()
            }
        }

        queue.add(() => result.completeWith(executor))

        runNext()

        result.future
    }
}


