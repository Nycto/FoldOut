package com.roundeights.foldout

import org.specs2.mutable._

import com.roundeights.scalon.nParser

class SpecTest extends Specification {

    "ViewSpecs" should {

        "build themselves from json" in {
            ViewSpec( nParser.json("""{
                "map": "function(){/* map */}"
            }""") ) must_== ViewSpec("function(){/* map */}")

            ViewSpec( nParser.json("""{
                "map": "function(){/* map */}",
                "reduce": "function(){/* reduce */}"
            }""") ) must_== ViewSpec(
                "function(){/* map */}",
                "function(){/* reduce */}"
            )
        }

        "generate json" in {
            ViewSpec("function(){/* map */}").toJson.toString
                .must_==("""{"map":"function(){/* map */}"}""")

            ViewSpec(
                "function(){/* map */}",
                "function(){/* reduce */}"
            ).toJson.toString.must_==(
                "{"
                    + """ "map":"function(){/* map */}", """.trim
                    + """ "reduce":"function(){/* reduce */}" """.trim
                + "}"
            )
        }

    }

    "DesignSpecs" should {

        "build themselves from json" in {
            DesignSpec( nParser.json("""{
                "Language": "JavaScript",
                "views": {
                    "one": { "map": "function(){/* map 1 */}" },
                    "two": { "map": "function(){/* map 2 */}" }
                }
            }""") ) must_== DesignSpec(
                language = "JavaScript",
                "one" -> ViewSpec("function(){/* map 1 */}"),
                "two" -> ViewSpec("function(){/* map 2 */}")
            )
        }

        "generate json" in {
            DesignSpec(
                language = "JavaScript",
                "key" -> ViewSpec("function(){/* map */}")
            ).toJson.toString.must_==(
                """{"Language":"JavaScript","views":"""
                + """{"key":{"map":"function(){/* map */}"}}"""
                + """}"""
            )
        }

    }

}


