package com.roundeights.foldout

import org.specs2.mutable._
import org.specs2.mock.Mockito

import com.roundeights.scalon.{nObject, nException}

import scala.concurrent._
import scala.concurrent.duration._
import java.util.concurrent.Executor

import com.ning.http.client.{Request, HttpResponseStatus, HttpResponseBodyPart}
import com.ning.http.client.AsyncHandler.STATE

class AsynchronizerTest extends Specification with Mockito {

    /** Blocks while waiting for the given future */
    def await[T] ( future: Future[T] ): T
        = Await.result( future, Duration(10, "second") )

    /** Builds a status part */
    def status( status: Int ) = {
        val part = mock[HttpResponseStatus]
        part.getStatusCode returns status
        part.getStatusText returns "CODE"
        part
    }

    /** Builds a piece of the body */
    def body( content: String ) = {
        val part = mock[HttpResponseBodyPart]
        part.getBodyPartBytes returns content.getBytes("UTF8")
        part
    }


    // A shared request object
    val request = mock[Request]
    request.getUrl returns "http://example.com"


    "An Asynchronizer" should {

        "Succeed with a None when a 404 is received" in {
            val async = new Asynchronizer(request)
            async.onStatusReceived(status(404)) must_== STATE.ABORT
            await( async.future ) must_== None
        }

        "Fail with a conflict when a 409 is received" in {
            val async = new Asynchronizer(request)
            async.onStatusReceived(status(409)) must_== STATE.ABORT
            await( async.future.failed ) must_== new RevisionConflict
        }

        "Accumulate and parse content on 200" in {
            val async = new Asynchronizer(request)
            async.onStatusReceived(status(200)) must_== STATE.CONTINUE
            async.onBodyPartReceived(body(""" {"one": 1, """))
            async.onBodyPartReceived(body("""  "two": 2} """))
            async.onCompleted
            await( async.future ) must_==
                Some(nObject( "one" -> 1, "two" -> 2))
        }

        "Fail when invalid json is returned" in {
            val async = new Asynchronizer(request)
            async.onStatusReceived(status(200)) must_== STATE.CONTINUE
            async.onBodyPartReceived(body("}"))
            async.onCompleted
            await( async.future ) must throwA[nException]
        }

        "Fail with details" in {
            val async = new Asynchronizer(request)
            async.onStatusReceived(status(500)) must_== STATE.CONTINUE
            async.onBodyPartReceived(body("""{ "error":  "CODE", """))
            async.onBodyPartReceived(body("""  "reason": "MESSAGE" }"""))
            async.onCompleted

            val error = await( async.future.failed )
            error must beAnInstanceOf[RequestError]
            error.getMessage must contain("CODE")
            error.getMessage must contain("MESSAGE")
            error.getMessage must contain("http://example.com")
        }

        "Fail generically when the an error message is invalid" in {
            val async = new Asynchronizer(request)
            async.onStatusReceived(status(500)) must_== STATE.CONTINUE
            async.onBodyPartReceived(body("""}"""))
            async.onCompleted

            val error = await( async.future.failed )
            error must beAnInstanceOf[RequestError]
            error.getMessage must contain("http://example.com")
        }

        "Fail generically when an error message is missing keys" in {
            val async = new Asynchronizer(request)
            async.onStatusReceived(status(500)) must_== STATE.CONTINUE
            async.onBodyPartReceived(body("""{}"""))
            async.onCompleted

            val error = await( async.future.failed )
            error must beAnInstanceOf[RequestError]
            error.getMessage must contain("http://example.com")
        }

        "Fail with an exception handed over by the framework" in {
            val error = new Exception("expected")
            val async = new Asynchronizer(request)
            async.onThrowable( error )
            await( async.future.failed ) must_== error
        }
    }

    "An Asynchronizer tracking metrics" should {

        /** Builds a mock timer and an async instance that uses it */
        def mockAsynchronizer = {
            val timer = mock[Metrics.Timer]
            val metrics = mock[Metrics]
            metrics.start returns timer
            (timer, new Asynchronizer(request, metrics))
        }

        "Call dataRecieved, bodyComplete and success" in {
            val (timer, async) = mockAsynchronizer

            async.onStatusReceived(status(200))
            async.onBodyPartReceived(body("{}"))
            async.onCompleted

            await( async.future )
            there was one(timer).dataReceived
            there was one(timer).bodyComplete
            there was one(timer).success
        }

        "Only call dataReceived once" in {
            val (timer, async) = mockAsynchronizer

            async.onStatusReceived(status(200))
            async.onBodyPartReceived(body("{"))
            async.onBodyPartReceived(body(""" "key": "value" """))
            async.onBodyPartReceived(body("}"))
            async.onCompleted

            await( async.future )
            there was one(timer).dataReceived
        }

        "Call notFound for 404s" in {
            val (timer, async) = mockAsynchronizer
            async.onStatusReceived(status(404))
            await( async.future )
            there was one(timer).notFound
        }

        "Call conflict for 409s" in {
            val (timer, async) = mockAsynchronizer
            async.onStatusReceived(status(409))
            await( async.future.failed )
            there was one(timer).conflict
        }

        "Call dataReceived, bodyComplete and failed for errors" in {
            val (timer, async) = mockAsynchronizer

            async.onStatusReceived(status(500))
            async.onBodyPartReceived(body("""{ "error":  "CODE", """))
            async.onBodyPartReceived(body("""  "reason": "MESSAGE" }"""))
            async.onCompleted

            await( async.future.failed )
            there was one(timer).dataReceived
            there was one(timer).bodyComplete
            there was one(timer).failed
        }

        "Call dataReceived, bodyComplete and failed for bad json" in {
            val (timer, async) = mockAsynchronizer

            async.onStatusReceived(status(200))
            async.onBodyPartReceived(body("}"))
            async.onCompleted

            await( async.future.failed )
            there was one(timer).dataReceived
            there was one(timer).bodyComplete
            there was one(timer).failed
        }
    }

}


