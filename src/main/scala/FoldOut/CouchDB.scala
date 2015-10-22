package com.roundeights.foldout

import com.roundeights.scalon.nElement
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._
import org.slf4j._

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
        maxConnections: Int = 10,
        metrics: Metrics = new Metrics.Void
    )( implicit context: ExecutionContext )
        = new CouchDB( host, port, ssl, auth, timeout, maxConnections, metrics )

    /** Constructs a new CouchDB connection pool to cloudant */
    def cloudant (
        username: String,
        password: String,
        db: Option[String] = None,
        timeout: Int = 5000,
        maxConnections: Int = 10,
        metrics: Metrics = new Metrics.Void
    )( implicit context: ExecutionContext ) = {
        new CouchDB(
            "%s.cloudant.com".format( db.getOrElse(username) ),
            443, true,
            Some(Auth(username, password)),
            timeout, maxConnections, metrics
        )
    }

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
    maxConnections: Int = 10,
    metrics: Metrics = new Metrics.Void,
    logger: Logger = LoggerFactory.getLogger( classOf[CouchDB] )
)( implicit context: ExecutionContext ) {

    /** {@inheritDoc} */
    override def toString = {
        "CouchDB(%s@%s:%d?ssl=%s&timeout=%d&maxconn=%d)".format(
            auth, host, port, ssl, timeout, maxConnections
        )
    }

    /** The internal interface for making requests to CouchDB */
    private val requestor = new Requestor(
        new UrlBuilder( host, port, ssl ),
        auth, timeout, metrics,
        Interceptor.create( logger, timeout.seconds, maxConnections )
    )

    /** Sends a message to close the connection down */
    def close: Unit = requestor.close

    /** Returns a the list of databases */
    def allDBs: Future[Set[String]] = {
        requestor.get("_all_dbs").map {
            (opt: Option[nElement]) => {
                opt.get.asArray.foldLeft( Set[String]() ) {
                    (accum, elem) => accum + elem.asString
                }
            }
        }
    }

    /** Returns the given database */
    def db ( database: String )
        = new Database( requestor.withBasePath(database) )

}


