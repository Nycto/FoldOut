package com.roundeights.foldout

import scala.concurrent.Promise
import scala.util.{Try, Success, Failure}

/** @see Metrics */
object Metrics {

    /** Wraps a promise in a metric */
    class PromiseTimer[T] (
        private val timer: Timer,
        private val promise: Promise[T]
    ) {

        /** The future associated with this promise */
        val future = promise.future

        /** Fulfills a promise */
        def success( value: T ): Unit = {
            timer.success
            promise.success(value)
        }

        /** Fulfills a promise and marks the timer as 'notFound' */
        def notFound( value: T ): Unit = {
            timer.notFound
            promise.success(value)
        }

        /** Fulfills a promise with an exception */
        def conflict( err: Throwable ): Unit = {
            timer.conflict
            promise.failure(err)
        }

        /** Fulfills a promise with an exception */
        def failure( err: Throwable ): Unit = {
            timer.failed
            promise.failure(err)
        }

        /** Fulfills a promise with an exception */
        def complete( result: Try[T] ): Unit = result match {
            case Success(value) => success(value)
            case Failure(err) => failure(err)
        }
    }

    /** An interface for marking that a request is complete */
    trait Timer {

        /** Marks that this metric is completed successfully */
        def success: Unit

        /** Marks that this metric failed */
        def failed: Unit

        /** Called when a value is not found */
        def notFound: Unit

        /** Called when an update encounters a conflict */
        def conflict: Unit

        /** Called when data is returned */
        def dataReceived: Unit

        /** Called when the full body is returned */
        def bodyComplete: Unit

        /** Wraps a promise in a timer */
        def apply[T]( promise: Promise[T] ) = new PromiseTimer(this, promise)
    }

    /** A void metric */
    class Void extends Metrics {

        /** {@inheritDoc} */
        override def start: Timer = new Timer {

            /** {@inheritDoc} */
            override def success = {}

            /** {@inheritDoc} */
            override def failed = {}

            /** {@inheritDoc} */
            override def notFound = {}

            /** {@inheritDoc} */
            override def conflict = {}

            /** {@inheritDoc} */
            override def dataReceived = {}

            /** {@inheritDoc} */
            override def bodyComplete = {}
        }
    }
}

/**
 * An interface for tracking the timing data around requests
 */
trait Metrics {

    /** Caled at the start of a request */
    def start: Metrics.Timer
}

