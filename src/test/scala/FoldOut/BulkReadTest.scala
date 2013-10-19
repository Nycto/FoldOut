package com.roundeights.foldout

import org.specs2.mutable._
import org.specs2.mock.Mockito

import com.roundeights.scalon._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class BulkReadTest extends Specification with Mockito {

    /** A shared result for the mocks to return */
    val mockResult = Future.successful( Some(
        nParser.json("""{ "total_rows": 0, "offset": 5, "rows": [] }""")
    ))

    /** The expected RowList comparison */
    val expected = RowList( 0, 5, List() )

    /** Blocks while waiting for the given future */
    def exec ( reader: BulkRead ): RowList
        = Await.result( reader.exec, Duration(5, "second") )

    /** Returns a mock requestor that returns the 'expected' RowList */
    def mockRequestor(
        result: Future[Option[nElement]] = mockResult
    ): Requestor.Preset = {
        val request = mock[Requestor.Preset]
        request.apply(any[Map[String,String]]) returns result
        request
    }


    "BulkRead" should {

        "pass an empty parameter list when nothing is specified" in {
            val request = mockRequestor()
            exec( new BulkRead(request) ) must_== expected
            there was one(request).apply(Map())
        }

        "pass a full parameter list (Part 1)" in {
            val request = mockRequestor()
            val read = new BulkRead(request)
                .key("abc123").limit(10).skip(2).desc
                .staleOk.reduce.includeEnd.includeDocs
                .group.groupLevel(5)

            exec( read ) must_== expected

            there was one(request).apply(Map(
                "key" -> "\"abc123\"", "limit" -> "10", "skip" -> "2",
                "descending" -> "true", "stale" -> "ok", "reduce" -> "true",
                "inclusive_end" -> "true", "include_docs" -> "true",
                "group" -> "true", "group_level" -> "5"
            ))
        }

        "pass a full parameter list (Part 2)" in {
            val request = mockRequestor()
            val read = new BulkRead(request)
                .keys("abc123", "xyz789").asc
                .staleOk( true ).reduce( false )
                .includeEnd( false ).includeDocs( false )

            exec( read ) must_== expected

            there was one(request).apply(Map(
                "keys" -> """["abc123","xyz789"]""",
                "descending" -> "false", "stale" -> "update_after",
                "reduce" -> "false", "inclusive_end" -> "false",
                "include_docs" -> "false"
            ))
        }

        "handle string list keys" in {
            val request = mockRequestor()
            val read = new BulkRead(request)
                .key("abc123", "lmnop456", "xyz789")

            exec( read ) must_== expected

            there was one(request).apply(Map(
                "key" -> """["abc123","lmnop456","xyz789"]"""
            ))
        }

        "handle nElement list keys" in {
            val request = mockRequestor()
            val read = new BulkRead(request)
                .key( nInt(123), nFloat(3.14), nObject(), nList() )

            exec( read ) must_== expected

            there was one(request).apply(Map(
                "key" -> """[123,3.14,{},[]]"""
            ))
        }

        "handle list based range keys" in {
            val request = mockRequestor()
            val read = new BulkRead(request)
                .range( List("abc", "123"), List("xyz", "789") )

            exec( read ) must_== expected

            there was one(request).apply(Map(
                "startkey" -> """["abc","123"]""",
                "endkey" -> """["xyz","789"]"""
            ))
        }

        "pass a key range" in {
            val request = mockRequestor()
            val read = new BulkRead(request)
                .range("abc123" -> "xyz789")

            exec( read ) must_== expected

            there was one(request).apply(Map(
                "startkey" -> "\"abc123\"", "endkey" -> "\"xyz789\""
            ))
        }

        "pass a doc ID range" in {
            val request = mockRequestor()
            val read = new BulkRead(request)
                .docIdRange("abc123" -> "xyz789")

            exec( read ) must_== expected

            there was one(request).apply(Map(
                "startkey_docid" -> "\"abc123\"",
                "endkey_docid" -> "\"xyz789\""
            ))
        }

        "remove conflicting keys" in {
            val request = mockRequestor()
            val read = new BulkRead(request)
                .docIdRange("abc123" -> "xyz789")
                .range("abc123" -> "xyz789")
                .keys("abc123", "xyz789")
                .key("abc123")

            exec( read ) must_== expected
            there was one(request).apply(Map("key" -> "\"abc123\""))
        }

        "Throw errors when a value is invalid" in {
            new BulkRead(mockRequestor()).limit(-5)
                .must( throwA[IllegalArgumentException] )

            new BulkRead(mockRequestor()).skip(-5)
                .must( throwA[IllegalArgumentException] )
        }

        "Return an empty rowlist when the bulk read returns a none" in {
            val request = mockRequestor( Future.successful(None) )
            val read = new BulkRead(request).key("abc123")

            exec( read ) must_== RowList(0,0,List())
        }

    }

}


