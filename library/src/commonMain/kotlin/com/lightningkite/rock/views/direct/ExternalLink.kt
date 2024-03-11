package com.lightningkite.rock.views.direct

import com.lightningkite.rock.views.NView
import com.lightningkite.rock.views.RView
import com.lightningkite.rock.views.ViewDsl
import com.lightningkite.rock.views.ViewWriter
import kotlin.jvm.JvmInline
import kotlin.contracts.*

expect class NExternalLink : NView

@JvmInline
value class ExternalLink(override val native: NExternalLink) : RView<NExternalLink>

@ViewDsl
expect fun ViewWriter.externalLinkActual(setup: ExternalLink.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.externalLink(noinline setup: ExternalLink.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; externalLinkActual(setup) }
expect var ExternalLink.to: String
expect var ExternalLink.newTab: Boolean
expect fun ExternalLink.onNavigate(action: suspend () -> Unit)
