package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.ContainerElement
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.kiteui.views.native
import com.lightningkite.kiteui.views.setup
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIGestureRecognizerStateBegan
import platform.UIKit.UIGestureRecognizerStateChanged
import platform.UIKit.UIGestureRecognizerStateEnded
import platform.UIKit.UILongPressGestureRecognizer
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Regression coverage for the iOS `hintPopover` fix. It used to attach a
 * `UILongPressGestureRecognizer` whose handler was an empty `openDialog()` stub marked
 * `// TODO: implement popover` - long-pressing silently did nothing. It now reuses
 * `Element.openPopover` (openPopover.ios.kt), the same in-tree overlay mechanism
 * `MenuButton.opensMenu()` and `BottomSheet.ios.kt`'s `rawPopover` are built on.
 *
 * Drives the gesture target ([HintPopoverTrigger.handleState]) directly with the state values a
 * long-press-and-release would produce, rather than a real touch or `NSObject.performSelector`
 * (which segfaults for void-returning selectors in Kotlin/Native). A real
 * `UIGestureRecognizerState` transition can't be forced on a fake recognizer either - confirmed
 * empirically, `setState()` silently no-ops outside UIKit's own touch-delivery pipeline - which is
 * exactly why [HintPopoverTrigger] splits the state-gating logic out from the `@ObjCAction` entry
 * point into an `internal` function tests can call directly, the same testability pattern as
 * [LocalDateTimeFieldClearTest]'s `ClearDoneTrigger`.
 */
@OptIn(ExperimentalForeignApi::class)
class HintPopoverTest {

    @Test
    fun longPressAttachesRecognizerAndOpensExactlyOnePopoverOnBegan() {
        val window = UIWindow(frame = CGRectMake(0.0, 0.0, 300.0, 300.0))
        val vc = UIViewController(null, null)
        window.rootViewController = vc
        vc.view.setFrame(CGRectMake(0.0, 0.0, 300.0, 300.0))

        lateinit var overlay: ContainerElement
        lateinit var target: Element
        vc.setup(Theme(id = "unitTest")) {
            // The overlay is the same root container that also holds page content - mirroring
            // appBase() in l2/appBase.kt, where context.overlayFrame is set to the coordinatorFrame
            // that also lays out mainLayout(). So `overlay` already has one child (the frame below)
            // before any popover opens; the "no popover yet" baseline is 1, not 0.
            col {
                overlay = this
                context.overlayFrame = this
                frame {
                    // hintPopover is a decorator modifier: it must be chained directly onto the
                    // widget it applies to (as real usage does - see
                    // AndroidFixesVerificationPage.kt), not called as a standalone statement, or
                    // its beforeSetup never runs against anything.
                    hintPopover(PopoverPreferredDirection.belowRight) {
                        text("hint content")
                    }.col {
                        target = this
                        text("press and hold")
                    }
                }
            }
        }
        vc.view.setNeedsLayout()
        vc.view.layoutIfNeeded()

        // The modifier must attach the recognizer to the native view it decorates.
        val recognizers = target.native.gestureRecognizers?.filterIsInstance<UILongPressGestureRecognizer>()
        assertEquals(1, recognizers?.size, "hintPopover should attach exactly one long-press recognizer")

        // The gesture's target must be retained (UIGestureRecognizer keeps only a weak reference),
        // via the same underlyingNativeElement.tag trick __scrollsWithRefreshUncontracted uses for
        // its UIRefreshControl target.
        val trigger = target.underlyingNativeElement.tag as? HintPopoverTrigger
        assertNotNull(trigger, "the gesture target must be retained on the element so it isn't GC'd")

        val baseline = overlay.children.size

        // A long-press-and-release drives Began -> Changed -> Ended. Only Began should open a
        // popover; Changed/Ended must not stack additional ones.
        trigger.handleState(UIGestureRecognizerStateChanged)
        assertEquals(baseline, overlay.children.size, "a Changed transition before Began must not open a popover")

        trigger.handleState(UIGestureRecognizerStateBegan)
        assertEquals(baseline + 1, overlay.children.size, "Began should open exactly one popover")

        trigger.handleState(UIGestureRecognizerStateEnded)
        assertEquals(baseline + 1, overlay.children.size, "releasing (Ended) must not open a second popover")
    }
}
