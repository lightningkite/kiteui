@file:MustUseReturnValues

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.reactive.context.ReactiveContext

@Deprecated("Renamed for consistency", ReplaceWith("dynamicWeight(amount)"))
public fun ElementWriter.CanAddWeight.changingWeight(amount: ReactiveContext.() -> Float): ElementWriter.CanAddShownWhen = dynamicWeight(amount)

@Deprecated("Renamed for consistency", ReplaceWith("dynamicSizeConstraints(constraints)"))
public fun ElementWriter.CanAddSizing.changingSizeConstraints(constraints: ReactiveContext.() -> SizeConstraints): ElementWriter.CanAddTheme = dynamicSizeConstraints(constraints)
