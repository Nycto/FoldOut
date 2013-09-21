package com.roundeights.foldout

import com.roundeights.scalon._
import com.roundeights.hasher.Hasher

import scala.annotation.tailrec
import java.net.URL
import scala.io.Source
import java.io.{File, FileNotFoundException}

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

    /** Locates a file within a base path */
    private[ViewSpec] def getContent ( base: File, path: String ) = {
        Some( new File(base, path) )
            .filter( _.exists )
            .map( Source.fromFile(_).mkString )
    }

    /** Requires that a file exists */
    private[ViewSpec] def requireContent ( base: File, path: String ) = {
        getContent( base, path ).getOrElse {
            throw new FileNotFoundException(
                "Could not find file: " + new File(base, path)
            )
        }
    }

    /**
     * Loads the map.js and reduce.js from a base directory
     */
    def fromDir( base: File ): ViewSpec = ViewSpec(
        requireContent(base, "map.js"),
        getContent(base, "reduce.js")
    )

    /**
     * Loads the map.js and reduce.js from a base directory
     */
    def fromDir( base: String ): ViewSpec = fromDir( new File(base) )

    /** Returns a file from a jar */
    private def loadJarFile( loader: ClassLoader, path: String ): String = {
        Source.fromURL(
            Option( loader.getResource( path ) ).getOrElse(
                throw new FileNotFoundException(
                    "Could not find Jar resource: %s".format(path)
                )
            )
        ).mkString
    }

    /**
     * Loads a view from jar resource files. This looks for a file named
     * map.js and one named reduce.js (not required).
     */
    def fromJar ( loader: ClassLoader, dir: String ): ViewSpec = {
        val trimmed = dir.dropWhile(_ == '/')
            .reverse.dropWhile(_ == '/').reverse

        val map = loadJarFile( loader, trimmed + "/map.js" )

        Option( loader.getResource( trimmed + "/reduce.js" ) ).map(
            reduceUrl => ViewSpec( map, Source.fromURL(reduceUrl).mkString )
        ).getOrElse(
            ViewSpec( map )
        )
    }

    /**
     * Loads a view from jar resource files. This looks for a file named
     * map.js and one named reduce.js (not required).
     */
    def fromJar ( loaderFrom: Class[_], dir: String ): ViewSpec
        = fromJar( loaderFrom.getClassLoader, dir )


    // A simple regex to match the import directives in a View
    val importMatcher = "(?m)^( *)(?://)? *!import *(.+)$".r
}

/**
 * The specification for a view, which is comprised of a map function and
 * possibly a reduce function
 */
case class ViewSpec ( val map: String, val reduce: Option[String] ) {

    /** Returns this view as json */
    def toJson: nObject = {
        reduce.foldLeft( nObject( "map" -> map ) ) {
            (obj, method) => obj + ("reduce" -> method)
        }
    }

    /** Generates a SHA1 hash of this spec */
    def sha1: String = Hasher( map + reduce.getOrElse("") ).sha1

    /** Post processes this view to include an imported code */
    def processImports ( resolve: (String) => String ): ViewSpec = {
        @tailrec def process( count: Int, code: String ): String = {
            if ( count >= 200 )
                throw new Exception

            val result = ViewSpec.importMatcher.replaceAllIn( code,
                (matched) => matched.group(1) + resolve( matched.group(2) )
            )

            if ( result == code )
                code
            else
                process( count + 1, result )
        }

        ViewSpec( process(0, map), reduce.map( process(0, _) ) )
    }

    /** Post processes this view to include imported code from a jar */
    def processImports ( loader: ClassLoader ): ViewSpec
        = processImports( ViewSpec.loadJarFile(loader, _) )

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
    val language: String = "javascript",
    val views: Map[String, ViewSpec] = Map()
) extends Keyable with Documentable {

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
    def toJson: nObject = {
        val viewObj = views.foldLeft( nObject() ) {
            (accum, pair) => accum + (pair._1 -> pair._2.toJson)
        }

        nObject( "Language" -> language, "views" -> viewObj )
    }

    /** {@inheritDoc} */
    override def toDocKey: String = "_design/" + sha1

    /** {@inheritDoc} */
    override def toDoc: Doc = Doc( toJson + ("_id" -> toDocKey) )

}


