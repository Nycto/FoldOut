package com.roundeights.foldout

import org.specs2.mutable._

class UrlBuilderTest extends Specification {

    "toQueryString" should {

        "Convert an empty list of tuples to an empty string" in {
            UrlBuilder.toQueryString() must_== ""
        }

        "Convert tuples into a query string" in {
            UrlBuilder.toQueryString( "one" -> 1 ) must_== "one=1"

            UrlBuilder.toQueryString(
                "one" -> 1, "two" -> "deuce", "pi" -> 3.1415
            ) must_== "one=1&two=deuce&pi=3.1415"
        }

        "Properly escape its values" in {
            UrlBuilder.toQueryString(
                "So Weird!" -> "Couldn't/be/true"
            ) must_== "So+Weird%21=Couldn%27t%2Fbe%2Ftrue"
        }

    }

    "UrlBuilder" should {

        "Generate URLs" in {
            new UrlBuilder("localhost", 8080).url("/path")
                .must_==( "http://localhost:8080/path" )
        }

        "Generate HTTPS URLs when ssl is true" in {
            new UrlBuilder("localhost", 8080, true).url("/path")
                .must_==( "https://localhost:8080/path" )
        }

        "Include query parameters when they're defined" in {
            new UrlBuilder("localhost", 8080)
                .url("/path", "one" -> 1, "two" -> 2)
                .must_==( "http://localhost:8080/path?one=1&two=2" )
        }

        "Prepend a slash to the path" in {
            new UrlBuilder("localhost", 8080, true).url("path")
                .must_==( "https://localhost:8080/path" )
        }

        "Prepend a root path when it is provided" in {
            new UrlBuilder("local", 8080, true, Some("root")).url("path")
                .must_==( "https://local:8080/root/path" )

            new UrlBuilder("local", 8080, true, Some("/root/")).url("/path")
                .must_==( "https://local:8080/root/path" )

            new UrlBuilder("local", 8080, true)
                .withBasePath("one")
                .withBasePath("/two/")
                .url("/three")
                .must_==( "https://local:8080/one/two/three" )
        }

        "Generate URLs without a path" in {
            new UrlBuilder("localhost", 8080).url( None, Map() )
                .must_==( "http://localhost:8080/" )

            new UrlBuilder("localhost", 8080)
                .withBasePath("path")
                .url( None, Map() )
                .must_==( "http://localhost:8080/path/" )
        }

    }

}


