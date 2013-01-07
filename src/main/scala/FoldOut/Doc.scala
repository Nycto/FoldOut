package com.roundeights.foldout

import com.roundeights.scalon.{nObject, nElement, nParser}

/** A CouchDB Document */
object Doc {

    /** Creates a new document */
    def apply ( obj: nObject ): Doc = new Doc( obj )

    /** Creates a new document from a JSON string */
    def apply ( json: String ): Doc = new Doc( nParser.jsonObj(json) )

}

/**
 * A CouchDB Document
 *
 * @param obj The underlying notation object that contains the data for
 *      this document
 */
class Doc ( val obj: nObject ) extends nObject.Interface[Doc] {

    /** Returns this document as JSON */
    def json: String = obj.json

    /** {@inheritDoc} */
    override protected def build ( o: nObject ): Doc = new Doc(o)

    /** {@inheritDoc} */
    override def iterator: Iterator[(String, nElement)] = obj.iterator

    /** {@inheritDoc} */
    override def get( key: String ): Option[nElement] = obj.get( key )

    /** {@inheritDoc} */
    override def toMap: Map[String, nElement] = obj.toMap

    /** Returns the ID from this document */
    def id: String  = obj.str("_id").getOrElse( throw MissingKey.id )

    /** Returns the revision of this document */
    def rev: String  = obj.str("_rev").getOrElse( throw MissingKey.rev )

}

