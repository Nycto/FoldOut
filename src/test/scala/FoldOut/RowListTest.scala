package com.roundeights.foldout

import org.specs2.mutable._

import com.roundeights.scalon._

class RowListTest extends Specification {

    "A RowList" should {

        "Fail when a key is missing" in {
            RowList("""{
                "total_rows": 0, "offset": 0
            }""") must throwA[Exception]

            RowList("""{
                "total_rows": 0, "rows": []
            }""") must throwA[Exception]

            RowList("""{
                "offset": 0, "rows": []
            }""") must throwA[Exception]
        }

        "Provide access to total rows and offset" in {
            val list = RowList("""{
                "total_rows": 5, "offset": 9, "rows": []
            }""")

            list.totalRows must_== 5
            list.offset must_== 9
        }

        "Iterate over the rows" in {
            val list = RowList("""{ "total_rows": 3, "offset": 0, "rows": [
                { "key": 1 }, { "key": 2 }, { "key": 3 }
            ] }""")

            list.foldRight( List[Int]() ) {
                (obj, accum) => obj.int("key").get.toInt :: accum
            } must_== List(1, 2, 3)
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

    }

}


