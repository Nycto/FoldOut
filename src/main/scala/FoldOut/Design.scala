package com.roundeights.foldout

import scala.concurrent.{Future, ExecutionContext}

/**
 * A database design
 */
class Design private[foldout] ( private val requestor: Requestor ) {

    /** Returns all the documents in a database */
    def view( name: String ): BulkRead
        = new BulkRead( requestor, "_view/%s".format(name) )

    /** Returns the raw specs for this design */
    def spec (
        implicit context: ExecutionContext
    ): Future[Option[DesignSpec]] = {
        requestor.get("/", Map()).map {
            opt => opt.map { DesignSpec(_) }
        }
    }

}

