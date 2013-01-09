package com.roundeights.foldout

import com.roundeights.scalon.nElement

/**
 * The result of a PUT or POST request
 */
case class Updated private[foldout] ( val rev: String ) {

    /** Builds a new instance from a notation document */
    private[foldout] def this ( doc: nElement )
        = this( doc.asObject.str("rev") )

}


