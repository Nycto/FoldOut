package com.roundeights.foldout

import com.roundeights.scalon.nElement

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Updated companion
 */
object Updated {

    /** Builds a new instance from respose future */
    private[foldout] def apply ( result: Future[Option[nElement]] ) = {
        result.map { opt =>
            new Updated(
                opt.getOrElse( throw new RequestError(
                    "Request did not return a valid response"
                ))
            )
        }
    }

}

/**
 * The result of a PUT or POST request
 */
case class Updated private[foldout] ( val rev: String, val id: String ) {

    /** Builds a new instance from a notation document */
    private[foldout] def this ( doc: nElement )
        = this( doc.asObject.str("rev"), doc.asObject.str("id") )

}


