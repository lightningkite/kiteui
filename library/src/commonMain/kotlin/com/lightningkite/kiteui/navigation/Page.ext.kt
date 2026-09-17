package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.views.ElementWriter
import kotlin.jvm.JvmName

public fun Page.render(writer: ElementWriter.CanAddTheme): Unit = with(writer) { render() }

@JvmName("renderToContext")
public context(writer: ElementWriter.CanAddTheme)
fun Page.render(): Unit = with(writer) { render() }

public fun ElementWriter.CanAddTheme.render(page: Page): Unit = page.render()
