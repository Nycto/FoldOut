package com.roundeights.foldout

import com.roundeights.scalon.nElement

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * A CouchDB database
 */
class Database private[foldout] ( private val requestor: Requestor ) {

    /** Returns the document with the given key */
    def get (
        key: String, params: Map[String, String]
    ): Future[Option[Doc]] = {
        requestor.get( key, params ).map {
            (opt: Option[nElement]) => opt.map {
                elem => Doc( elem.asObject )
            }
        }
    }

    /** Returns the document with the given key */
    def get ( key: String, params: (String, String)* ): Future[Option[Doc]]
        = get( key, Map(params: _*) )

}



