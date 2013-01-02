package com.roundeights.foldout

import com.roundeights.scalon.nElement

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * CouchDB companion
 */
object CouchDB {

    /** Constructs a new CouchDB connection pool */
    def apply (
        host: String,
        port: Int,
        ssl: Boolean = true,
        auth: Option[Auth] = None,
        timeout: Int = 5000,
        maxConnections: Int = 10
    ) = new CouchDB( host, port, ssl, auth, timeout, maxConnections )

    /** Constructs a new CouchDB connection pool to cloudant */
    def cloudant (
        username: String,
        password: String,
        db: Option[String] = None,
        timeout: Int = 5000,
        maxConnections: Int = 10
    ) = new CouchDB(
        "%s.cloudant.com".format( db.getOrElse(username) ),
        443, true,
        Some(Auth(username, password)),
        timeout, maxConnections
    )

}

/**
 * The connection pool to a CouchDB server
 */
class CouchDB (
    host: String,
    port: Int,
    ssl: Boolean = true,
    auth: Option[Auth] = None,
    timeout: Int = 5000,
    maxConnections: Int = 10
) {

    /** The internal interface for making requests to CouchDB */
    private val requestor = new Requestor(
        new UrlBuilder( host, port, ssl ),
        auth, timeout, maxConnections
    )

    /** Sends a message to close the connection down */
    def close: Unit = requestor.close

    /** Returns the document with the given key */
    def get (
        key: String, params: Map[String, String]
    ): Future[Option[nElement]] = requestor.get( key, params )

    /** Returns the document with the given key */
    def get (
        key: String, params: (String, String)*
    ): Future[Option[nElement]] = get( key, Map(params: _*) )

    /** Returns a the list of databases */
    def allDBs: Future[Set[String]] = requestor.get("_all_dbs").map {
        (opt: Option[nElement]) => opt.get.asArray.foldLeft( Set[String]() ) {
            (accum, elem) => accum + elem.asString
        }
    }

    /** Returns the given database */
    def db ( database: String )
        = new Database( requestor.withBasePath(database) )

}


