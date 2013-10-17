package com.roundeights.foldout

import com.roundeights.scalon.{nObject, nElement, nParser}


/**
 * A Row from a bulk read
 */
class Row private[foldout] (
    val key: nElement,
    val rowID: Option[String],
    obj: nObject
) extends Doc(obj) {

    /** Constructs a row from a notation element */
    private[foldout] def this ( elem: nElement ) = this(
        elem.asObject.get("key"),
        elem.asObject.str_?("id"),
        elem.asObject.obj("value")
    )

    /** {@inheritDoc} */
    override def id: String = rowID.getOrElse(
        throw new NoSuchElementException("Row does not have an ID")
    )

    /** {@inheritDoc} */
    override def toString: String
        = "Row(%s, %s, %s)".format(key.toString, rowID, obj.toString)

    /** {@inheritDoc} */
    override def toJson
        = nObject( "key" -> key, "id" -> rowID, "value" -> super.toJson )
}


/**
 * RowList Companion
 */
object RowList {

    /** Creates a new RowList */
    def apply ( totalRows: Int, offset: Int, rows: Seq[nElement] ): RowList
        = new RowList( totalRows, offset, rows )

    /** Creates a new document from a JSON object */
    def apply ( elem: nElement ): RowList = {
        val obj = elem.asObject
        val rows = obj.ary("rows")
        apply(
            obj.int_?("total_rows").map(_.toInt).getOrElse(rows.length),
            obj.int_?("offset").map(_.toInt).getOrElse(0),
            rows
        )
    }

    /** Creates a new document from a JSON string */
    def apply ( json: String ): RowList = apply( nParser.jsonObj(json) )
}

/**
 * A list of results from a view query
 */
class RowList private[foldout](
    val totalRows: Int,
    val offset: Int,
    private val rows: Seq[nElement]
) extends Seq[Row] with Equals with nElement.ToJson {

    /** {@inheritDoc} */
    override def apply ( idx: Int ): Row = new Row( rows.apply(idx) )

    /** {@inheritDoc} */
    override def length: Int = rows.length

    /** {@inheritDoc} */
    override def iterator: Iterator[Row] = new Iterator[Row] {
        private val entries = rows.iterator
        override def hasNext = entries.hasNext
        override def next = new Row(entries.next)
    }

    /** {@inheritDoc} */
    override def hashCode
        = 41 * (
            41 * (
                41 + totalRows.hashCode
            ) + offset.hashCode
        ) + rows.hashCode

    /** {@inheritDoc} */
    override def equals(other: Any) = other match {
        case that: RowList if (
            that.canEqual(this) &&
            this.totalRows == that.totalRows &&
            this.offset == that.offset &&
            this.length == that.length
        ) => {
            this.rows.zip( that.rows ).forall(
                pair => pair._1 == pair._2
            )
        }
        case _ => false
    }

    /** {@inheritDoc} */
    override def canEqual(other: Any) = other.isInstanceOf[RowList]

    /** {@inheritDoc} */
    override def toString = "RowList(offset: %d, rows: %d, [%s])".format(
        offset, totalRows,
        rows.map( _.toString ).mkString(", ")
    )

    /** {@inheritDoc} */
    override def toJson
        = nObject("total" -> totalRows, "offset" -> offset, "rows" -> iterator)
}


