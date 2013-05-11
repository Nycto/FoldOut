package com.roundeights.foldout

import com.roundeights.scalon.nElement

import com.ning.http.client.{ RequestBuilder => AsyncRequestBuilder }
import com.ning.http.client.Request

/**
 * Generates request objects
 */
private[foldout] class RequestBuilder (
    private val url: UrlBuilder,
    private val auth: Option[Auth]
) {

    /** Returns the base path of this Request Builder */
    def rootPath = url.rootPath

    /** Constructs a new RequestBuilder that adds a base path to requests */
    def withBasePath( basePath: String ): RequestBuilder
        = new RequestBuilder( url.withBasePath(basePath), auth )

    /** Returns a prefilled request builder */
    private def build(
        method: String,
        key: Option[String],
        params: Map[String, String] = Map(),
        body: Option[nElement] = None
    ): Request = {
        val builder = new AsyncRequestBuilder( method )
            .setUrl( url.url( key, params) )
            .addHeader( "Content-Type", "application/json;charset=utf-8" )

        // Add in the authorization header
        auth.foreach( _.addHeader( builder ) )

        // Set the request body
        body.map { doc => builder.setBody( doc.json.getBytes("UTF-8") ) }

        builder.build
    }

    /** Returns the document with the given key */
    def get ( key: String, params: Map[String, String] = Map() ): Request
        = build("GET", Some(key), params)

    /** Deletes the document with the given key */
    def delete ( key: String, revision: String ): Request
        = build("DELETE", Some(key), Map("rev" -> revision))

    /** Sends a delete for the given path */
    def delete ( key: String ): Request
        = build("DELETE", Some(key), Map(), None)

    /** Sends a PUT request without a body */
    def put ( key: String ): Request
        = build("PUT", Some(key), Map(), None)

    /** Sends the given document using a PUT request */
    def put ( key: String, doc: nElement ): Request
        = build("PUT", Some(key), Map(), Some(doc))

    /** Sends the given document using a PUT request */
    def post ( doc: nElement ): Request
        = build("POST", None, Map(), Some(doc))

    /** Sends the given document using a PUT request */
    def post (
        key: String,
        doc: nElement,
        params: Map[String, String]
    ): Request = build("POST", Some(key), params, Some(doc))

}


