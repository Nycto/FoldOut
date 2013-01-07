package com.roundeights.foldout

/**
 * A database design
 */
class Design private[foldout] ( private val requestor: Requestor ) {

    /** Returns all the documents in a database */
    def view( name: String ): BulkRead
        = new BulkRead( requestor, "_view/%s".format(name) )

}


