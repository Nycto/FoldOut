package com.roundeights.foldout

import com.roundeights.scalon._

import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Success, Failure}

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
    val Group = Value("group")
    val GroupLevel = Value("group_level")
    val Reduce = Value("reduce")
    val InclusiveEnd = Value("inclusive_end")
    val IncludeDocs = Value("include_docs")

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
    private val execute: Requestor.Preset,
    private val params: Map[Params.Param, String] = Map()
)( implicit context: ExecutionContext ) {

    import Params._

    /** Converts the params map into a String/String map */
    private def urlParams = params.map { pair => ((pair._1.toString, pair._2)) }

    /** {@inheritDoc} */
    override def toString = "BulkRead(%s)".format( execute.describe(urlParams) )

    /** Executes this query and dumps the formatted results to stdout */
    def dump: Unit = {
        execute( urlParams ).onComplete {
            case Failure(err) => println(err)
            case Success(None) => println( None )
            case Success(Some(result)) => println( result.pretty )
        }
    }

    /** Executes this request and returns a future containing the results */
    def exec: Future[RowList] = {
        execute( urlParams ).map { _ match {
            case None => RowList( 0, 0, List() )
            case Some ( rows ) => RowList( rows.asObject )
        }}
    }

    /** A helper that adds the given param and returns a new BulkRead */
    private def withParam ( param: Param, value: String ): BulkRead = {
        new BulkRead(
            execute,
            params -- Params.precludes(param) + ((param, value))
        )
    }

    /** A helper that adds the given param and returns a new BulkRead */
    private def withParam ( param: Param, value: nElement ): BulkRead
        = withParam( param, value.json )


    /** Defines the specific key to return */
    def key ( k: nElement ): BulkRead = withParam( Key, k )

    /** Defines the specific key to return */
    def key ( k: nElement, k2: nElement, k3: nElement* ): BulkRead
        = key( k :: k2 :: nList(k3:_*) )

    /** Defines the specific key to return */
    def key ( k: String ): BulkRead = key( nString(k) )

    /** Defines the specific key to return */
    def key ( k: String, k2: String, k3: String* ): BulkRead
        = key( k :: k2 :: nList( k3:_* ) )


    /** Defines a list of keys to return */
    def keys ( ids: String* ): BulkRead = withParam( Keys, nList( ids:_* ) )


    /** Defines the key at which the results should start */
    def startKey ( key: nElement ): BulkRead = withParam(StartKey, key)

    /** Defines the key at which the results should start */
    def startKey ( k: nElement, k2: nElement, k3: nElement* ): BulkRead
        = startKey( k :: k2 :: nList(k3:_*) )

    /** Defines the key at which the results should start */
    def startKey ( key: String ): BulkRead = startKey(nString(key))

    /** Defines the specific key to return */
    def startKey ( k: String, k2: String, k3: String* ): BulkRead
        = startKey( k :: k2 :: nList( k3:_* ) )

    /** Defines the key at which the results should start */
    def startKey ( key: Seq[String] ): BulkRead
        = startKey( nList(key:_*) )


    /** Defines the key at which the results should end */
    def endKey ( key: nElement ): BulkRead = withParam(EndKey, key)

    /** Defines the key at which the results should end */
    def endKey ( k: nElement, k2: nElement, k3: nElement* ): BulkRead
        = endKey( k :: k2 :: nList(k3:_*) )

    /** Defines the key at which the results should end */
    def endKey ( key: String ): BulkRead = endKey(nString(key))

    /** Defines the key at which the results should end */
    def endKey ( k: String, k2: String, k3: String* ): BulkRead
        = endKey( k :: k2 :: nList( k3:_* ) )

    /** Defines the key at which the results should end */
    def endKey ( key: Seq[String] ): BulkRead
        = endKey( nList(key:_*) )


    /** Defines a range of keys to select */
    def range ( start: String, end: String ): BulkRead
        = startKey(start).endKey(end)

    /** Defines a range of keys to select */
    def range ( ids: (String, String) ): BulkRead = range( ids._1, ids._2 )

    /** Defines a range of keys to select */
    def range ( start: Seq[String], end: Seq[String] ): BulkRead
        = startKey(start).endKey(end)


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


    /** Groups reduce results by key */
    def group( grouped: Boolean ) : BulkRead = withParam(Group, nBool(grouped))

    /** Groups reduce results by key */
    def group: BulkRead = group(true)


    /** Groups reduce results by key */
    def groupLevel( level: Int ) : BulkRead = {
        require( level > 0 )
        withParam(GroupLevel, nInt(level))
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


    /** Marks that full documents should be included as part of the results */
    def includeDocs ( include: Boolean ): BulkRead
        = withParam( IncludeDocs, nBool(include) )

    /** Marks that full documents should be included as part of the results */
    def includeDocs: BulkRead = includeDocs( true )

}


