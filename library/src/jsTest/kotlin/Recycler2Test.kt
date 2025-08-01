package com.lightningkite.kiteui

import com.lightningkite.testing.manual.assertManualReview
import kotlin.test.Test

public class Recycler2Test {
    @Test fun scrollRules() = assertManualReview(
        file = "ScrollView.commonHtml.js.kt",
        currentHash = "fba7a2cd837572a70abc5359b1fbdbd244240cfd",
        reviewedHash = "fba7a2cd837572a70abc5359b1fbdbd244240cfd",
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
        currentHash = "f11c93d365428bb99d3841015aa5a592c2d80a59",
        reviewedHash = "f11c93d365428bb99d3841015aa5a592c2d80a59",
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