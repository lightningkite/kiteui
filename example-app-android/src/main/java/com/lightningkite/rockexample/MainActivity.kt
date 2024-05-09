package com.lightningkite.kiteuiexample

import android.os.Bundle
import android.widget.FrameLayout
import com.lightningkite.mppexampleapp.*
import com.lightningkite.kiteui.KiteUiActivity
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

class MainActivity : KiteUiActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        codeCacheDir.setReadOnly()
        with(viewWriter) {
            app()
//            text { content = "Welcome!" }
//            col {
//                themeChoice = ThemeChoice.Set(appTheme.value)
//                card - row {
//                    card - text("A")
//                    card - text("B")
//                    card - text("C")
//                }
//                card - row {
//                    space()
//                    card - text("A")
//                    card - text("B")
//                    card - text("C")
//                    space()
//                }
//            }
        }
    }

    override val theme: suspend () -> Theme
        get() = { appTheme.await() }
}