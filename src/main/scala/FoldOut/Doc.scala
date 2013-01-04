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

}

/**
 * A list of results from a view query
 */
class RowList ( doc: nObject ) extends Iterable[Doc] {

    /** Creates a new document from a JSON string */
    def this ( json: String ) = this( nParser.jsonObj(json) )

    /** The number of rows in this list */
    val totalRows = {
        val value = doc.int("total_rows")
        require(value.isDefined, "total_rows key is missing from doc")
        value.get
    }

    /** The offset of the start of this result set */
    val offset = {
        val value = doc.int("offset")
        require(value.isDefined, "offset key is missing from doc")
        value.get
    }

    /** The list of rows */
    private val rows = {
        val value = doc.ary("rows")
        require(value.isDefined, "rows key is missing from doc")
        value.get
    }

    /** {@inheritDoc} */
    override def iterator: Iterator[Doc] = new Iterator[Doc] {
        private val entries = rows.iterator
        override def hasNext = entries.hasNext
        override def next = Doc( entries.next.asObject )
    }

    /** {@inheritDoc} */
    override def toString = "RowList(%s)".format( rows.toString )

}

