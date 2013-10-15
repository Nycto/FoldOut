package com.roundeights.foldout

import org.specs2.mutable._

import com.roundeights.scalon._

class RowListTest extends Specification {

    "A RowList" should {

        "Fail when a row list is missing" in {
            RowList("""{}""") must throwA[Exception]
        }

        "Provide access to total rows and offset" in {
            val list = RowList("""{
                "total_rows": 5, "offset": 9, "rows": []
            }""")

            list.totalRows must_== 5
            list.offset must_== 9
        }

        "Default the total_rows and offset keys if missing" in {
            val list = RowList("""{ "rows": [ {}, {} ] }""")

            list.totalRows must_== 2
            list.offset must_== 0
        }

        "Iterate over the rows" in {
            val list = RowList("""{ "rows": [
                { "id": "1", "key": "2", "value": {} },
                { "id": "3", "key": "4", "value": {} }
            ] }""")

            list.foldRight( List[String]() ) {
                (obj, accum) => obj.toString :: accum
            } must_== List(
                """Row(2, Some(1), {})""",
                """Row(4, Some(3), {})"""
            )
        }

        "Provide equality comparisons" in {
            RowList(5, 9, List())
                .must_==( RowList(5, 9, List()) )

            RowList(5, 9, List( nObject() ))
                .must_==( RowList(5, 9, List( nObject() )) )

            RowList(5, 9, List( nObject() + (( "key", nString("val") )) ))
                .must_!=( RowList(5, 9, List( nObject() )) )

            RowList(5, 9, List( nObject(), nObject() ))
                .must_!=( RowList(5, 9, List( nObject() )) )

            RowList(4, 9, List( nObject() ))
                .must_!=( RowList(5, 9, List( nObject() )) )

            RowList(5, 8, List( nObject() ))
                .must_!=( RowList(5, 9, List( nObject() )) )
        }

        "Convert to json" in {
            RowList(5, 9, List( nObject(), nObject() )).toJson must_== nObject(
                "total" -> 5, "offset" -> 9,
                "rows" -> nList( nObject(), nObject() )
            )
        }

    }

}


