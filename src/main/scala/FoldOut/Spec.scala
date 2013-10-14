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

    /** Joins two file paths */
    private def join( left: String, right: String ) = {
        val base = if ( left.endsWith("/") ) left else left + "/"
        base + right.dropWhile(_ == '/')
    }

    /** Returns a file from a jar */
    private def loadJarFile(
        loader: ClassLoader,
        baseDir: String,
        path: String
    ): String = {
        Source.fromURL(
            Option( loader.getResource( join(baseDir, path) ) ).getOrElse(
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

        val map = loadJarFile( loader, trimmed, "map.js" )

        Option( loader.getResource( join(trimmed, "reduce.js") ) ).map(
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
            if ( count >= 50 )
                throw new StackOverflowError("Import recursion too deep")

            val result = ViewSpec.importMatcher.replaceAllIn( code,
                (matched) => matched.group(1) + resolve( matched.group(2) )
            )

            if ( ViewSpec.importMatcher.findFirstIn(result).isEmpty )
                result
            else
                process( count + 1, result )
        }

        ViewSpec( process(0, map), reduce.map( process(0, _) ) )
    }

    /** Post processes this view to include imported code from a jar */
    def processImports ( loader: ClassLoader, dir: String ): ViewSpec
        = processImports( ViewSpec.loadJarFile(loader, dir, _) )

    /** Post processes this view to include imported code from a jar */
    def processImports ( clazz: Class[_], dir: String ): ViewSpec
        = processImports( clazz.getClassLoader, dir )

    /** Post processes this view to include imported code from a directory */
    def processImports ( baseDir: File ): ViewSpec
        = processImports( ViewSpec.requireContent(baseDir, _) )

    /** Post processes this view to include imported code from a directory */
    def processImports ( baseDir: String ): ViewSpec
        = processImports( ViewSpec.requireContent( new File(baseDir), _ ) )
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

    /** Creates a design spec from a list of jar resource paths */
    def fromJar (
        loader: ClassLoader,
        views: (String, String)*
    ): DesignSpec = {
        views.foldLeft( DesignSpec() ) {
            (accum, view) => accum + (
                view._1 -> ViewSpec.fromJar( loader, view._2 )
            )
        }
    }

    /** Creates a design spec from a list of jar resource paths */
    def fromJar (
        loaderFrom: Class[_],
        views: (String, String)*
    ): DesignSpec = {
        fromJar( loaderFrom.getClassLoader, views:_* )
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

    /** Generates a new spec using a callback to mutate the views */
    def map ( callback: (ViewSpec) => ViewSpec ): DesignSpec = {
        DesignSpec( language, views.map {
            (pair) => pair._1 -> callback(pair._2)
        } )
    }

    /** Post processes the views in this design to include imported code */
    def processImports ( resolve: (String) => String ): DesignSpec
        = map( _.processImports(resolve) )

    /** Post processes the views in this design to imported code from a jar */
    def processImports ( loader: ClassLoader, dir: String ): DesignSpec
        = map( _.processImports(loader, dir) )

    /** Post processes the views in this design to imported code from a jar */
    def processImports ( clazz: Class[_], dir: String ): DesignSpec
        = map( _.processImports(clazz, dir) )

    /** Post processes the views in this design to imported code from a dir */
    def processImports ( baseDir: File ): DesignSpec
        = map( _.processImports(baseDir) )

    /** Post processes the views in this design to imported code from a dir */
    def processImports ( baseDir: String ): DesignSpec
        = map( _.processImports(baseDir) )
}


