package com.roundeights.foldout

import scala.concurrent.Promise

/** @see Metrics */
object Metrics {

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

