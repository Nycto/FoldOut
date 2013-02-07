package com.roundeights.foldout

import org.specs2.mutable._
import org.specs2.mock.Mockito

import com.roundeights.scalon._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class DatabaseTest extends Specification with Mockito {

    /** Blocks while waiting for the given future */
    def await[T] ( future: Future[T]): T
        = Await.result( future, Duration(5, "second") )

    /** A shared result for the mocks to return */
    val successResult = Future.successful( Some(
        nObject( "rev" -> "123", "id" -> "abc" )
    ))

    /** The successful result data as a Written object */
    val written = await( Written(successResult) )

    /** A sample document */
    val sampleDoc = Doc( "_id" -> "abc", "data" -> 31415 )

    "Database.push" should {

        "successfully PUT when there is no value in the db" in {
            val request = mock[Requestor]
            request.get("abc") returns Future.successful( None )
            request.put(any[String], any[nObject]) returns successResult

            await( new Database(request).push(sampleDoc) ) must_== written

            there was one(request).put("abc", sampleDoc.obj)
        }

        "not perform a put when the value in the db matches" in {
            val request = mock[Requestor]
            request.put(any[String], any[nObject]) throws new RuntimeException
            request.get("abc") returns Future.successful(
                Some( sampleDoc.obj + ("_rev" -> "123") )
            )

            await( new Database(request).push(sampleDoc) ) must_== written
        }

        "perform a PUT when documents dont match" in {
            val request = mock[Requestor]
            request.get("abc") returns Future.successful(
                Some( nObject("other" -> "doc", "_rev" -> "xyz") )
            )
            request.put(any[String], any[nObject]) returns successResult

            await( new Database(request).push(sampleDoc) ) must_== written

            there was one(request).put("abc", sampleDoc.obj)
        }

    }

    "Database.create" should {

        "Run a PUT if the database doesn't exist" in {
            val request = mock[Requestor]
            request.get("/") returns Future.failed( new RequestError("No DB") )
            request.put("/") returns Future.successful( Some( nObject() ) )

            await( new Database(request).create )

            there was one(request).put("/")
        }

        "Succeed if the database exists" in {
            val request = mock[Requestor]
            request.get("/") returns Future.successful( Some( nObject() ) )
            request.put( any[String] ) returns Future.failed(
                new Exception("Should not be called")
            )
            await( new Database(request).create )
            ok
        }

        "Fail if the GET request fails for any other reason" in {
            val err = new Exception("Should be caught")
            val request = mock[Requestor]
            request.get("/") returns Future.failed( err )
            await( new Database(request).create.failed ) must_== err
        }

        "Fail if the PUT request fails" in {
            val err = new Exception("Should be caught")
            val request = mock[Requestor]
            request.get("/") returns Future.failed( new RequestError("No DB") )
            request.put("/") returns Future.failed( err )
            await( new Database(request).create.failed ) must_== err
        }

    }

}



