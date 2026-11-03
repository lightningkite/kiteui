package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.setup
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.core.RawReactive
import com.lightningkite.reactive.core.ReactiveState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression coverage for the iOS button spinner, which was invisible for two independent reasons:
 *
 * 1. `NativeElement`'s process aggregate is a `Reactive<Unit>`, and its change guard compared
 *    `ReactiveState.raw`s - statically typed `Unit`, which Kotlin/Native folds to the `Unit`
 *    singleton, so the guard saw every change as a no-op and no element's foreground/background
 *    process state ever moved on iOS.
 * 2. The spinner's opacity binding lives inside `activityIndicator { }`, where the innermost
 *    implicit receiver is the indicator, not the button - so the `working` extension resolved
 *    against the indicator, whose foreground process set is always empty.
 *
 * Both had to be fixed for a press to show a spinner, and the second one is exactly the sort of
 * thing that compiles silently, so this asserts the end result rather than either mechanism.
 */
@OptIn(ExperimentalForeignApi::class)
class ButtonSpinnerTest {

    private val Button.spinner: ActivityIndicator get() = children.filterIsInstance<ActivityIndicator>().single()

    @Test
    fun spinnerTracksItsOwnButtonsAction() {
        val window = UIWindow(frame = CGRectMake(0.0, 0.0, 300.0, 200.0))
        val vc = UIViewController(null, null)
        window.rootViewController = vc
        vc.view.setFrame(CGRectMake(0.0, 0.0, 300.0, 200.0))

        // Holds the action open so the test can observe the running state.
        val gate = RawReactive<Unit>(ReactiveState.notReady)
        lateinit var pressed: Button
        lateinit var untouched: Button
        vc.setup(Theme(id = "button-spinner-test")) {
            column {
                button {
                    pressed = this
                    // keepRunningWhile = null so the action runs in the button's own scope, which
                    // dispatches on the main thread the test is already on - no pumping required.
                    action = Action("pressed", keepRunningWhile = null) { gate() }
                }
                button {
                    untouched = this
                    action = Action("untouched", keepRunningWhile = null) {}
                }
            }
        }
        vc.view.setNeedsLayout()
        vc.view.layoutIfNeeded()

        assertEquals(0.0, pressed.spinner.opacity, "no spinner before anything is pressed")

        pressed.action!!.startAction(pressed)
        assertEquals(1.0, pressed.spinner.opacity, "the pressed button should spin while its action runs")
        assertEquals(0.0, untouched.spinner.opacity, "a button nobody pressed must not spin")

        gate.state = ReactiveState(Unit)
        assertEquals(0.0, pressed.spinner.opacity, "the spinner should stop once the action finishes")
    }
}
