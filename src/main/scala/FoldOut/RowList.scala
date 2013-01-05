package com.roundeights.foldout

import com.roundeights.scalon.{nObject, nElement, nParser}

/**
 * RowList Companion
 */
object RowList {

    /** Creates a new RowList */
    def apply ( totalRows: Int, offset: Int, rows: Seq[nElement] ): RowList
        = new RowList( totalRows, offset, rows )

    /** Creates a new document from a JSON object */
    def apply ( elem: nElement ): RowList = apply(
        elem.asObject.int("total_rows").get.toInt,
        elem.asObject.int("offset").get.toInt,
        elem.asObject.ary("rows").get
    )

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
) extends Seq[Doc] with Equals {

    /** {@inheritDoc} */
    override def apply ( idx: Int ): Doc = Doc( rows.apply(idx).asObject )

    /** {@inheritDoc} */
    override def length: Int = rows.length

    /** {@inheritDoc} */
    override def iterator: Iterator[Doc] = new Iterator[Doc] {
        private val entries = rows.iterator
        override def hasNext = entries.hasNext
        override def next = Doc( entries.next.asObject )
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

}


