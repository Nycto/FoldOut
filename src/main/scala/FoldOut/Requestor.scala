package com.roundeights.foldout

import com.roundeights.scalon.nElement

import scala.concurrent.{Promise, Future}
import scala.concurrent.ExecutionContext.Implicits.global

import com.ning.http.client.{
    AsyncHttpClientConfig, AsyncHttpClient, RequestBuilder
}

/**
 * The internal interface for making raw requests to the CouchDB server
 */
private[foldout] class Requestor (
    private val url: UrlBuilder,
    private val auth: Option[Auth],
    private val client: AsyncHttpClient
) {

    /** Alternate constructor that puts together an async client */
    def this (
        url: UrlBuilder, auth: Option[Auth],
        timeout: Int, maxConnections: Int
    ) = this(
        url, auth,
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

    /** Sends a message to close the connection down */
    def close: Unit = client.close

    /** Returns a prefilled request builder */
    def buildRequest(
        method: String, key: String, params: Map[String, String]
    ): RequestBuilder = {
        val builder = new RequestBuilder( method )
            .setUrl( url.url(key, params) )

        // Add in the authorization header
        auth.foreach( _.addHeader( builder ) )

        builder
    }

    /** Executes the given request and returns a promise */
    def execute ( request: RequestBuilder ): Future[Option[nElement]] = {
        val promise = Promise[Option[nElement]]()
        client.executeRequest( request.build(), new Asynchronizer( promise ) );
        promise.future
    }

    /** Returns the document with the given key */
    def get (
        key: String, params: Map[String, String] = Map()
    ): Future[Option[nElement]]
        = execute( buildRequest("GET", key, params) )

}


