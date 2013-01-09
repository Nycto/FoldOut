package com.roundeights.foldout

import com.roundeights.scalon.nElement

import scala.concurrent.{Promise, Future}
import scala.concurrent.ExecutionContext.Implicits.global

import com.ning.http.client.{ AsyncHttpClientConfig, AsyncHttpClient, Request }

/**
 * The internal interface for making raw requests to the CouchDB server
 */
private[foldout] class Requestor (
    private val builder: RequestBuilder,
    private val client: AsyncHttpClient
) {

    /** Alternate constructor that puts together an async client */
    def this (
        url: UrlBuilder, auth: Option[Auth],
        timeout: Int, maxConnections: Int
    ) = this(
        new RequestBuilder( url, auth ),
        new AsyncHttpClient(
            new AsyncHttpClientConfig.Builder()
                .setCompressionEnabled(true)
                .setFollowRedirects(false)
                .setAllowPoolingConnection(true)
                .setRequestTimeoutInMs( timeout )
                .setMaximumConnectionsPerHost( maxConnections )
                .build()
        )
    )

    /** Constructs a new Requestor that adds a base path to requests */
    def withBasePath( basePath: String ): Requestor
        = new Requestor( builder.withBasePath(basePath), client )

    /** Sends a message to close the connection down */
    def close: Unit = client.close

    /** Executes the given request and returns a promise */
    def execute ( request: Request ): Future[Option[nElement]] = {
        val promise = Promise[Option[nElement]]()
        client.executeRequest( request, new Asynchronizer( promise ) );
        promise.future
    }

    /** Returns the document with the given key */
    def get (
        key: String, params: Map[String, String] = Map()
    ): Future[Option[nElement]]
        = execute( builder.get(key, params) )

    /** Returns the document with the given key */
    def put( key: String, doc: nElement ): Future[Option[nElement]]
        = execute( builder.put(key, doc) )

}


