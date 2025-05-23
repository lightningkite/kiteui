package com.lightningkite.kiteui.views.direct

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDragHandleView
import com.google.android.material.sidesheet.SideSheetBehavior
import com.google.android.material.sidesheet.SideSheetCallback
import com.lightningkite.kiteui.models.CardSemantic
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.drawableWithoutCorners
import com.lightningkite.kiteui.views.lparams
import com.lightningkite.kiteui.views.withoutAnimation
import com.lightningkite.signal.Property
import com.lightningkite.signal.Writable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


actual class CoordinatorFrame actual constructor(context: RContext) : RView(context) {
    override val cannotBeCovered: Boolean get() = false
    override val native = CoordinatorLayoutWithGestures(context.activity)
    override fun childTouches(child: RView): Int {
        val p = child.lparams as CoordinatorLayout.LayoutParams
        var total = 0
        if (p.width == ViewGroup.LayoutParams.MATCH_PARENT) total = total or Gravity.LEFT or Gravity.RIGHT
        if (p.height == ViewGroup.LayoutParams.MATCH_PARENT) total = total or Gravity.TOP or Gravity.BOTTOM
        if (p.gravity and Gravity.LEFT > 0) total = total or Gravity.LEFT
        if (p.gravity and Gravity.RIGHT > 0) total = total or Gravity.RIGHT
        if (p.gravity and Gravity.TOP > 0) total = total or Gravity.TOP
        if (p.gravity and Gravity.BOTTOM > 0) total = total or Gravity.BOTTOM
        return total
    }

    override fun willAddChild(view: RView) {
        view.native.layoutParams = defaultLayoutParams()
        super.willAddChild(view)
    }

    override fun internalAddChild(index: Int, view: RView) {
        view.native.z = index.toFloat() // Coordinator Frame layout uses elevation by default to determine the z axis, so we have to set this ourselves
        super.internalAddChild(index, view)
    }

    override fun defaultLayoutParams(): ViewGroup.LayoutParams =
        CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

    actual fun bottomSheet(
        peekSize: Dimension?,
        partialRatio: Float,
        draggable: Boolean,
        startState: BottomSheetState,
        shouldRemoveExpandedCorners: Boolean,
        blockBehind: Boolean,
        content: ViewWriter.(control: BottomSheetControl) -> ViewModifiable
    ) {
        lateinit var b: BottomSheetBehavior<View>
        var sub: ViewModifiable? = null
        var backToRemove: RView? = null
        val state = Property(startState)
        val control = object : BottomSheetControl {
            override val state: Writable<BottomSheetState> = state
            override fun close() {
                b.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }
        withoutAnimation {
            backToRemove = if (blockBehind) dismissBackground { opacity = 0.0; onClick { control.close() } } else null
            beforeNextElementSetup {
                b = BottomSheetBehavior<View>(context.activity, null).apply {
                    this.halfExpandedRatio = partialRatio
                    peekSize?.value?.toInt()?.let { this.peekHeight = it }
                    state.addListener {
                        this.state = when (state.value) {
                            BottomSheetState.EXPANDED -> BottomSheetBehavior.STATE_EXPANDED
                            BottomSheetState.PARTIALLY_EXPANDED -> BottomSheetBehavior.STATE_HALF_EXPANDED
                            BottomSheetState.COLLAPSED -> BottomSheetBehavior.STATE_COLLAPSED
                        }
                    }
                    launch {
                        delay(32)
                        this@apply.state = when (state.value) {
                            BottomSheetState.EXPANDED -> BottomSheetBehavior.STATE_EXPANDED
                            BottomSheetState.PARTIALLY_EXPANDED -> BottomSheetBehavior.STATE_HALF_EXPANDED
                            BottomSheetState.COLLAPSED -> BottomSheetBehavior.STATE_COLLAPSED
                        }
                    }
                    isFitToContents = false
                    isShouldRemoveExpandedCorners = shouldRemoveExpandedCorners
                    isDraggable = draggable
                    this.isHideable = true
                    addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                        override fun onSlide(bottomSheet: View, slideOffset: Float) {
                            println("onSlide $sub $slideOffset")
                            sub?.rView?.native?.run {
                                layoutParams.height = (this@CoordinatorFrame.native.height - bottomSheet.top).also { println("Height is $it") }
                                requestLayout()
                            }
                            backToRemove?.native?.alpha = (1f + slideOffset).coerceIn(0f, 1f)
                        }

                        override fun onStateChanged(bottomSheet: View, newState: Int) {
                            when (newState) {
                                BottomSheetBehavior.STATE_EXPANDED -> state.value = BottomSheetState.EXPANDED
                                BottomSheetBehavior.STATE_HALF_EXPANDED -> state.value =
                                    BottomSheetState.PARTIALLY_EXPANDED

                                BottomSheetBehavior.STATE_COLLAPSED -> state.value = BottomSheetState.COLLAPSED
                                BottomSheetBehavior.STATE_DRAGGING -> {}
                                BottomSheetBehavior.STATE_HIDDEN -> {
                                    this@CoordinatorFrame.removeChild(this@beforeNextElementSetup)
                                    backToRemove?.let { this@CoordinatorFrame.removeChild(it) }
                                }

                                BottomSheetBehavior.STATE_SETTLING -> {}
                            }
                        }
                    })
                    this.state = BottomSheetBehavior.STATE_HIDDEN
                }
                (lparams as? CoordinatorLayout.LayoutParams)?.behavior = b
                b.state = BottomSheetBehavior.STATE_HIDDEN

            } - col { sub = content(control) }
        }
    }

    actual fun leftSlidingPanel(
        ratio: Float?,
        blockBehind: Boolean,
        content: ViewWriter.(control: SlidingPanelControl) -> ViewModifiable
    ) {
        lateinit var b: SideSheetBehavior<View>
        var backToRemove: RView? = null
        val control = object : SlidingPanelControl {
            override fun close() {
                b.state = SideSheetBehavior.STATE_HIDDEN
            }
        }
        backToRemove = if (blockBehind) dismissBackground { opacity = 0.0; onClick { control.close() } } else null
        beforeNextElementSetup {
            b = SideSheetBehavior<View>(context.activity, null).apply {
                this.state = SideSheetBehavior.STATE_HIDDEN
                launch {
                    delay(16)
                    this@apply.state = SideSheetBehavior.STATE_EXPANDED
                }
                addCallback(object : SideSheetCallback() {
                    override fun onStateChanged(sheet: View, newState: Int) {
                        when (newState) {
                            SideSheetBehavior.STATE_HIDDEN -> {
                                this@CoordinatorFrame.removeChild(this@beforeNextElementSetup)
                                backToRemove?.let { this@CoordinatorFrame.removeChild(it) }
                            }

                            else -> {}
                        }
                    }

                    override fun onSlide(sheet: View, slideOffset: Float) {
                        backToRemove?.native?.alpha = slideOffset
                    }
                })
                //TODO: blocksBehind
            }
            (lparams as? CoordinatorLayout.LayoutParams)?.gravity = Gravity.LEFT
            (lparams as? CoordinatorLayout.LayoutParams)?.width = ratio?.let {
                (AppState.windowInfo.value.width.value * it).toInt()
            } ?: ViewGroup.LayoutParams.WRAP_CONTENT
            (lparams as? CoordinatorLayout.LayoutParams)?.behavior = b

        } - content(control)
    }

    actual fun rightSlidingPanel(
        ratio: Float?,
        blockBehind: Boolean,
        content: ViewWriter.(control: SlidingPanelControl) -> ViewModifiable
    ) {
        lateinit var b: SideSheetBehavior<View>
        var backToRemove: RView? = null
        val control = object : SlidingPanelControl {
            override fun close() {
                b.state = SideSheetBehavior.STATE_HIDDEN
            }
        }
        backToRemove = if (blockBehind) dismissBackground { opacity = 0.0; onClick { control.close() } } else null
        beforeNextElementSetup {
            b = SideSheetBehavior<View>(context.activity, null).apply {
                this.state = SideSheetBehavior.STATE_HIDDEN
                launch {
                    delay(16)
                    this@apply.state = SideSheetBehavior.STATE_EXPANDED
                }
                addCallback(object : SideSheetCallback() {
                    override fun onStateChanged(sheet: View, newState: Int) {
                        when (newState) {
                            SideSheetBehavior.STATE_HIDDEN -> {
                                this@CoordinatorFrame.removeChild(this@beforeNextElementSetup)
                                backToRemove?.let { this@CoordinatorFrame.removeChild(it) }
                            }

                            else -> {}
                        }
                    }

                    override fun onSlide(sheet: View, slideOffset: Float) {
                        backToRemove?.native?.alpha = slideOffset
                    }
                })
                //TODO: blocksBehind
            }
            (lparams as? CoordinatorLayout.LayoutParams)?.gravity = Gravity.RIGHT
            (lparams as? CoordinatorLayout.LayoutParams)?.width = ratio?.let {
                (AppState.windowInfo.value.width.value * it).toInt()
            } ?: ViewGroup.LayoutParams.WRAP_CONTENT
            (lparams as? CoordinatorLayout.LayoutParams)?.behavior = b

        } - content(control)
    }

    actual fun onLeftSwipe(action: suspend () -> Unit) {
        native.onLeftSwipeAction = { launch { action() } }
    }

    actual fun onRightSwipe(action: suspend () -> Unit) {
        native.onRightSwipeAction = { launch { action() } }
    }
}

actual class CoordinatorDragHandle actual constructor(context: RContext) : RView(context) {
    override val native: BottomSheetDragHandleView = BottomSheetDragHandleView(context.activity).apply {
        minimumWidth = 5.rem.value.toInt()
        minimumHeight = 1.rem.value.toInt()
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack = theme[CardSemantic]
    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        native.setImageDrawable(drawableWithoutCorners(theme.theme.icon, Color.transparent, 0.px).apply {
        })
    }
}

