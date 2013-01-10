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
    def put ( doc: Doc ): Future[Written]
        = Written( requestor.put( doc.id, doc.obj ) )

    /** Deletes the given key and revision*/
    def delete ( key: String, revision: String ): Future[Written]
        = Written( requestor.delete( key, revision ) )

    /** Deletes the given document */
    def delete ( doc: Doc ): Future[Written] = delete( doc.id, doc.rev )

    /** Posts the given document */
    def post ( doc: Doc ): Future[Written]
        = Written( requestor.post(doc.obj) )

    /** Returns all the documents in a database */
    def allDocs: BulkRead = new BulkRead( requestor, "_all_docs" )

    /** Returns access to the given design */
    def design ( name: String ): Design
        = new Design( requestor.withBasePath( "_design/%s".format(name) ) )

    /** Returns the given view */
    def view ( designName: String, viewName: String ): BulkRead
        = design( designName ).view( viewName )

}


