package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.requireDisplay
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.KiteUiRoot
import javax.swing.JOptionPane

// Desktop apps have no URL to start from, so they open on the landing route, like iOS without a deep link.
public actual fun PageNavigator.bindToPlatform(context: ElementContext) {
    stack.value = listOf(routes.parse(UrlLikePath.EMPTY) ?: routes.fallback)
    checkNotNull(KiteUiRoot.current) { "appBase must run inside a KiteUiRoot" }.navigator = this
}

// Like the web's window.confirm: navigation needs the answer before it returns, which a Compose dialog can't give.
public actual fun PageNavigator.askForConfirmNavigateAway(): Boolean {
    requireDisplay("ask whether to leave a page with unsaved changes")
    return JOptionPane.showConfirmDialog(
        null,
        "Are you sure you want to leave this page?\nChanges will not be saved.",
        "Leave page?",
        JOptionPane.OK_CANCEL_OPTION,
    ) == JOptionPane.OK_OPTION
}
