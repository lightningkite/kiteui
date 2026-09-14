package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ImportantSemantic
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.dynamicThemed
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Signal
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Reproduces the pattern used by app action bars (utils.FormattedAction.button): a Button whose
 * `action` and label are bound REACTIVELY (`this::action { … }`, `text { ::content { … } }`), often
 * inside a `shownWhen` wrapper. In the AI test driver these buttons must still expose a `click`
 * action (which Button.driverActions derives from a non-null `action`) and their reactive label.
 */
class ReactiveButtonDriverTest {

    /** Reactive action binding, no wrapper — does the button expose click + label? */
    @Test
    fun reactiveActionBindingExposesClick() = uiTest(
        content = {
            col {
                val act = Signal<Action?>(Action("Save", Icon.send) { })
                button {
                    debugName = "save"
                    text { ::content { act()?.title ?: "" } }
                    this::action { act() }
                }
            }
        }
    ) {
        val snap = snapshot()
        println("noShownWhen snapshot:\n$snap")
        val fc = findClickable("save")
        println("noShownWhen findClickable(save): $fc")
        assertTrue(fc.isNotEmpty() && "click" in fc.first().actions,
            "reactive-action button should expose click: $fc")
    }

    /** The real-world pattern: reactive action binding inside a shownWhen wrapper. */
    @Test
    fun reactiveActionBindingInsideShownWhenExposesClick() = uiTest(
        content = {
            col {
                val act = Signal<Action?>(Action("Save", Icon.send) { })
                shownWhen { act() != null }.button {
                    debugName = "save2"
                    text { ::content { act()?.title ?: "" } }
                    this::action { act() }
                }
            }
        }
    ) {
        val snap = raw("root\tsnapshot\t--hidden")
        println("shownWhen snapshot(--hidden):\n$snap")
        val fc = findClickable("save2")
        println("shownWhen findClickable(save2): $fc")
        assertTrue(fc.isNotEmpty() && "click" in fc.first().actions,
            "reactive-action button inside shownWhen should expose click: $fc")
    }

    /** Does the driver's setValue propagate through a two-way `content bind writable` to the model? */
    @Test
    fun setValuePropagatesToBoundWritable() = uiTest(
        content = {
            col {
                val model = Signal("original")
                textInput { debugName = "field"; content bind model }
                text { debugName = "echo"; ::content { "model=${model()}" } }
            }
        }
    ) {
        setValue("field", "changed")
        println("echo after setValue: " + snapshot("echo"))
        assertValue("echo", "model=changed")
    }

    /** Does shownWhen { false } actually hide its content from the driver's default (non-hidden) view? */
    @Test
    fun shownWhenFalseHidesFromDriver() = uiTest(
        content = {
            col {
                shownWhen { false }.text { content = "SecretText"; debugName = "secret" }
                text { content = "VisibleText"; debugName = "visible" }
            }
        }
    ) {
        val snap = snapshot()
        println("shownWhen{false} snapshot:\n$snap")
        val found = findAll("secret")
        println("findAll(secret): $found")
        assertTrue(found.isEmpty(),
            "shownWhen{false} content must be excluded from the default driver view, but was found: $found")
    }

    /** The EXACT app helper chain: shownWhen { … }.dynamicThemed { … }.button { reactive action }. */
    @Test
    fun reactiveActionBindingWithDynamicThemedExposesClick() = uiTest(
        content = {
            col {
                val act = Signal<Action?>(Action("Save", Icon.send) { })
                shownWhen { act() != null }
                    .dynamicThemed { ImportantSemantic }
                    .button {
                        debugName = "save3"
                        text { ::content { act()?.title ?: "" } }
                        this::action { act() }
                    }
            }
        }
    ) {
        val snap = raw("root\tsnapshot\t--hidden")
        println("dynamicThemed snapshot(--hidden):\n$snap")
        val fc = findClickable("save3")
        println("dynamicThemed findClickable(save3): $fc")
        assertTrue(fc.isNotEmpty() && "click" in fc.first().actions,
            "reactive-action button with dynamicThemed should expose click: $fc")
    }
}
