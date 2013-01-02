package com.roundeights.foldout

import org.specs2.mutable._

class AuthTest extends Specification {

    "Auth pairs" should {

        "prepare Basic Auth Headers" in {
            Auth("uname", "pword").basicAuth
                .must_==( "Basic dW5hbWU6cHdvcmQ=" )
        }

    }

}



