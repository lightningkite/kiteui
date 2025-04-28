package com.lightningkite.kiteui

import com.lightningkite.testing.manual.assertManualReview
import kotlin.test.Test

class Recycler2Test {
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
        currentHash = "f4fce9e695ee11b0d651355b313a3e8dfe032d19",
        reviewedHash = "f4fce9e695ee11b0d651355b313a3e8dfe032d19",
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