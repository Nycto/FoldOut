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
    def mockRequestor(): Requestor = {
        val request = mock[Requestor]
        request.get(any[String], any[Map[String,String]]) returns mockResult
        request
    }


    "BulkRead" should {

        "pass an empty parameter list when nothing is specified" in {
            val request = mockRequestor()
            exec( new BulkRead(request, "path") ) must_== expected
            there was one(request).get("path", Map())
        }

        "pass a full parameter list (Part 1)" in {
            val request = mockRequestor()
            val read = new BulkRead(request, "path")
                .key("abc123").limit(10).skip(2).desc
                .staleOk.reduce.includeEnd.includeDocs
                .group.groupLevel(5)

            exec( read ) must_== expected

            there was one(request).get("path", Map(
                "key" -> "\"abc123\"", "limit" -> "10", "skip" -> "2",
                "descending" -> "true", "stale" -> "ok", "reduce" -> "true",
                "inclusive_end" -> "true", "include_docs" -> "true",
                "group" -> "true", "group_level" -> "5"
            ))
        }

        "pass a full parameter list (Part 2)" in {
            val request = mockRequestor()
            val read = new BulkRead(request, "path")
                .keys("abc123", "xyz789").asc
                .staleOk( true ).reduce( false )
                .includeEnd( false ).includeDocs( false )

            exec( read ) must_== expected

            there was one(request).get("path", Map(
                "keys" -> """["abc123","xyz789"]""",
                "descending" -> "false", "stale" -> "update_after",
                "reduce" -> "false", "inclusive_end" -> "false",
                "include_docs" -> "false"
            ))
        }

        "handle string list keys" in {
            val request = mockRequestor()
            val read = new BulkRead(request, "path")
                .key("abc123", "lmnop456", "xyz789")

            exec( read ) must_== expected

            there was one(request).get("path", Map(
                "key" -> """["abc123","lmnop456","xyz789"]"""
            ))
        }

        "handle nElement list keys" in {
            val request = mockRequestor()
            val read = new BulkRead(request, "path")
                .key( nInt(123), nFloat(3.14), nObject(), nList() )

            exec( read ) must_== expected

            there was one(request).get("path", Map(
                "key" -> """[123,3.14,{},[]]"""
            ))
        }

        "handle list based range keys" in {
            val request = mockRequestor()
            val read = new BulkRead(request, "path")
                .range( List("abc", "123"), List("xyz", "789") )

            exec( read ) must_== expected

            there was one(request).get("path", Map(
                "startkey" -> """["abc","123"]""",
                "endkey" -> """["xyz","789"]"""
            ))
        }

        "pass a key range" in {
            val request = mockRequestor()
            val read = new BulkRead(request, "path")
                .range("abc123" -> "xyz789")

            exec( read ) must_== expected

            there was one(request).get("path", Map(
                "startkey" -> "\"abc123\"", "endkey" -> "\"xyz789\""
            ))
        }

        "pass a doc ID range" in {
            val request = mockRequestor()
            val read = new BulkRead(request, "path")
                .docIdRange("abc123" -> "xyz789")

            exec( read ) must_== expected

            there was one(request).get("path", Map(
                "startkey_docid" -> "\"abc123\"",
                "endkey_docid" -> "\"xyz789\""
            ))
        }

        "remove conflicting keys" in {
            val request = mockRequestor()
            val read = new BulkRead(request, "path")
                .docIdRange("abc123" -> "xyz789")
                .range("abc123" -> "xyz789")
                .keys("abc123", "xyz789")
                .key("abc123")

            exec( read ) must_== expected
            there was one(request).get("path", Map("key" -> "\"abc123\""))
        }

        "Throw errors when a value is invalid" in {
            new BulkRead(mockRequestor(), "path").limit(-5)
                .must( throwA[IllegalArgumentException] )

            new BulkRead(mockRequestor(), "path").skip(-5)
                .must( throwA[IllegalArgumentException] )
        }

    }

}


