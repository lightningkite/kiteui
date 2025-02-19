package com.lightningkite.kiteui

import com.lightningkite.testing.manual.assertManualReview
import kotlin.test.Test

class Recycler2Test {
    @Test fun scrollRules() = assertManualReview(
        file = "ScrollView.commonHtml.js.kt",
        currentHash = "0f4b6b88598a3c96efe056e33bc922515bad01bf",
        reviewedHash = "0f4b6b88598a3c96efe056e33bc922515bad01bf",
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
        currentHash = "28fac6ee4dd0786eec6c9a4b3dc797c2ed14d8e4",
        reviewedHash = "28fac6ee4dd0786eec6c9a4b3dc797c2ed14d8e4",
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