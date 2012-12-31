package com.roundeights.foldout

import scala.concurrent.{Promise, Future}

import com.ning.http.client.{
    AsyncHttpClientConfig, AsyncHttpClient, RequestBuilder
}

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

    /** Generates URLs for the given parameters */
    private val url = new UrlBuilder( host, port, ssl )

    /** The HTTP Client for making requests */
    private val client = new AsyncHttpClient(
        new AsyncHttpClientConfig.Builder()
            .setCompressionEnabled(true)
            .setFollowRedirects(false)
            .setAllowPoolingConnection(true)
            .setRequestTimeoutInMs( timeout )
            .setMaximumConnectionsPerHost( maxConnections )
            .build()
    )

    /** Sends a message to close the connection down */
    def close: Unit = client.close

    /** Returns a prefilled request builder */
    private def request(
        method: String, key: String, params: Map[String, String]
    ): RequestBuilder = {
        val builder = new RequestBuilder( method )
            .setUrl( url.url(key, params) )

        // Add in the authorization header
        auth.foreach( _.addHeader( builder ) )

        builder
    }

    /** Returns the document with the given key */
    def get (
        key: String, params: Map[String, String]
    ): Future[Option[Doc]] = {

        val promise = Promise[Option[Doc]]()

        client.executeRequest(
            request("GET", key, params).build(),
            new Asynchronizer( promise )
        );

        promise.future
    }

    /** Returns the document with the given key */
    def get ( key: String, params: (String, String)* ): Future[Option[Doc]]
        = get( key, Map(params: _*) )

}


