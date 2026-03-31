package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.views.ElementWriter
import kotlin.jvm.JvmName

fun Page.render(writer: ElementWriter.CanAddTheme) = with(writer) { render() }

@JvmName("renderToContext")
context(writer: ElementWriter.CanAddTheme)
fun Page.render() = with(writer) { render() }

fun ElementWriter.CanAddTheme.render(page: Page) = page.render()
