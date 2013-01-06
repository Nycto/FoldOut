package com.roundeights.foldout

import com.roundeights.scalon.nElement

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * A CouchDB database
 */
class Database private[foldout] ( private val requestor: Requestor ) {

    /** Returns the document with the given key */
    def get ( key: String ): Future[Option[Doc]] = {
        requestor.get( key ).map {
            (opt: Option[nElement]) => opt.map {
                elem => Doc( elem.asObject )
            }
        }
    }

    /** Returns all the documents in a database */
    def allDocs: BulkRead = new BulkRead( requestor, "_all_docs" )

}



