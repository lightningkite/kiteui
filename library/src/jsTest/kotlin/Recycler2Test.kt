package com.lightningkite.kiteui

import com.lightningkite.testing.manual.assertManualReview
import kotlin.test.Test

class Recycler2Test {
    @Test fun scrollRules() = assertManualReview(
        file = "ScrollView.commonHtml.js.kt",
        currentHash = "5040c7dfd58c2e223d86ea2b1383cb53e92311d9",
        reviewedHash = "5040c7dfd58c2e223d86ea2b1383cb53e92311d9",
        whatToTest = """
            This file is *extremely sensitive* to view pagers and recycler views.
            As such, upon editing this file you must retest manually:
            - Recycler View on Chrome
            - Recycler View on Safari
            - Recycler View on Firefox
            - View Pager on Chrome
            - View Pager on Safari
            - View Pager on Firefox
        """.trimIndent()
    )
    @Test fun r2Bullshit() = assertManualReview(
        file = "Recycler2.kt",
        currentHash = "4dd107f40b1379361f50d7a62233031c0d6815c5",
        reviewedHash = "4dd107f40b1379361f50d7a62233031c0d6815c5",
        whatToTest = """
            This file is *extremely sensitive* to view pagers and recycler views.
            As such, upon editing this file you must retest manually:
            - Recycler View on Chrome
            - Recycler View on Safari
            - Recycler View on Firefox
            - View Pager on Chrome
            - View Pager on Safari
            - View Pager on Firefox
        """.trimIndent()
    )
}