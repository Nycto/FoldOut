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

    /** Constructs a new RequestBuilder that adds a base path to requests */
    def withBasePath( basePath: String ): RequestBuilder
        = new RequestBuilder( url.withBasePath(basePath), auth )

    /** Returns a prefilled request builder */
    def build(
        method: String,
        key: String,
        params: Map[String, String] = Map(),
        body: Option[nElement] = None
    ): Request = {
        val builder = new AsyncRequestBuilder( method )
            .setUrl( url.url(key, params) )

        // Add in the authorization header
        auth.foreach( _.addHeader( builder ) )

        // Set the request body
        body.map { doc => builder.setBody( doc.json.getBytes("UTF-8") ) }

        builder.build
    }

    /** Returns the document with the given key */
    def get ( key: String, params: Map[String, String] = Map() ): Request
        = build("GET", key, params)

    /** Returns the document with the given key */
    def delete ( key: String, revision: String ): Request
        = build("DELETE", key, Map("rev" -> revision))

    /** Sends the given document using a PUT request */
    def put ( key: String, doc: nElement ): Request
        = build("PUT", key, Map(), Some(doc))

    /** Sends the given document using a PUT request */
    def post ( key: String, doc: nElement ): Request
        = build("POST", key, Map(), Some(doc))

}


