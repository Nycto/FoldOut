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

    /** Puts the given document */
    def put ( doc: Doc ): Future[Updated] = {
        requestor.put( doc.id, doc.obj ).map {
            opt => new Updated(
                opt.getOrElse( throw new RequestError(
                    "PUT request did not return a valid response"
                ))
            )
        }
    }

    /** Returns all the documents in a database */
    def allDocs: BulkRead = new BulkRead( requestor, "_all_docs" )

    /** Returns access to the given design */
    def design ( name: String ): Design
        = new Design( requestor.withBasePath( "_design/%s".format(name) ) )

    /** Returns the given view */
    def view ( designName: String, viewName: String ): BulkRead
        = design( designName ).view( viewName )

}


