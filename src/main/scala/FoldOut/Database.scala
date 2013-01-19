package com.roundeights.foldout

import com.roundeights.scalon.nElement

import scala.concurrent.{Future, ExecutionContext}

/**
 * A trait for objects that can convert themselves to a document
 */
trait Documentable {

    /** Returns this object as a document */
    def toDoc: Doc
}

/**
 * A trait for objects that can convert themselves to a document key
 */
trait Keyable {

    /** Returns this object as a document */
    def toDocKey: String
}

/**
 * A CouchDB database
 */
class Database private[foldout] ( private val requestor: Requestor ) {

    /** Returns the document with the given key */
    def get
        ( key: String )
        ( implicit context: ExecutionContext )
    : Future[Option[Doc]] = {
        requestor.get( key ).map {
            (opt: Option[nElement]) => opt.map {
                elem => Doc( elem.asObject )
            }
        }
    }

    /** Returns the document with the given key */
    def get
        ( key: Keyable )
        ( implicit context: ExecutionContext )
    : Future[Option[Doc]]
        = get( key.toDocKey )

    /** Returns an updated version of the given document*/
    def get
        ( doc: Doc )
        ( implicit context: ExecutionContext )
    : Future[Option[Doc]]
        = get( doc.id )

    /** Puts the given document */
    def put ( doc: Doc )( implicit context: ExecutionContext ): Future[Written]
        = Written( requestor.put( doc.id, doc.obj ) )

    /** Puts the given document */
    def put
        ( doc: Documentable )
        ( implicit context: ExecutionContext )
    : Future[Written]
        = put( doc.toDoc )

    /** Deletes the given key and revision*/
    def delete
        ( key: String, revision: String )
        ( implicit context: ExecutionContext )
    : Future[Written]
        = Written( requestor.delete( key, revision ) )

    /** Deletes the given document */
    def delete
        ( doc: Doc )
        ( implicit context: ExecutionContext )
    : Future[Written]
        = delete( doc.id, doc.rev )

    /** Puts the given document */
    def delete
        ( doc: Documentable )
        ( implicit context: ExecutionContext )
    : Future[Written]
        = delete( doc.toDoc )

    /** Posts the given document */
    def post
        ( doc: Doc )
        ( implicit context: ExecutionContext )
    : Future[Written]
        = Written( requestor.post(doc.obj) )

    /** Puts the given document */
    def post
        ( doc: Documentable )
        ( implicit context: ExecutionContext )
    : Future[Written]
        = post( doc.toDoc )

    /** Returns all the documents in a database */
    def allDocs: BulkRead = new BulkRead( requestor, "_all_docs" )

    /** Returns access to the given design */
    def design ( name: String ): Design
        = new Design( requestor.withBasePath( "_design/%s".format(name) ) )

    /** Returns the given view */
    def view ( designName: String, viewName: String ): BulkRead
        = design( designName ).view( viewName )

    /** Puts a value, writing over whatever is already there */
    def push
        ( doc: Doc, retries: Int = 3 )
        ( implicit context: ExecutionContext )
    : Future[Written] = {
        get( doc ).flatMap {
            _ match {
                case None => put( doc ) recoverWith {
                    case _: RevisionConflict if retries > 0
                        => push(doc, retries - 1)
                }

                case Some( fromDb )
                    if (fromDb - "_rev") == (doc - "_rev")
                    => Future.successful( Written(fromDb.rev, fromDb.id) )

                case Some( fromDb ) => {
                    put( doc + ("_rev" -> fromDb.rev) ).recoverWith {
                        case _: RevisionConflict if retries > 0
                            => push( doc, retries - 1 )
                    }
                }
            }
        }
    }

    /** Sets a value, writing over whatever is already there */
    def push
        ( doc: Documentable )
        ( implicit context: ExecutionContext )
    : Future[Written]
        = push( doc.toDoc )

    /** Creates this database */
    def create ( implicit context: ExecutionContext ): Future[Unit]
        = requestor.put("/").map { (v) => () }

    /** Deletes this database */
    def delete ( implicit context: ExecutionContext ): Future[Unit]
        = requestor.delete("/").map { (v) => () }

}


