package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.views.ElementWriter

fun Page.render(writer: ElementWriter.CanAddTheme) = with(writer) { render() }

context(writer: ElementWriter.CanAddTheme)
fun Page.render() = with(writer) { render() }

fun ElementWriter.CanAddTheme.render(page: Page) = page.render()
