package com.roundeights.foldout

import com.roundeights.scalon._
import com.roundeights.hasher.Hasher

/**
 * ViewSpec Companion
 */
object ViewSpec {

    /** Constructor */
    def apply ( map: String, reduce: String )
        = new ViewSpec( map, Some(reduce) )

    /** Constructor */
    def apply ( map: String )
        = new ViewSpec( map, None )

    /** Constructor */
    def apply ( data: nElement ) = {
        val obj = data.asObject
        new ViewSpec( obj.str("map"), obj.str_?("reduce") )
    }

}

/**
 * The specification for a view, which is comprised of a map function and
 * possibly a reduce function
 */
case class ViewSpec ( val map: String, val reduce: Option[String] ) {

    /** Returns this view as json */
    def toJson: nElement = {
        reduce.foldLeft( nObject( "map" -> map ) ) {
            (obj, method) => obj + ("reduce" -> method)
        }
    }

    /** Generates a SHA1 hash of this spec */
    def sha1: String = Hasher( map + reduce.getOrElse("") ).sha1

}

/**
 * Design Companion
 */
object DesignSpec {

    /** Constructor */
    def apply ( language: String, views: (String, ViewSpec)* )
        = new DesignSpec( language, Map(views: _*) )

    /** Constructor */
    def apply ( views: Map[String, ViewSpec] )
        = new DesignSpec( "Javascript", views )

    /** Constructor */
    def apply ( views: (String, ViewSpec)* )
        = new DesignSpec( "Javascript", Map(views: _*) )

    /** Constructor */
    def apply ( json: nElement ) = {
        val obj = json.asObject
        new DesignSpec(
            obj.str_?("Language").getOrElse("JavaScript"),
            obj.obj("views").foldLeft( Map[String, ViewSpec]() ) {
                (accum, pair) => accum + (( pair._1, ViewSpec(pair._2) ))
            }
        )
    }

}

/**
 * The specification for a design document, which is comprised of a
 * set of views
 */
case class DesignSpec (
    val language: String,
    val views: Map[String, ViewSpec]
) {

    /** Generates a SHA1 hash of this spec */
    lazy val sha1: String = {
        views.keys.toList.sorted.foldLeft( Hasher( language ) ) {
            (hash, key) => hash.salt(key).salt( views(key).sha1 )
        }.sha1
    }

    /** Adds a new view to this design */
    def + ( view: (String, ViewSpec) ): DesignSpec
        = new DesignSpec( language, views + view )

    /** Combines two DesignSpecs */
    def ++ ( other: DesignSpec ): DesignSpec
        = new DesignSpec( language, other.views ++ views )

    /** Returns this view as json */
    def toJson: nElement = {
        val viewObj = views.foldLeft( nObject() ) {
            (accum, pair) => accum + (pair._1 -> pair._2.toJson)
        }

        nObject( "Language" -> language, "views" -> viewObj )
    }

}


