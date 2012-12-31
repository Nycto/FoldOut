package com.roundeights.foldout

import java.net.URLEncoder
import java.net.URL

/**
 * UrlBuilder Companion
 */
private[foldout] object UrlBuilder {

    /** Generates a query string from a list of tuples */
    def toQueryString ( pairs: List[(_, _)] ): String = {
        pairs.map( (pair) => "%s=%s".format(
                URLEncoder.encode(pair._1.toString, "UTF-8"),
                URLEncoder.encode(pair._2.toString, "UTF-8")
            ) )
            .mkString("&")
    }

    /** Generates a query string from a list of tuples*/
    def toQueryString ( pairs: (_, _)* ): String
        = toQueryString( pairs.toList )

    /** Generates a query string from a map of parameters */
    def toQueryString ( pairs: Map[_, _] ): String
        = toQueryString( pairs.toList )

}

/**
 * Constructs URLs for a base host
 */
private[foldout] class UrlBuilder (
    private val host: String,
    private val port: Int,
    private val ssl: Boolean = false
) {

    /** Generates a URL with the given path and query parameters */
    def url ( path: String, query: List[(_, _)] ): String = {
        val url = new URL(
            if ( ssl ) "https" else "http",
            host, port,
            if ( path.startsWith("/") ) path else "/" + path
        )

        if ( query.length != 0 )
            "%s?%s".format( url, UrlBuilder.toQueryString( query ) )
        else
            url.toString
    }

    /** Generates a URL with the given path and query parameters */
    def url ( path: String, query: Map[_, _] ): String
        = url( path, query.toList )

    /** Generates a URL with the given path and query parameters */
    def url ( path: String, query: (_, _)* ): String
        = url( path, query.toList )

}

