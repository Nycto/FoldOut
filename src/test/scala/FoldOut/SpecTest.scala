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

        "generate a consistent sha1 hash" in {
            ViewSpec("function(){/* map */}").sha1
                .must_==("b51f8c5d56a7c6c35fb5b5cf6d1bb2528017257f")

            ViewSpec("function(){/* map */}", "function(){/* reduce */}").sha1
                .must_==("fb7fb246113a2486ace763c34e9163729dfef304")
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

        "generate a consistent hash" in {
            DesignSpec(
                language = "JavaScript",
                "key" -> ViewSpec("function(){/* map */}"),
                "xyz" -> ViewSpec("function(){/* other */}"),
                "abc" -> ViewSpec("function(){/*m*/}", "function(){/*r*/}")
            ).sha1 must_== "a7027fe68733f10a867e004a3abb775691e55ea6"
        }

    }

}


