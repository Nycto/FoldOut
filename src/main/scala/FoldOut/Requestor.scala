package com.roundeights.foldout

import com.roundeights.scalon.nElement
import scala.concurrent.{Promise, Future, ExecutionContext}
import com.ning.http.client.{ AsyncHttpClientConfig, AsyncHttpClient, Request }
import org.slf4j.Logger

/** @see Requestor */
object Requestor {

    /** A requestor that is preconfigured to execute a request */
    trait Preset {

        /** Executes this preset request */
        def apply ( params: Map[String, String] ): Future[Option[nElement]]

        /** Returns a string description of this preset request */
        def describe ( params: Map[String, String] ): String
    }
}

/**
 * The internal interface for making raw requests to the CouchDB server
 */
private[foldout] class Requestor (
    private val builder: RequestBuilder,
    private val client: AsyncHttpClient,
    private val metrics: Metrics,
    private val intercept: Interceptor
) {

    /** Alternate constructor that puts together an async client */
    def this (
        url: UrlBuilder, auth: Option[Auth], timeout: Int,
        metrics: Metrics, intercept: Interceptor
    )( implicit context: ExecutionContext ) = this(
        new RequestBuilder( url, auth ),
        new AsyncHttpClient(
            new AsyncHttpClientConfig.Builder()
                .setMaxRedirects(3)
                .setAllowPoolingConnections(true)
                .setAllowPoolingSslConnections(true)
                .setRequestTimeout( timeout )
                .build()
        ),
        metrics,
        intercept
    )

    /** Returns the base path of this Requestor */
    def rootPath = builder.rootPath.getOrElse("/")

    /** Constructs a new Requestor that adds a base path to requests */
    def withBasePath( basePath: String ): Requestor = new Requestor(
        builder.withBasePath(basePath), client, metrics, intercept
    )

    /** Sends a message to close the connection down */
    def close: Unit = client.close

    /** Executes the given request and returns a promise */
    def execute ( request: Request ): Future[Option[nElement]] = {
        intercept( request, {
            val async = new Asynchronizer( request, metrics )
            client.executeRequest( request, async )
            async.future
        } )
    }

    /** Returns the document with the given key */
    def get (
        key: String, params: Map[String, String] = Map()
    ): Future[Option[nElement]]
        = execute( builder.get(key, params) )

    /** Builds a preset request for a GET */
    def presetGet( key: String ) = new Requestor.Preset {
        override def describe ( params: Map[String, String] )
            = "GET " + builder.buildUrl( Some(key), params )
        override def apply ( params: Map[String, String] ) = get(key, params)
    }

    /** Sends a put request */
    def put( key: String ): Future[Option[nElement]]
        = execute( builder.put(key) )

    /** Puts the given document */
    def put( key: String, doc: nElement ): Future[Option[nElement]]
        = execute( builder.put(key, doc) )

    /** Deletes the given key/revision */
    def delete( key: String, revision: String ): Future[Option[nElement]]
        = execute( builder.delete(key, revision) )

    /** Deletes the given key */
    def delete( key: String ): Future[Option[nElement]]
        = execute( builder.delete(key) )

    /** Posts the given document */
    def post( doc: nElement ): Future[Option[nElement]]
        = execute( builder.post(doc) )

    /** Posts the given document */
    def post(
        key: String,
        doc: nElement,
        params: Map[String, String]
    ): Future[Option[nElement]]
        = execute( builder.post(key, doc, params) )

    /** Builds a preset request for a POST */
    def presetPost( key: String, doc: nElement ) = new Requestor.Preset {
        override def describe ( params: Map[String, String] )
            = "POST " + builder.buildUrl( Some(key), params )
        override def apply ( params: Map[String, String] )
            = post(key, doc, params)
    }

}


