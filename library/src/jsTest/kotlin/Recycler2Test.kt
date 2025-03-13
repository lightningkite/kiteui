package com.lightningkite.kiteui

import com.lightningkite.testing.manual.assertManualReview
import kotlin.test.Test

class Recycler2Test {
    @Test fun scrollRules() = assertManualReview(
        file = "ScrollView.commonHtml.js.kt",
        currentHash = "acc67f4627fa42136602297e74040999ea4b4fa1",
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
        currentHash = "f2adccc217a44c6b94b6ed4b11ddeca4d3049958",
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