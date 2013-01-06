package com.roundeights.foldout

import com.roundeights.scalon._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * The list of parameters that BulkRead operations support
 *
 * More information about these parameters can be found here:
 * http://wiki.apache.org/couchdb/HTTP_view_API
 */
private[foldout] object Params extends Enumeration {
    type Param = Value

    /** The list of supported parameters */
    val Key = Value("key")
    val Keys = Value("keys")
    val StartKey = Value("startkey")
    val EndKey = Value("endkey")
    val StartDocID = Value("startkey_docid")
    val EndDocID = Value("endkey_docid")
    val Limit = Value("limit")
    val Stale = Value("stale")
    val Descending = Value("descending")
    val Skip = Value("skip")
    val Reduce = Value("reduce")
    val InclusiveEnd = Value("inclusive_end")

    /** Returns a list of keys that can not be set when the given key is set */
    def precludes ( param: Param ) = param match {
        case Key => Set(Keys, StartKey, EndKey, StartDocID, EndDocID)
        case Keys => Set(Key, StartKey, EndKey, StartDocID, EndDocID)
        case StartKey => Set(Key, Keys, StartDocID)
        case EndKey => Set(Key, Keys, EndDocID)
        case StartDocID => Set(Key, Keys, StartKey)
        case EndDocID => Set(Key, Keys, EndKey)
        case _ => Set()
    }

}

/**
 * An interface for defining parameters for bulk reads
 *
 * More information about these parameters can be found here:
 * http://wiki.apache.org/couchdb/HTTP_view_API
 */
class BulkRead private[foldout] (
    private val requestor: Requestor,
    private val docID: String,
    private val params: Map[Params.Param, String] = Map()
) {

    import Params._

    /** Executes this request and returns a future containing the results */
    def exec: Future[RowList] = {
        val parameters = params.map { pair => ((pair._1.toString, pair._2)) }
        requestor
            .get( docID, parameters )
            .map { opt => RowList( opt.get.asObject ) }
    }

    /** A helper that adds the given param and returns a new BulkRead */
    private def withParam ( param: Param, value: String ): BulkRead = {
        new BulkRead(
            requestor, docID,
            params -- Params.precludes(param) + ((param, value))
        )
    }

    /** A helper that adds the given param and returns a new BulkRead */
    private def withParam ( param: Param, value: nElement ): BulkRead
        = withParam( param, value.json )

    /** Defines the specific key to return */
    def key ( id: String ): BulkRead = withParam(Key, nString(id))

    /** Defines a list of keys to return */
    def keys ( ids: String* ): BulkRead = withParam(
       Keys, nList( ids.foldRight( List[nString]() ){ nString(_) :: _ } )
    )

    /** Defines the key at which the results should start */
    def startKey ( id: String ): BulkRead = withParam(StartKey, nString(id))

    /** Defines the key at which the results should end */
    def endKey ( id: String ): BulkRead = withParam(EndKey, nString(id))

    /** Defines a range of keys to select */
    def range ( start: String, end: String ): BulkRead
        = startKey(start).endKey(end)

    /** Defines a range of keys to select */
    def range ( ids: (String, String) ): BulkRead = range( ids._1, ids._2 )

    /** Defines document ID at which the results should start */
    def startDocID ( id: String ): BulkRead = withParam(StartDocID, nString(id))

    /** Defines document ID at which the results should end */
    def endDocID ( id: String ): BulkRead = withParam(EndDocID, nString(id))

    /** Defines a range of keys to select */
    def docIdRange ( start: String, end: String ): BulkRead
        = startDocID(start).endDocID(end)

    /** Defines a range of keys to select */
    def docIdRange ( ids: (String, String) ): BulkRead
        = docIdRange( ids._1, ids._2 )

    /** Limits the number of documents */
    def limit ( count: Int ): BulkRead = {
        require( count >= 0 )
        withParam( Limit, count.toString )
    }

    /** Drops the first few documents from the results */
    def skip ( count: Int ): BulkRead = {
        require( count >= 0 )
        withParam( Skip, count.toString )
    }

    /** Allows stale documents to be returned */
    def staleOk ( updateAfter: Boolean ): BulkRead = {
        val value: String = if ( updateAfter ) "update_after" else "ok"
        withParam( Stale, value )
    }

    /** Allows stale documents to be returned */
    def staleOk: BulkRead = staleOk( false )

    /** Sorts the results in ascending order */
    def asc: BulkRead = withParam(Descending, nBool(false))

    /** Sorts the results in descendingorder */
    def desc: BulkRead = withParam(Descending, nBool(true))

    /** Whether to run the reduce pass */
    def reduce ( enable: Boolean ): BulkRead
        = withParam( Reduce, nBool(enable) )

    /** Marks that the reduce pass should be run */
    def reduce: BulkRead = reduce( true )

    /** Whether to include the final key when a range is specified */
    def includeEnd ( include: Boolean ): BulkRead
        = withParam( InclusiveEnd, nBool(include) )

    /** Marks that the final key should be included when a range is specified */
    def includeEnd: BulkRead = includeEnd( true )

}

