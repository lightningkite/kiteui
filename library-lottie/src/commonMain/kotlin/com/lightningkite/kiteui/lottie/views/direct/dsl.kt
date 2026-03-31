@file:OptIn(ExperimentalContracts::class)

package com.lightningkite.kiteui.lottie.views.direct

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.Untested
import com.lightningkite.kiteui.lottie.models.LottieRemote
import com.lightningkite.kiteui.lottie.models.LottieSource
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.write
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Creates a Lottie animation view from a [LottieSource].
 *
 * @param source The Lottie animation source (Remote URL or raw JSON)
 * @param description Accessibility description for the animation
 * @param setup Configuration block for the view
 */
@Untested
@ExperimentalKiteUi
@ViewDsl
inline fun ViewWriter.lottie(
    source: LottieSource,
    description: String,
    setup: LottieView.() -> Unit = {}
): LottieView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(LottieView(context, source, description), setup)
}

/**
 * Creates a Lottie animation view from a remote URL.
 *
 * @param url The URL to the Lottie JSON file
 * @param description Accessibility description for the animation
 * @param setup Configuration block for the view
 */
@Untested
@ExperimentalKiteUi
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.lottie(
    url: String,
    description: String,
    setup: LottieView.() -> Unit = {}
): LottieView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(LottieView(context, LottieRemote(url), description), setup)
}
