package com.lightningkite.kiteuiexample

import android.os.Bundle
import android.widget.FrameLayout
import com.lightningkite.mppexampleapp.*
import com.lightningkite.kiteui.KiteUiActivity
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.mainPageNavigator
import com.lightningkite.signal.ReactiveContext
import com.lightningkite.signal.await
import com.lightningkite.signal.invoke
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.internal.LeakCheckerPage

class MainActivity : KiteUiActivity() {
    override val mainNavigator: PageNavigator = PageNavigator { AutoRoutes }
    val dialogNavigator: PageNavigator = PageNavigator { AutoRoutes }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        codeCacheDir.setReadOnly()
        with(viewWriter) {
            app(mainNavigator, dialogNavigator)
//            this.mainPageNavigator = mainNavigator
//            with(LeakCheckerPage) {
//                render()
//            }
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

    override val theme: ReactiveContext.() -> Theme
        get() = { appTheme() }
}