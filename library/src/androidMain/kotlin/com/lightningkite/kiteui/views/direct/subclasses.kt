package com.lightningkite.kiteui.views.direct

import android.animation.LayoutTransition
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.lightningkite.kiteui.navigation.ScreenStack
import com.lightningkite.kiteui.views.animationsEnabled

class LinkFrameLayout(context: Context): SlightlyModifiedFrameLayout(context) {
    lateinit var navigator: ScreenStack
    var onNavigate: suspend () -> Unit = {}
    var resetsStack: Boolean = false
}

class KiteUiLayoutTransition: LayoutTransition() {
    override fun addChild(parent: ViewGroup?, child: View?) {
        if (animationsEnabled) super.addChild(parent, child)
    }

    override fun removeChild(parent: ViewGroup?, child: View?) {
        if (animationsEnabled) super.removeChild(parent, child)
    }

    override fun showChild(parent: ViewGroup?, child: View?, oldVisibility: Int) {
        if (animationsEnabled) super.showChild(parent, child, oldVisibility)
    }

    override fun hideChild(parent: ViewGroup?, child: View?, newVisibility: Int) {
        if (animationsEnabled) super.hideChild(parent, child, newVisibility)
    }

}